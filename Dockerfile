ARG APP_INSIGHTS_AGENT_VERSION=3.4.18

FROM hmctsprod.azurecr.io/base/java:25-distroless

USER hmcts
COPY lib/applicationinsights.json /opt/app/

COPY build/libs/rpa-native-pdf-annotator-app.jar /opt/app/

EXPOSE 8080
CMD ["rpa-native-pdf-annotator-app.jar"]
