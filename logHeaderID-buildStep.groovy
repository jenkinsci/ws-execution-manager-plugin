/*
 * Copyright (c)  Worksoft, Inc.
 *
 * logHeaderID-buildStep
 *
 */

// Requires Groovy plugin - http://wiki.jenkins-ci.org/display/JENKINS/Groovy+plugin

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def build = Thread.currentThread().executable
def resolver = build.buildVariableResolver
def workspace = build.getModuleRoot().absolutize().toString()

def API_TOKEN = resolver.resolve("API_TOKEN")
def API_URL = resolver.resolve("API_URL")

def execManResult = new FilePath(build.getModuleRoot().getChannel(), workspace + "/execMan-result.json")
//def execManResult = new File(workspace + "/execMan-result.json")
if (execManResult.exists()) {
    def result = new JsonSlurper().parse(execManResult.read())
    //def result = new JsonSlurper().parse(execManResult)
    println "result=" + JsonOutput.prettyPrint(JsonOutput.toJson(result))

    // Loop through the tasks looking for result IDs
    for(int i=0; i < result['Tasks'].size(); i++) {
        def logHeaderID = result['Tasks'][i]['CertifyResultID']
        def executionStatus = result['Tasks'][i]['ExecutionStatus']
        if (logHeaderID != null && executionStatus.toUpperCase().equals("FAILED")) {
            def url = API_URL + "?logHeaderID=" + logHeaderID
            def api = url.toURL().openConnection()
            api.addRequestProperty("Accept", "application/json")
            api.addRequestProperty("token", API_TOKEN)
            api.setRequestMethod("GET")
            api.connect()

            println "Tasks[" + i +"] logHeaderID=" + logHeaderID + ":" +  JsonOutput.prettyPrint(api.content.text)
        }
    } 
}

