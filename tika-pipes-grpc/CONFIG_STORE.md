# Tika Pipes gRPC Config Store Options

## Overview

Tika Pipes supports two different configuration store backends for managing fetchers, emitters, pipe iterators, and job status:

1. **In-Memory Store (Default)** - Lightweight, no external dependencies
2. **Apache Ignite Store** - Distributed cache for multi-instance deployments

## In-Memory Config Store (Default)

The in-memory configuration store is enabled by default and requires no additional setup. It stores all configurations in memory using concurrent hash maps. This is ideal for:

- Single-instance deployments
- Development and testing
- Scenarios where you want to sync configs over gRPC without managing Ignite infrastructure

### Usage

Simply start your Tika Pipes server without any special configuration. The server will automatically use in-memory repositories.

```bash
java -jar tika-pipes-grpc.jar
```

### Configuration Sync via gRPC

With the in-memory store, all configuration synchronization happens via gRPC API calls:

- **Save Configuration**: Use `saveFetcher`, `saveEmitter`, or `savePipeIterator` gRPC methods
- **Get Configuration**: Use `getFetcher`, `getEmitter`, or `getPipeIterator` gRPC methods
- **List Configurations**: Use `listFetchers`, `listEmitters`, or `listPipeIterators` gRPC methods
- **Delete Configuration**: Use `deleteFetcher`, `deleteEmitter`, or `deletePipeIterator` gRPC methods

This allows you to manage configurations across multiple clients without needing a distributed cache like Ignite.

## Apache Ignite Config Store

To enable the Apache Ignite-based configuration store for distributed deployments, activate the `ignite` Spring profile:

```bash
java -jar tika-pipes-grpc.jar --spring.profiles.active=ignite
```

### Configuration

You can configure Ignite's work directory using the `ignite.workDir` property:

```bash
java -jar tika-pipes-grpc.jar --spring.profiles.active=ignite --ignite.workDir=/path/to/ignite/work
```

Or via `application.properties`:

```properties
spring.profiles.active=ignite
ignite.workDir=/path/to/ignite/work
```

## Architecture

### In-Memory Implementation

The in-memory repositories are implemented in the `org.apache.tika.pipes.repo.memory` package:

- `InMemoryFetcherRepository` - Stores fetcher configurations
- `InMemoryEmitterRepository` - Stores emitter configurations
- `InMemoryPipeIteratorRepository` - Stores pipe iterator configurations
- `InMemoryJobStatusRepository` - Stores job status

These repositories use `ConcurrentHashMap` for thread-safe operations and implement the base repository interfaces.

### Ignite Implementation

The Ignite repositories are implemented in the `org.apache.tika.pipes.repo` package and extend `IgniteRepository` for distributed caching capabilities.

## Choosing the Right Store

| Feature | In-Memory Store | Apache Ignite Store |
|---------|----------------|---------------------|
| Setup Complexity | Simple | Moderate |
| External Dependencies | None | Requires Ignite |
| Persistence | None (in-memory only) | Configurable |
| Multi-Instance | Via gRPC sync | Native distributed cache |
| Performance | Fast (local) | Fast (with caching) |
| Use Case | Single instance, gRPC-based sync | Multi-instance clusters |

## Example: Using In-Memory Store with Multiple Clients

```java
// Client 1: Save a fetcher configuration
ManagedChannel channel = ManagedChannelBuilder
    .forAddress("localhost", 9090)
    .usePlaintext()
    .build();

TikaGrpc.TikaBlockingStub stub = TikaGrpc.newBlockingStub(channel);

// Save fetcher config
SaveFetcherRequest request = SaveFetcherRequest.newBuilder()
    .setFetcherId("my-fetcher")
    .setPluginId("filesystem-fetcher")
    .setFetcherConfigJson("{\"basePath\": \"/data\"}")
    .build();

stub.saveFetcher(request);

// Client 2: Retrieve the same fetcher configuration
GetFetcherRequest getRequest = GetFetcherRequest.newBuilder()
    .setFetcherId("my-fetcher")
    .build();

GetFetcherReply reply = stub.getFetcher(getRequest);
// reply contains the fetcher configuration saved by Client 1
```

This demonstrates how configurations can be synced across multiple clients via gRPC without requiring Ignite.
