ARG APP_INSIGHTS_AGENT_VERSION=2.3.1
FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.1

COPY build/libs/rpa-native-pdf-annotator-app.jar lib/applicationinsights-agent-2.3.1.jar lib/AI-Agent.xml /opt/app/

CMD ["rpa-native-pdf-annotator-app.jar"]

EXPOSE 8080
