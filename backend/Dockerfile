FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN apt-get update \
    && apt-get install -y maven \
    && mvn clean package -DskipTests \
    && rm -rf /var/lib/apt/lists/*


FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Install FFmpeg, curl and unzip
RUN apt-get update \
    && apt-get install -y \
       ffmpeg \
       curl \
       unzip \
    && rm -rf /var/lib/apt/lists/*

# Install yt-dlp
RUN curl -L \
       https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp \
       -o /usr/local/bin/yt-dlp \
    && chmod +x /usr/local/bin/yt-dlp

# Install Deno
RUN curl -fsSL https://deno.land/install.sh | sh

ENV PATH="/root/.deno/bin:${PATH}"

ENV YT_DLP_PATH=/usr/local/bin/yt-dlp
ENV FFMPEG_PATH=/usr/bin/ffmpeg
ENV DOWNLOAD_DIR=/app/downloads

COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/downloads

EXPOSE 8080

CMD ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]