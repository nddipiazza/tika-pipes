TAG_NAME=$1

if [ -z "${TAG_NAME}" ]; then
    echo "Single command line argument is required which will be used as the -t parameter of the docker build command"
    exit 1
fi

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

mvn clean install || exit

OUT_DIR=target/tika-docker

project_version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

mkdir -p "${OUT_DIR}/libs"
mkdir -p "${OUT_DIR}/plugins"
mkdir -p "${OUT_DIR}/config"
cp -r "tika-pipes-grpc/target/tika-pipes-grpc-${project_version}.jar" "${OUT_DIR}/libs"

cp -r "tika-pipes-fetchers/tika-fetcher-file-system/target/tika-fetcher-file-system-${project_version}.zip" "${OUT_DIR}/plugins"
cp -r "tika-pipes-fetchers/tika-fetcher-http/target/tika-fetcher-http-${project_version}.zip" "${OUT_DIR}/plugins"
cp -r "tika-pipes-fetchers/tika-fetcher-microsoft-graph/target/tika-fetcher-microsoft-graph-${project_version}.zip" "${OUT_DIR}/plugins"

cp -r "tika-pipes-test/src/test/resources/log4j2.xml" "${OUT_DIR}/config"
cp -r "tika-pipes-grpc/docker-build/start-tika-grpc.sh" "${OUT_DIR}"

cp "tika-pipes-grpc/docker-build/Dockerfile" "${OUT_DIR}/Dockerfile"

cd "${OUT_DIR}" || exit

# build single arch
#docker build "${OUT_DIR}" -t "${TAG_NAME}"

# Or we can build multi-arch - https://www.docker.com/blog/multi-arch-images/
docker buildx create --name tikabuilder
# see https://askubuntu.com/questions/1339558/cant-build-dockerfile-for-arm64-due-to-libc-bin-segmentation-fault/1398147#1398147
docker run --rm --privileged tonistiigi/binfmt --install amd64
docker run --rm --privileged tonistiigi/binfmt --install arm64
docker buildx build --builder=tikabuilder "${OUT_DIR}" -t "${TAG_NAME}" --platform linux/amd64,linux/arm64 --push
docker buildx stop tikabuilder
