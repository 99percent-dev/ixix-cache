# Overview

This document provides an overview of the architecture of components detailing the design and interaction of various
modules within the cache system.

## Overview

The cache system is designed to provide a scalable caching mechanism that supports both local and
distributed caching strategies. It adheres to the JSR-107 (Java Caching) standard, which provides a standardized API for
cache operations, ensuring compatibility and ease of integration with other systems. The choice of JSR-107 allows for a
consistent and well-defined approach to caching, allow this cache to be a drop in replacement for other caching system
that used the same standard.

Additionally, it leverages gRPC for communication between cache nodes and provides a flexible API for cache operations.

## Spring Cache Integration

The cache system works with Spring Cache.

### Pros:

- **Ease of Integration**: Spring Cache offers straightforward annotations and configuration, making it easy to
  integrate with existing Spring applications.
- **Abstraction**: It abstracts the underlying cache implementation, allowing developers to switch between different
  cache providers with minimal code changes.
- **Support for Multiple Cache Providers**: Spring Cache supports various cache providers, including Ehcache, Hazelcast,
  and Redis, providing flexibility in choosing the backend.

Unless there are specific requirements (performance, functional) that cannot be covered by using Spring Cache it is
better
to use standard implementation as it allows for swapping caching implementations later on if necessary.

## Components

### 1. Cache Library (`cache_lib`)

- **IxIxCache**: This is the core cache implementation that provides basic cache operations such
  as `get`, `put`, `remove`, and `clear`.
  It is designed to be generic, allowing for customizable key-value types.

- **IxIxCacheEntry**: Represents individual cache entries, encapsulating the key-value pairs stored in the cache.

- **CacheStoreManager**: Manages the storage and retrieval of cache entries. It acts as an intermediary between the
  cache and the underlying storage mechanism.

- **RemoteCache**: Implements a distributed cache using gRPC to communicate with remote cache nodes. It supports
  operations like `put`, `get`, and `remove` across network boundaries.

- **CacheServiceGrpcFactory**: A factory class responsible for creating and managing gRPC stubs for communication with
  cache nodes.
  It uses a round-robin strategy for load balancing and performs health checks to ensure node availability. This class
  manages
  connections to multiple cache nodes, providing a robust mechanism for distributed caching. It handles the lifecycle
  of gRPC channels and stubs, ensuring efficient resource utilization and fault tolerance.

### 2. Cache Node (`cache_node`)

- **CacheServiceImpl**: Extends the gRPC service base class to implement the cache service. It
  handles gRPC requests for cache operations and interacts with the local cache to perform these operations.

- **Serialization**: Utilizes Kryo for efficient serialization and deserialization of cache entries,
  ensuring that data can be efficiently transmitted over the network.

### 3. Cache Tester (`cache_tester`)

The `cache_tester` is used to test the functionality and performance of the cache by exposing basic endpoints to be
cached.

Cache tester can be started as Spring Boot application using `./gradlew bootRun`

## Communication

The system uses gRPC for communication between different cache nodes. The `CacheServiceGrpcFactory` manages the
creation of gRPC channels and stubs, ensuring efficient and reliable communication. It supports load balancing and
health checks to maintain a robust connection to cache nodes.

## Deployment

The cache node application is packaged as a Docker container, to allow for deployment and scaling.
The provided `run.sh` is used for building and running the Docker container, making
it simple to deploy the cache node service on different environments.

## Notes on running

Currently, the **CacheServiceGrpcFactory** uses a hardcoded list of ports and hosts to connect to. The list of ports
and hosts is meant to maintained dynamically by another component which keeps an up to date list of available nodes.

**VERY IMPORTANT** The list is hardcoded in **IxIxCacheManager**
