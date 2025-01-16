# Tika Pipes

Tika Pipes is a collection of Java services and libraries that handles the large scale downloading of files, 
then parsing of the downloaded content using Apache Tika.

# Build prerequisites

You will need ability to execute .sh files.

# Build steps

A docker build script will prepare and run the docker build to prepare the tika-pipes docker image.

By default, the script will tag the image with the tika pipes version.

To specify a custom tag, specify the ENV variable `TAG_NAME`. For example: `TAG_NAME=ndipiazza/tika-grpc:3.0.0-beta5`

To run docker in multi-arch build mode, specify ENV variable `MULTI_ARCH=true`

# Fetcher architecture

Each Tika Pipes fetcher is implemented as a pf4j plugin.

There are two Pf4j extensions for Fetchers: One for the Fetcher code, and one for the FetcherConfig.

Fetchers are Maven Java Jar modules that contain the following key files:

* `tika-pipes-fetchers/tika-fetcher-YOURFETCHER/src/main/assembly.xml` - maven assembly. Tells maven how to build the package.
* `tika-pipes-fetchers/tika-fetcher-YOURFETCHER/src/main/java/org/apache/tika/pipes/fetchers/YOURFETCHER/config/YOURFETCHERFetcherConfig.java` - Custom configuration properties for the fetcher.
* `tika-pipes-fetchers/tika-fetcher-YOURFETCHER/src/main/java/org/apache/tika/pipes/fetchers/YOURFETCHER/FETCHERFetcher.java` - The fetcher code.
* `tika-pipes-fetchers/tika-fetcher-YOURFETCHER/src/main/java/org/apache/tika/pipes/fetchers/YOURFETCHER/FETCHERPlugin.java` - The pf4j plugin with start/stop event handler at the pf4j plugin level.
* `tika-pipes-fetchers/tika-fetcher-YOURFETCHER/src/main/resources/plugin.properties` - pf4j plugin properties file (see https://pf4j.org/doc/plugins.html)

When packaged, they will be built to a `.zip` file format.

# How to add a new feature

Copy one of the existing folders in tika-pipes-fetchers to `tika-pipes-fetchers/tika-fetcher-YOURFETCHER` that most closely matches your new Fetcher.

Update `tika-pipes-fetchers/tika-fetcher-YOURFETCHER/pom.xml`

Update groupId, artifactId, to match your project.

Update the Maven project dependencies:

* Remove the dependencies from the fetcher you copied from that you do not need. 
* Add the dependency your project needs as you need them.

All the java classes for the fetcherconfig, fetcher and plugin need to be refactored to your fetcher's name.



