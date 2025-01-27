# Tika Pipes

Tika Pipes is a Java Grpc web service that handles the large scale downloading of files, 
then parsing of the downloaded content using Apache Tika.

Tika Pipes plugins can be used to create customizable fetchers, pipe iterators, and emitters.

## What is Tika Pipes Built With
* Java 17
* gRPC
* Spring Boot 3.x
* Apache Tika
* Apache Ignite

## How to Start Tika Pipes Server

The [tika-pipes-grpc](tika-pipes-grpc) module is a Spring Boot powered gRPC service that can be used to download and parse files.

To start Tika grpc, see the following start script: [start-tika-grpc.sh](tika-pipes-grpc%2Fdocker-build%2Fstart-tika-grpc.sh)

## Tika Pipes Architecture

Tika pipes services are described in the following protobuf file: [tika.proto](tika-pipes-proto%2Fsrc%2Fmain%2Fproto%2Ftika.proto)

Tika Pipes Grpc Service is made up of the following components:

* [Tika Parse Service](tika-pipes-core%2Fsrc%2Fmain%2Fjava%2Forg%2Fapache%2Ftika%2Fpipes%2Fcore%2Fparser%2FParseService.java) - The Tika Pipes service contains a Apache Tika service to convert the Documents into parsed Tika text metadata.
* [Tika Fetchers](tika-pipes-fetchers%2Ftika-fetchers-core%2Fsrc%2Fmain%2Fjava%2Forg%2Fapache%2Ftika%2Fpipes%2Ffetchers%2Fcore%2FFetcher.java) - Tika Pipes Plugin type that fetches the binary document from a source, authenticating if necessary. 
* [Tika Pipe Iterators](tika-pipes-pipe-iterators%2Ftika-pipe-iterators-core%2Fsrc%2Fmain%2Fjava%2Forg%2Fapache%2Ftika%2Fpipes%2Fcore%2Fiterators%2FPipeIterator.java) - Tika Pipes Plugin type can identify files to fetch over a content source. For example, you might use 
a CSV pipe iterator if you have a CSV file that contains URLs to fetch.
* [Tika Emitters](tika-pipes-emitters%2Ftika-emitters-core%2Fsrc%2Fmain%2Fjava%2Forg%2Fapache%2Ftika%2Fpipes%2Fcore%2Femitter%2FEmitter.java) - Tika Pipes Plugin type that emits the parsed Tika metadata to a destination. For example, you 
might use an Apache Solr emitter if you would like to emit your parsed documents to an Apache search index.

### Tika Pipes Jobs

The Tika Grpc service has create/read/update/delete services for Tika Pipes fetchers, pipe iterators, and emitters.

The Tika Grpc service has a `runPipeJob` service that takes 3 arguments:
* Pipe iterator ID
* Fetcher ID
* Emitter ID

The Pipe Job will open a bidirectional stream the Grpc `fetchAndParse` service. 

The Pipe Iterator then streams in the documents to fetch.

As the fetchAndParse method handles the fetch input requests, it will use the Fetcher to obtain the binary contents, 
then Tika Parse Service to parse the document to Tika Text Metadata. 

The method responds with stream of `FetchAndParseReply` objects from each record parsed from the input.

Finally, the Emitter takes these parsed Tika metadata objects and emits them to a destination.

![tika-pipes-jobs.drawio.png](readme-files%2Ftika-pipes-jobs.drawio.png)
![tika-pipes-jobs-inner.drawio.png](readme-files%2Ftika-pipes-jobs-inner.drawio.png)

For an example client, see [TikaServerImplPipeJobTest.java](tika-pipes-grpc%2Fsrc%2Ftest%2Fjava%2Forg%2Fapache%2Ftika%2Fpipes%2Fgrpc%2FTikaServerImplPipeJobTest.java) for an example.

Basically you can see it starts the managed channel, creates the pipe iterator, emitter and fetcher, then runs the pipe job.

You can then use the `jobStatus` service as the job progresses and eventually will show when the job has completed.

### Accessing Tika Pipes Grpc Directly to Fetch and Parse Documents

There are situations where you already have code that has the input documents available (no need for the Pipe Iterator),
and you would then like to send the parsed output to your own destination (no need for the Emitter).

In this use-case, you can use the Tika Grpc services for `saveFetcher` to create a Fetcher, 
open a channel to Grpc, then access `fetchAndParse` yourself.

We have external facing tests to demonstrate the use of Tika Pipes and each Fetcher direct usage.

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

For example:

```bash
MULTI_ARCH=false DOCKER_ID=ndipiazza PROJECT_NAME=tika-pipes RELEASE_IMAGE_TAG=3.0.0-beta20 mvn clean package
```

Would result in:

```
 ===================================================================================================
 Done running docker build with tag -t ndipiazza/tika-pipes:3.0.0-beta20
 ===================================================================================================
```
# Docker Usage
You can pull down the version you would like using:

`docker pull ndipiazza/tika-pipes:<version>`

Then to run the container, execute the following command:

`docker run -d -p 127.0.0.1::50051 ndipiazza/tika-pipes:<version>`

Where <version> is the Apache Tika Server version - e.g. 3.0.0
