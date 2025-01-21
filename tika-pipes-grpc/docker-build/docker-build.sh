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

# Loop through tika-pipes-fetchers directories and copy the zip files
for dir in tika-pipes-fetchers/*/; do
    fetcher_name=$(basename "$dir")
    cp -v -r "${dir}target/${fetcher_name}-${TIKA_PIPES_VERSION}.zip" "${OUT_DIR}/plugins"
done

cp -v -r "tika-pipes-test/src/main/resources/log4j2.xml" "${OUT_DIR}/config"
cp -v -r "tika-pipes-grpc/src/main/resources/application.yaml" "${OUT_DIR}/config"
cp -v -r "tika-pipes-grpc/docker-build/start-tika-grpc.sh" "${OUT_DIR}/bin"

cp -v "tika-pipes-grpc/docker-build/Dockerfile" "${OUT_DIR}/Dockerfile"

cd "${OUT_DIR}" || exit

echo "Running docker build from directory: $(pwd)"

if [ -z "${MULTI_ARCH}" ]; then
  # build single arch
  docker build . -t "${TAG_NAME}"
else
  # build multi arch
  docker buildx create --name tikapipesbuilder
  # see https://askubuntu.com/questions/1339558/cant-build-dockerfile-for-arm64-due-to-libc-bin-segmentation-fault/1398147#1398147
  docker run --rm --privileged tonistiigi/binfmt --install amd64
  docker run --rm --privileged tonistiigi/binfmt --install arm64
  docker buildx build --builder=tikapipesbuilder . -t "${TAG_NAME}" --platform linux/amd64,linux/arm64 --push
  docker buildx stop tikapipesbuilder
fi

echo "Done running docker build for tag ${TAG_NAME}"
