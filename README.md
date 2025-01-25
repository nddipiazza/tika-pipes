# Tika Pipes

Tika Pipes is a collection of Java services and libraries that handles the large scale downloading of files, 
then parsing of the downloaded content using Apache Tika.

## What is Tika Pipes Built With
* Java 17
* gRPC
* Spring Boot 3.x
* Apache Tika
* Apache Ignite

## How to Start Tika Pipes Server

The [tika-pipes-grpc](tika-pipes-grpc) module is a Spring Boot powered gRPC service that can be used to download and parse files.

To start Tika grpc, see the following start script: [start-tika-grpc.sh](tika-pipes-grpc%2Fdocker-build%2Fstart-tika-grpc.sh)

## Tika Pipes Services

In order to use Tika Pipes, you need to use Tika Pipes service to 

* Create Fetchers
* Use the fetch and parser services to download and parse content.

Tika pipes services are described in the following protobuf file: [tika.proto](tika-pipes-proto%2Fsrc%2Fmain%2Fproto%2Ftika.proto)

### Example of Tika Pipes services

We have external facing tests to demonstrate the use of Tika Pipes and each Fetcher.

See the following package for the different Java client examples: [cli](tika-pipes-cli%2Fsrc%2Fmain%2Fjava%2Forg%2Fapache%2Ftika%2Fpipes%2Fcli)

In particular, this example is fairly basic and easier to see the core parts in action: [FileSystemFetcherCli.java](tika-pipes-cli%2Fsrc%2Fmain%2Fjava%2Forg%2Fapache%2Ftika%2Fpipes%2Fcli%2Ffilesystem%2FFileSystemFetcherCli.java)

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

* `RELEASE_IMAGE_TAG` will serve as the image tag across all deployed repositories. For example: `RELEASE_IMAGE_TAG=3.0.0-beta5`. Defaults to TIKA_PIPES_VERSION when unspecified.
* `AWS_ACCOUNT_ID` pushes the docker image to the specified AWS Account. `AWS_ACCOUNT_ID=<aws-account-id>`
* `AZURE_REGISTRY_NAME` pushes the docker image to the specified Azure registry. `AZURE_REGISTRY_NAME=<registry-name>`
* `DOCKER_ID` pushes the docker image to the specified account within Docker Hub. `DOCKER_ID=nddipiazza`
* `PROJECT_NAME` allows changing the name of the project, as part of the push to repositories. Defaults to `tika`
* `MULTI_ARCH` set this to true if you want to build for Multi-arch mode.

Here is an example:

```bash
MULTI_ARCH=true RELEASE_IMAGE_TAG=3.0.0-beta5 DOCKER_ID=ndipiazza mvn package
```

# Docker Usage
You can pull down the version you would like using:

`docker pull ndipiazza/tika-pipes:<version>`

Then to run the container, execute the following command:

`docker run -d -p 127.0.0.1::50051 ndipiazza/tika-pipes:<version>`

Where <version> is the Apache Tika Server version - e.g. 3.0.0
