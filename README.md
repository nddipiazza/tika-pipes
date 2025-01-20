# Tika Pipes

Tika Pipes is a collection of Java services and libraries that handles the large scale downloading of files, 
then parsing of the downloaded content using Apache Tika.

## Starting Tika Pipes

The [tika-pipes-grpc](tika-pipes-grpc) module is a Spring Boot powered gRPC service that can be used to download and parse files.

To start Tika grpc, see the following start script: [start-tika-grpc.sh](tika-pipes-grpc%2Fdocker-build%2Fstart-tika-grpc.sh)

## Tika Pipes Services

In order to use Tika Pipes, you need to use Tika Pipes service to 

* Create Fetchers
* Use the fetch and parser services to download and parse content.

Tika pipes services are described in the following protobuf file: [tika.proto](tika-pipes-proto%2Fsrc%2Fmain%2Fproto%2Ftika.proto)

### Example of Tika Pipes services

We have external facing tests to demonstrate the use of Tika Pipes and each Fetcher.

See the following package for the different test examples: [pipes](tika-pipes-test%2Fsrc%2Fmain%2Fjava%2Fpipes)

In particular, this example is fairly basic and easier to see the core parts in action: [FileSystemFetcherExternalTest.java](tika-pipes-test%2Fsrc%2Fmain%2Fjava%2Fpipes%2Ffilesystem%2FFileSystemFetcherExternalTest.java)

## Tika Pipes Fetchers

More info about fetchers can be found in the [`tika-pipes-fetchers` module](tika-pipes-fetchers).

# Build Steps

The following are steps on how to build Tika Pipes.

You will need ability to execute .sh files. So if you are running from Mac or Linux you are good. I haven't tried Windows but
it should work WSL or Git Bash.

# Building

A docker build script will prepare and run the docker build to prepare the tika-pipes docker image.

By default, the script will tag the image with the tika pipes version.

When building a Docker image that you intend to use, when building you must specify some ENV variables:

* `TAG_NAME` will serve as the `-t` parameter for docker build to tag the built image. For example: `TAG_NAME=ndipiazza/tika-grpc:3.0.0-beta5`
* `MULTI_ARCH` set this to true if you want to build for Multi-arch mode.

Verify the image that is built is as expected, then push your image to the remote docker repository if necessary.

Here is an example:

```bash
MULTI_ARCH=true TAG_NAME=ndipiazza/tika-grpc:3.0.0-beta5 mvn package
docker push ${TAG_NAME}
```
