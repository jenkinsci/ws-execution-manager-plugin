Worksoft Execution Manager Jenkins Plugin
========================

### Introduction
TODO: Give a short introduction of your project. Let this section explain the objectives or the motivation behind this project. 

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

If you want to learn more about creating good readme files then refer the following [guidelines](https://www.visualstudio.com/en-us/docs/git/create-a-readme). You can also seek inspiration from the below readme files:
- [ASP.NET Core](https://github.com/aspnet/Home)
- [Visual Studio Code](https://github.com/Microsoft/vscode)
- [Chakra Core](https://github.com/Microsoft/ChakraCore)