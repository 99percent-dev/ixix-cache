# Cache Library

This library provides a caching mechanism that can be used to store and retrieve data efficiently. It is designed to be
flexible and can be integrated into various applications.

## Features

- Generic JSR-107 cache implementation with customizable key-value types.
- Supports basic cache operations such as get, put, remove, and clear.
- Includes a near cache and remote cache implementation.
- Provides serialization support for cache entries using Kyro.

## Usage

To use the cache library, include it as a dependency in your project. You can then create an instance of the cache and
perform operations like storing and retrieving data.

## gRPC Communication

The `CacheServiceGrpcFactory` class in this library utilizes gRPC to manage connections to cache nodes. It
provides a factory for creating and managing `CacheServiceGrpc.CacheServiceBlockingStub` instances, which
are used to communicate with remote cache services. The factory supports round-robin selection of nodes and
performs health checks to ensure connectivity.

## Components

- **IxIxCache**: The main cache implementation.
- **IxIxCacheEntry**: Represents a single cache entry.
- **CacheStoreManager**: Manages the storage of cache entries.
- **NearCache**: A local cache implementation for fast access.
- **RemoteCache**: A distributed cache implementation for scalability.

