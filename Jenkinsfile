#!/usr/bin/env groovy

/*
 * Bootstrap jenkinsfile for the Execution Manager Jenkins Plugin
 *
 * How to use:
 *
 *    1. Create Pipeline project
 *    2. Pipeline configuration:
 *       a. Definition = Pipeline script from SCM
 *       b. SCM = Git
 *       c. Repository URL = http://wsengtfs01:8080/tfs/DefaultCollection/Worksoft/_git/worksoft-em-plugin
 *       d. Repository credentials = <valid credentials>
 *       e. Branch = <branch containing this file> (e.g. develop, master, etc.)
 *       f. Script path = Jenkinsfile (case sensitive)
 *    3. Save the project
 *    4. Build until the following script signatures have been approved:
 *       method hudson.plugins.git.GitSCM getBranches
 *       method hudson.plugins.git.GitSCM getUserRemoteConfigs
 *       method hudson.plugins.git.GitSCMBackwardCompatibility getExtensions
 *    5. Use normally
 *
 */

// We don't support multi-branch builds, so build only the first branch specified in the UI
def branch = scm.branches[0]
branch = "${branch}".replaceAll("/", "#")
def buildType = ""
if (branch == 'master') {
    buildType = ""
} else if (branch == 'develop') {
    buildType = "beta"
} else {
    buildType = "SNAPSHOT"
}

// Adjust the following for the desired build node and where to clone the repo
def workspacePath = "D:\\hudson\\Builds\\JenkinsEMPlugin\\${branch}"
def buildNode = 'master'

def params_shouldBuild = true

pipeline {
    agent {
        node {
            label "${buildNode}"
            customWorkspace workspacePath
        }
    }

    /*triggers {
        pollSCM('H/15 * * * *')
    }*/

    parameters {
        string(name: 'ArtifactBaseDir', defaultValue: "\\\\wsengfiles01\\Automated_Builds\\JenkinsEMPlugin", description: 'base location where to put build artifacts')
        booleanParam(name: 'shouldBuild', description: 'DEBUG USE ONLY: execute build/compile', defaultValue: true)
        booleanParam(name: 'deployVM', description: 'DEBUG USE ONLY: Create a new VM from template and install/update certify and interfaces', defaultValue: true)
        booleanParam(name: 'executeTests', description: 'DEBUG USE ONLY: execute tests (certify processes, unit, regression)', defaultValue: true)
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
        timeout(time: 5, unit: 'MINUTES')
    }


    stages {
        script {
            if (params.containsKey("params.shouldBuild")) {
                params_shouldBuild = params.shouldBuild
            }
        }

        stage('Checkout') {
            steps {
                echo "parameters = $params"
                cleanWs cleanWhenFailure: false

                checkout([$class                           : 'GitSCM',
                          branches                         : scm.branches,
                          doGenerateSubmoduleConfigurations: false,
                          extensions                       : [],
                          submoduleCfg                     : [],
                          userRemoteConfigs                : scm.userRemoteConfigs
                ])
            }
        }

        stage('Pre-Build') {
            steps {
                echo "powershell -noprofile -command \".\\versioner.ps1 -b ${BUILD_NUMBER} -t ${buildType}\" > version.txt"
                bat returnStatus: true, script: "powershell -noprofile -command \".\\versioner.ps1 -b ${BUILD_NUMBER} -t ${buildType}\" > version.txt"
                script {
                    currentBuild.displayName = readFile 'version.txt'
                }
            }
        }

        stage('Build') {
            when {
                equals expected: true, actual: params.shouldBuild
            }
            steps {
                bat returnStatus: true, script: "buildit.cmd"
                bat script: "deliverit.cmd \"${ArtifactBaseDir}\\${branch}\\${currentBuild.displayName}\""
            }
        }

        stage('Deploy for Test') {
            when {
                equals expected: true, actual: params.deployVM
            }
            steps {
                echo "------------------------------- Deploying for Tests --------------------------------------"
            }
        }

        stage('Execute Tests') {
            when {
                equals expected: true, actual: params.executeTests
            }
            steps {

                echo "---------------------------------- Exec Tests -------------------------------------------"
            }
        }
        stage('Post Results') {
            steps {
                bat script: "echo post"
            }

        }

    }
    post {
        failure {
            echo "--- Build FAILURE"
        }

        always {
            echo "Pipeline result: ${currentBuild.result}"
            echo "Pipeline currentResult: ${currentBuild.currentResult}"
            //slackSend channel: '#java-oracle-sap-rqm', message: "Postman build ${currentBuild.fullDisplayName}  status:  ${currentBuild.currentResult}"
        }
        changed {
            echo "--- Build CHANGED "
        }
        aborted {
            echo "--- Build ABORTED"
        }
        success {
            echo "--- Build SUCCESS"
        }
        unstable {
            echo "--- Build UNSTABLE"
        }
    }
}