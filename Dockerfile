FROM hmcts/cnp-java-base:openjdk-8u181-jre-alpine3.8-1.0

MAINTAINER "HMCTS Team <https://github.com/hmcts>"
LABEL maintainer = "HMCTS Team <https://github.com/hmcts>"

# Mandatory!
ENV APP rpa-native-pdf-annotator-app.jar
ENV APPLICATION_TOTAL_MEMORY 512M
ENV APPLICATION_SIZE_ON_DISK_IN_MB 53

# Optional
ENV JAVA_OPTS ""

COPY build/libs/$APP /opt/app/
