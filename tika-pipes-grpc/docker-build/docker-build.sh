#!/bin/bash
# This script is intended to be run from Maven exec plugin during the package phase of maven build

if [ -z "${TIKA_PIPES_VERSION}" ]; then
    echo "Environment variable TIKA_PIPES_VERSION is required, and should match the maven project version of Tika Pipes"
    exit 1
fi

# Decide what docker tag -t to use
# First look for tag in a TAG_NAME env variable.
if [ -z "${TAG_NAME}" ]; then
    TAG_NAME=$1
fi
# If TAG_NAME not specified, use TIKA_PIPES_VERSION
if [ -z "${TAG_NAME}" ]; then
    TAG_NAME="${TIKA_PIPES_VERSION}"
fi

# Remove '-SNAPSHOT' from the version string
TAG_NAME="${TAG_NAME//-SNAPSHOT/}"

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

cd "${SCRIPT_DIR}/../../" || exit

OUT_DIR=target/tika-docker

mkdir -p "${OUT_DIR}/libs"
mkdir -p "${OUT_DIR}/plugins"
mkdir -p "${OUT_DIR}/config"
mkdir -p "${OUT_DIR}/bin"
cp -v -r "tika-pipes-grpc/target/tika-pipes-grpc-${TIKA_PIPES_VERSION}.jar" "${OUT_DIR}/libs"

cp -v -r "tika-pipes-fetchers/tika-fetcher-file-system/target/tika-fetcher-file-system-${TIKA_PIPES_VERSION}.zip" "${OUT_DIR}/plugins"
cp -v -r "tika-pipes-fetchers/tika-fetcher-http/target/tika-fetcher-http-${TIKA_PIPES_VERSION}.zip" "${OUT_DIR}/plugins"
cp -v -r "tika-pipes-fetchers/tika-fetcher-microsoft-graph/target/tika-fetcher-microsoft-graph-${TIKA_PIPES_VERSION}.zip" "${OUT_DIR}/plugins"

cp -v -r "tika-pipes-test/src/main/resources/log4j2.xml" "${OUT_DIR}/config"
cp -v -r "tika-pipes-grpc/docker-build/start-tika-grpc.sh" "${OUT_DIR}/bin"

cp -v "tika-pipes-grpc/docker-build/Dockerfile" "${OUT_DIR}/Dockerfile"

cd "${OUT_DIR}" || exit

echo "Running docker build from directory: $(pwd)"

# build single arch
docker build . -t "${TAG_NAME}"

#docker buildx create --name tikapipesbuilder
## see https://askubuntu.com/questions/1339558/cant-build-dockerfile-for-arm64-due-to-libc-bin-segmentation-fault/1398147#1398147
#docker run --rm --privileged tonistiigi/binfmt --install amd64
#docker run --rm --privileged tonistiigi/binfmt --install arm64
#docker buildx build --builder=tikapipesbuilder . -t "${TAG_NAME}" --platform linux/amd64,linux/arm64 --push
#docker buildx stop tikapipesbuilder

echo "Done running docker build for tag ${TAG_NAME}"
