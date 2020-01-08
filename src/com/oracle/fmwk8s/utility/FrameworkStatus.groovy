package com.oracle.fmwk8s.utility

import com.oracle.fmwk8s.common.Base
import com.oracle.fmwk8s.env.Logging
import com.oracle.fmwk8s.test.Test
import groovy.json.JsonOutput

class FrameworkStatus {
    String buildName
    String runId
    String jobStatus
    Map<String, String> parameters
    String jobLink
    String jobUrl
    String testStatus
    Map<String, String> testSummary
    String logLocation

    FrameworkStatus(script) {
        this.buildName = "${script.currentBuild.displayName}"
        this.runId = Base.runId
        this.jobStatus = "${script.currentBuild.currentResult}"

        parameters = new LinkedHashMap<>()
        parameters.put("cloud", Base.cloud)
        parameters.put("operatorVersion", Base.operatorVersion)
        parameters.put("productName", Base.productName)
        parameters.put("domainType", Base.domainType)
        parameters.put("productImage", Base.productImage)
        parameters.put("databaseVersion", Base.cloud)
        parameters.put("loadBalancer", Base.cloud)
        parameters.put("testImage", Base.cloud)
        parameters.put("testType", Base.cloud)
        parameters.put("testTarget", Base.cloud)

        this.jobLink = "${script.env.JENKINS_URL}/blue/organizations/jenkins/${script.env.JOB_NAME}/detail/${script.env.JOB_NAME}/${script.env.BUILD_NUMBER}/pipeline"
        this.jobUrl = "${script.env.JOB_URL}"
        this.testStatus = Test.testStatus

        testSummary = new LinkedHashMap<>()
        ReportUtility.overallExecutedTestCaseList.split().each {
            testSummary.put("${it}".toString(), "${it}".contains(".suc") ? "PASS" : ("${it}".contains(".dif") ? "FAIL" : "SKIP"))
        }

        this.logLocation = Logging.artifactoryVirtualRepo + Logging.fmwk8sArtifactoryLogLocation
    }

    static getFrameworkStatusJson(script) {
        FrameworkStatus frameworkStatus = new FrameworkStatus(script);
        def json = JsonOutput.toJson(frameworkStatus)
        def prettyJson = JsonOutput.prettyPrint(json)

        script.sh label: "print framework status json",
                script: "echo \"${prettyJson}\""
    }
}


