TAG_NAME=$1

if [ -z "${TAG_NAME}" ]; then
    echo "Single command line argument is required which will be used as the -t parameter of the docker build command"
    exit 1
fi

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

cd "${SCRIPT_DIR}/../../" || exit

#mvn clean install || exit

OUT_DIR=target/tika-docker

project_version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

mkdir -p "${OUT_DIR}/libs"
mkdir -p "${OUT_DIR}/plugins"
mkdir -p "${OUT_DIR}/config"
mkdir -p "${OUT_DIR}/bin"
cp -v -r "tika-pipes-grpc/target/tika-pipes-grpc-${project_version}.jar" "${OUT_DIR}/libs"

cp -v -r "tika-pipes-fetchers/tika-fetcher-file-system/target/tika-fetcher-file-system-${project_version}.zip" "${OUT_DIR}/plugins"
cp -v -r "tika-pipes-fetchers/tika-fetcher-http/target/tika-fetcher-http-${project_version}.zip" "${OUT_DIR}/plugins"
cp -v -r "tika-pipes-fetchers/tika-fetcher-microsoft-graph/target/tika-fetcher-microsoft-graph-${project_version}.zip" "${OUT_DIR}/plugins"

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
