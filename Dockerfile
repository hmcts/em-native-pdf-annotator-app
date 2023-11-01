ARG APP_INSIGHTS_AGENT_VERSION=3.4.10

FROM hmctspublic.azurecr.io/base/java:21-distroless

COPY lib/applicationinsights.json /opt/app/

COPY build/libs/rpa-native-pdf-annotator-app.jar /opt/app/

EXPOSE 8080
CMD ["rpa-native-pdf-annotator-app.jar"]
