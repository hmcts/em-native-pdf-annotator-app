FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.2

COPY build/libs/rpa-native-pdf-annotator-app.jar /opt/app/

CMD ["rpa-native-pdf-annotator-app.jar"]

EXPOSE 8080
