Worksoft Execution Manager Jenkins Plugin
========================

### Introduction
Worksoft EM plugin for Jenkins


example Pipeline DSL script:

```groovy

pipeline {
    environment {
        emUser = credentials("executionManager")
    }
    stage('Execute Tests') {
        steps {
            em.login credentials: emUser
            em.executeRequest 'requestName'
            em.waitForCompletion
        }
    }
}
```

### Getting Started
Prerequisites:

* JDK 8 (or above)


### Build and Test
To build the plugin from source:

    ./gradlew build

 You also need to run the `localizer` task to generate the `Messages` class before building and testing the project in IntelliJ IDEA:

    ./gradlew localizer

Authors

To run Jenkins (http://localhost:8080) and test the plugin:

    ./gradlew server

Build .hpi file to be installed in Jenkins:

    ./gradlew jpi

To run all unit tests run `gradle test` Unlike with Maven, the tests will not be automatically run when building the plugin via gradle jpi, or when releasing via gradle publish.

### Debugging
To attach a remote debugging to the Jenkins instance started by Gradle set the GRADLE_OPTS environment variable.

For example:

    export GRADLE_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
    ./gradlew server


----------
