ARG APP_INSIGHTS_AGENT_VERSION=3.2.6

FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY build/libs/rpa-native-pdf-annotator-app.jar /opt/app/

EXPOSE 8080
CMD ["rpa-native-pdf-annotator-app.jar"]
