ARG APP_INSIGHTS_AGENT_VERSION=2.5.1

FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.4

COPY build/libs/rpa-native-pdf-annotator-app.jar lib/applicationinsights-agent-2.5.1.jar lib/AI-Agent.xml /opt/app/

EXPOSE 8080
CMD ["rpa-native-pdf-annotator-app.jar"]
