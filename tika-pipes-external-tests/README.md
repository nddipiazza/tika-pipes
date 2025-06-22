# Tika Pipes External tests

These test use TestContainers to pull down a Tika Pipes server via docker and then run tests against it.

## Files used by the tests

These tests will download files from digi corpa dump here: https://corp.digitalcorpora.org/corpora/files/govdocs1/zipfiles

These are backed by S3 and publicly funded by Amazon.

The tests will begin by downloading the zip file(s) from the above URL, then extracting the files, and then running the tests. The tests will now use these files in order to test the Tika Pipes functionality.

## JVM system properties you can use to customize the test

`-Dgovdocs1.fromIndex` - The first digital corpora file to download from the zip folder. min is 1. max is 999. Default: 1 
`-Dgovdocs1.toIndex` - the last digital corpora file to download from the zip folder. min is 1. max is 999. Default: 1
