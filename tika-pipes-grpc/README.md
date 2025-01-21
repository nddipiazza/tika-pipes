# Tika Pipes Grpc

The Tika Pipes Grpc service hosts the Tika Pipes service as a gRPC service.

# How do fetchers get stored across nodes in the cluster?

Apache ignite is used to store the fetchers across the cluster. 

The fetchers are stored in the `fetchers` cache. 

The key is the fetcher's ID and the value is the fetcher's configuration.

The fetchers are loaded into the service by the `FetcherManager`.
