package dev.ixixpercent.cache.connector;

import dev.ixixpercent.cache.grpc.CacheServiceGrpc;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Factory class for managing CacheServiceGrpc.CacheServiceBlockingStub instances.
 * Provides stubs in a round-robin fashion from a dynamically managed list of nodes.
 */
@Slf4j
public class CacheServiceGrpcFactory {

  // Holds channels and stubs based on host-port key
  private final Map<String, ManagedChannel> channelCache = new ConcurrentHashMap<>();
  private final Map<String, CacheServiceGrpc.CacheServiceBlockingStub> stubCache = new ConcurrentHashMap<>();

  // Thread-safe list of nodes for round-robin selection
  private final CopyOnWriteArrayList<String> nodes = new CopyOnWriteArrayList<>();

  // Atomic counter for round-robin index
  private final AtomicInteger roundRobinIndex = new AtomicInteger(0);


  // TODO extract in a health checker component
  // Executor for asynchronous health checks
  private final ScheduledExecutorService healthCheckExecutor = Executors.newScheduledThreadPool(1);

  public CacheServiceGrpcFactory() {
    // Schedule periodic health checks
    // wait an initial period
    healthCheckExecutor.scheduleAtFixedRate(this::performHealthChecks, 10, 10, SECONDS);
  }

  /**
   * Adds a node to the factory. If the node already exists, it does nothing.
   *
   * @param host Hostname of the node
   * @param port Port of the node
   */
  public void addNode(String host, int port) {
    String key = createKey(host, port);
    boolean added = nodes.addIfAbsent(key);
    if (added) {
      log.info("Node added: {}", key);
      // Initialize stub to ensure it's cached
      getStubFromCache(key);
    } else {
      log.warn("Node {} already exists. Skipping add.", key);
    }
  }

  /**
   * Removes a node from the factory. If the node does not exist, it does nothing.
   *
   * @param host Hostname of the node
   * @param port Port of the node
   */
  public void removeNode(String host, int port) {
    String key = createKey(host, port);
    boolean removed = nodes.remove(key);
    if (removed) {
      log.info("Node removed: {}", key);
      invalidateNode(host, port);
    } else {
      log.warn("Node {} does not exist. Skipping removal.", key);
    }
  }

  /**
   * Returns a CacheServiceBlockingStub using round-robin selection from the available nodes.
   *
   * @return CacheServiceGrpc.CacheServiceBlockingStub
   * @throws IllegalStateException if no nodes are available
   */
  public CacheServiceGrpc.CacheServiceBlockingStub getStub() {
    if (nodes.isEmpty()) {
      throw new IllegalStateException("No available nodes to create a stub.");
    }

    // Calculate the current index in a thread-safe manner
    int index = Math.abs(roundRobinIndex.getAndIncrement()) % nodes.size();
    String key = nodes.get(index);

    log.trace("Getting stub for node {}", key);
    // Retrieve the stub from the cache, creating it if necessary
    return getStubFromCache(key);
  }

  /**
   * Invalidates a specific node's channel and stub.
   *
   * @param host Hostname of the node
   * @param port Port of the node
   */
  private void invalidateNode(String host, int port) {
    String key = createKey(host, port);

    // Shutdown and remove the channel
    ManagedChannel channel = channelCache.remove(key);
    if (channel != null) {
      channel.shutdown();
      log.info("Channel shutdown for node {}", key);
    }

    // Remove the stub from the cache
    CacheServiceGrpc.CacheServiceBlockingStub stub = stubCache.remove(key);
    if (stub != null) {
      log.info("Stub removed from cache for node {}", key);
    }
  }

  /**
   * Shuts down all channels and clears caches.
   */
  public void shutdownAllChannels() {
    channelCache.forEach((key, channel) -> {
      channel.shutdown();
      log.info("Shutting down channel for {}", key);
    });
    channelCache.clear();
    stubCache.clear();
    nodes.clear();
    log.info("All channels and stubs have been shutdown and caches cleared.");
  }

  /**
   * Helper method to create a unique key from host and port.
   *
   * @param host Hostname
   * @param port Port
   * @return String key in the format "host:port"
   */
  private String createKey(String host, int port) {
    return host + ":" + port;
  }

  /**
   * Retrieves a stub from the cache or creates a new one if it doesn't exist.
   *
   * @param key Unique key representing the node
   * @return CacheServiceGrpc.CacheServiceBlockingStub
   */
  private CacheServiceGrpc.CacheServiceBlockingStub getStubFromCache(String key) {
    return stubCache.computeIfAbsent(key, k -> {
      String[] parts = k.split(":");
      log.info("Creating new stub for node {}", key);
      return CacheServiceGrpc.newBlockingStub(getChannel(parts[0], Integer.parseInt(parts[1])));
    });
  }

  /**
   * Retrieves a ManagedChannel from the cache or creates a new one if it doesn't exist.
   *
   * @param host Hostname of the node
   * @param port Port of the node
   * @return ManagedChannel
   */
  private ManagedChannel getChannel(String host, int port) {
    String key = createKey(host, port);
    return channelCache.computeIfAbsent(key, k -> {
      log.info("Creating new channel for node {}", key);
      // Use plaintext
      return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    });
  }


  /**
   * Performs health checks on all nodes and removes any that are unhealthy.
   */
  private void performHealthChecks() {
    log.info("Performing health checks on all nodes...");
    for (String key : nodes) {
      String[] parts = key.split(":");
      String host = parts[0];
      int port = Integer.parseInt(parts[1]);
      //TODO add more sophisticated health checking

      ManagedChannel channel = channelCache.get(key);
      if (channel == null || channel.isShutdown() || channel.isTerminated()) {
        log.warn("Channel for node {} is shutdown or terminated. Removing node.", key);
        removeNode(host, port);
      }
      try {
        ConnectivityState state = channel.getState(true);
        switch (state) {
          case CONNECTING, READY, IDLE -> {
            log.trace("Node {} responded, channel is in state {}", key, state);
            // NOOP
          }
          case TRANSIENT_FAILURE -> {
            log.warn("Channel for node {} is in failure. Removing node.", key);
            removeNode(host, port);
          }
          case SHUTDOWN -> {
            log.warn("Channel for node {} is in shutdown. Removing node.", key);
            removeNode(host, port);
          }
        }
      } catch (RuntimeException e) {
        log.error("Failed to perform health checks on node {}", key, e);
        removeNode(host, port);
      }
    }
  }
}

