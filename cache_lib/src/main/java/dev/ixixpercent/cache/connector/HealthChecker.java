package dev.ixixpercent.cache.connector;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * HealthChecker is responsible for performing health checks on a set of nodes
 * managed by a CacheServiceGrpcFactory. It periodically checks the connectivity
 * state of each node and removes any that are not healthy.
 */
@Slf4j
public class HealthChecker {
    private final CacheServiceGrpcFactory factory;
    private final long checkIntervalSeconds;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    
    private HealthChecker(Builder builder) {
        this.factory = builder.factory;
        this.checkIntervalSeconds = builder.checkIntervalSeconds;
    }

    /**
     * Performs health checks on all nodes managed by the factory.
     * It checks the state of each node's channel and logs the results.
     * Nodes that are found to be unhealthy are removed from the factory.
     */
    public void performHealthChecks() {
        log.info("Performing health checks on all nodes...");
        for (String key : factory.getNodes()) {
            String[] parts = key.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            ManagedChannel channel = factory.getChannelFromCache(key);
            if (channel == null || channel.isShutdown() || channel.isTerminated()) {
                log.warn("Channel for node {} is shutdown or terminated. Removing node.", key);
                factory.removeNode(host, port);
                continue;
            }

            try {
                ConnectivityState state = channel.getState(true);
                switch (state) {
                    case CONNECTING, READY, IDLE -> {
                        log.trace("Node {} responded, channel is in state {}", key, state);
                    }
                    case TRANSIENT_FAILURE, SHUTDOWN -> {
                        log.warn("Channel for node {} is in failure or shutdown. Removing node.", key);
                        factory.removeNode(host, port);
                    }
                }
            } catch (RuntimeException e) {
                log.error("Failed to perform health checks on node {}", key, e);
                factory.removeNode(host, port);
            }
        }
    }

    /**
     * Starts periodic health checks at the specified interval.
     * The health checks are executed in a separate thread at a fixed rate.
     */
    public void startPeriodicHealthChecks() {
        executor.scheduleAtFixedRate(
                this::performHealthChecks,
                checkIntervalSeconds,
                checkIntervalSeconds,
                TimeUnit.SECONDS
        );
        log.info("Periodic health checks started with an interval of {} seconds.", checkIntervalSeconds);
    }

    /**
     * Builder class for constructing instances of HealthChecker.
     * This class allows for setting the CacheServiceGrpcFactory and
     * the health check interval before building the HealthChecker instance.
     */
    public static class Builder {
        private CacheServiceGrpcFactory factory;
        private long checkIntervalSeconds = 60; // Default interval
        

        /**
         * Sets the CacheServiceGrpcFactory instance to be used by the HealthChecker.
         *
         * @param factory the CacheServiceGrpcFactory instance
         * @return the Builder instance for method chaining
         */
        public Builder withFactory(CacheServiceGrpcFactory factory) {
            this.factory = factory;
            return this;
        }

        /**
         * Sets the health check interval in seconds.
         *
         * @param interval the interval in seconds
         * @return the Builder instance for method chaining
         */
        public Builder withCheckIntervalSeconds(long interval) {
            this.checkIntervalSeconds = interval;
            return this;
        }

        /**
         * Builds a new HealthChecker instance with the provided configuration.
         *
         * @return a new HealthChecker instance
         * @throws IllegalStateException if the CacheServiceGrpcFactory is not set
         */
        public HealthChecker build() {
            if (factory == null) {
                throw new IllegalStateException("CacheServiceGrpcFactory must be provided");
            }
            return new HealthChecker(this);
        }

        /**
         * Builds a new HealthChecker instance and starts periodic health checks.
         *
         * @return a new HealthChecker instance with health checks started
         * @throws IllegalStateException if the CacheServiceGrpcFactory is not set
         */
        public HealthChecker buildAndStart() {
            if (factory == null) {
                throw new IllegalStateException("CacheServiceGrpcFactory must be provided");
            }
            HealthChecker healthChecker = new HealthChecker(this);
            // Start health checks after building
            healthChecker.startPeriodicHealthChecks(); 
            return healthChecker;
        }
    }
}
