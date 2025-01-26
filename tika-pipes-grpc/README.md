# Tika Pipes Grpc

The Tika Pipes Grpc service hosts the Tika Pipes service as a gRPC service.

# How do objects such as Fetchers and Emitters get stored across nodes in the cluster?

Apache ignite is used to store the fetchers across the cluster. 

The fetchers are stored in the `FetcherCache` cache, the emitters are stored in the `EmitterCache` cache, etc. 
