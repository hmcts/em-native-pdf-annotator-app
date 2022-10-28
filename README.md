# Evidence Management Native PDF Annotator App
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://travis-ci.org/hmcts/rpa-native-pdf-annotator-app.svg?branch=master)](https://travis-ci.org/hmcts/rpa-native-pdf-annotator-app)
[![codecov](https://codecov.io/gh/hmcts/rpa-native-pdf-annotator-app/branch/master/graph/badge.svg)](https://codecov.io/gh/hmcts/rpa-native-pdf-annotator-app)

Native PDF Annotator is a backend service that manages native annotations on PDF documents. 
In addition, the Native PDF Annotator handles requests from the Media Viewer to markup sensitive content in a case document and subsequent requests to redact marked up content..

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

#### To clone repo and prepare to pull containers:
```
git clone https://github.com/hmcts/em-native-pdf-annotator-app.git
cd em-native-pdf-annotator-app/
```

#### Clean and build the application:

Requires docker desktop running

```
./gradlew clean
./gradlew build
```

#### To run the application:

VPN connection is required

```
az login
./gradlew bootWithCCD
```


### Swagger UI
To view our REST API go to http://{HOST}/swagger-ui/index.html
On local machine with server up and running, link to swagger is as below
> http://localhost:8080/swagger-ui/index.html
> if running on AAT, replace localhost with ingressHost data inside values.yaml class in the necessary component, making sure port number is also removed.

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

To run the project functional tests, first ensure you have run `./gradlew bootWithCCD` as in the above setup instructions, then run
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
