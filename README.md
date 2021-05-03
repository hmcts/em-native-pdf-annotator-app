# Evidence Management Native PDF Annotator App
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://travis-ci.org/hmcts/rpa-native-pdf-annotator-app.svg?branch=master)](https://travis-ci.org/hmcts/rpa-native-pdf-annotator-app)
[![codecov](https://codecov.io/gh/hmcts/rpa-native-pdf-annotator-app/branch/master/graph/badge.svg)](https://codecov.io/gh/hmcts/rpa-native-pdf-annotator-app)

Native PDF Annotator is a backend service that manages native annotations on PDF documents. 
In addition the Native PDF Annotator handles requests from the Media Viewer to markup sensitive content in a case document and subsequent requests to redact marked up content..

### Tech

It uses:

* Java11
* Spring boot
* Junit, Mockito and SpringBootTest
* Gradle
* [lombok project](https://projectlombok.org/) - Lombok project

### Plugins
* [lombok plugin](https://plugins.jetbrains.com/idea/plugin/6317-lombok-plugin) - Lombok IDEA plugin

## Quickstart
```bash
#Cloning repo and running dependencies through docker

git clone git@github.com:hmcts/em-native-pdf-annotator-app.git
cd em-native-pdf-annotator-app/

az login
az acr login --name hmctspublic
docker-compose -f docker-compose-dependencies-simulator.yml pull
docker-compose -f docker-compose-dependencies-simulator.yml up

wait for 2-3 minutes till all the dependencies in the docker are up and running.

./gradlew clean
./gradlew build

./gradlew migratePostgresDatabase
./gradlew bootRun
```

### Swagger UI
To view our REST API go to {HOST}:{PORT}/swagger-ui.html
> http://localhost:8080/swagger-ui.html

### API Endpoints
A list of our endpoints can be found here
> https://hmcts.github.io/reform-api-docs/swagger.html?url=https://hmcts.github.io/reform-api-docs/specs/rpa-native-pdf-annotator-app.json

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
./gradlew build
```

To run the project unit tests execute the following command:

```
./gradlew test
```

To run the project functional tests, first ensure all project dependency Docker containers have started and you have run `./gradlew bootRun` as in the above setup instructions, then run
```
./gradlew functional 
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

### Running contract or pact tests:

You can run contract or pact tests as follows:

```
./gradlew clean
```

```
./gradlew contract
```

You can then publish your pact tests locally by first running the pact docker-compose:

```
docker-compose -f docker-pactbroker-compose.yml up
```

and then using it to publish your tests:

```
./gradlew pactPublish
```

Checking PR build
