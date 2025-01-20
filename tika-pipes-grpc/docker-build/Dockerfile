FROM ubuntu:latest
COPY libs/ /tika/libs/
COPY plugins/ /tika/plugins/
COPY config/ /tika/config/
COPY bin/ /tika/bin
ARG JRE='openjdk-17-jre-headless'
ARG VERSION='3.0.0-SNAPSHOT'
RUN set -eux \
    && apt-get update \
    && apt-get install --yes --no-install-recommends gnupg2 software-properties-common \
    && DEBIAN_FRONTEND=noninteractive apt-get install --yes --no-install-recommends $JRE \
        gdal-bin \
        tesseract-ocr \
        tesseract-ocr-eng \
        tesseract-ocr-ita \
        tesseract-ocr-fra \
        tesseract-ocr-spa \
        tesseract-ocr-deu \
    && echo ttf-mscorefonts-installer msttcorefonts/accepted-mscorefonts-eula select true | debconf-set-selections \
    && DEBIAN_FRONTEND=noninteractive apt-get install --yes --no-install-recommends \
        xfonts-utils \
        fonts-freefont-ttf \
        fonts-liberation \
        ttf-mscorefonts-installer \
        wget \
        cabextract \
    && apt-get clean -y \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

EXPOSE 50051
ENV TIKA_PIPES_VERSION=$VERSION
RUN chmod +x "/tika/bin/start-tika-grpc.sh"
ENTRYPOINT ["/tika/bin/start-tika-grpc.sh"]
