package dev.ixixpercent.cache.node;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class CacheNode {

  private final int port;
  private final Server server;

  public CacheNode(int port) {
    this.port = port;
    this.server = ServerBuilder.forPort(port).addService(new CacheServiceImpl()).build();
  }

  public void start() throws IOException {
    server.start();
    log.info("Server started, listening on {}", port);

    // Add shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("*** shutting down gRPC server since JVM is shutting down");
      CacheNode.this.stop();
      log.info("*** server shut down");
    }));
  }

  public void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  // Await termination on the main thread since the grpc library uses daemon threads.
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    if (args.length < 1) {
      log.error("Usage: CacheNode <port>");
      System.exit(1);
    }

    int port = Integer.parseInt(args[0]);
    CacheNode server = new CacheNode(port);

    server.start();
    server.blockUntilShutdown();
  }
}
