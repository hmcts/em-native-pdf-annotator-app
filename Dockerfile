FROM hmcts/cnp-java-base:openjdk-8u191-jre-alpine3.9-2.0.1

COPY build/libs/rpa-native-pdf-annotator-app.jar /opt/app/

CMD ["rpa-native-pdf-annotator-app.jar"]

EXPOSE 8080
