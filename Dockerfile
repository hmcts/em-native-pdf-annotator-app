ARG APP_INSIGHTS_AGENT_VERSION=2.5.1-BETA

FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.2

COPY build/libs/rpa-native-pdf-annotator-app.jar lib/AI-Agent.xml /opt/app/

EXPOSE 8080
CMD ["rpa-native-pdf-annotator-app.jar"]
