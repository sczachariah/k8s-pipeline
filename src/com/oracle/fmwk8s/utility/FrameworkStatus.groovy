package com.oracle.fmwk8s.utility

import com.oracle.fmwk8s.common.Base
import com.oracle.fmwk8s.env.Logging
import com.oracle.fmwk8s.test.Test
import groovy.json.JsonOutput

class FrameworkStatus {
    String buildName
    String runId
    String productName
    String releaseVersion
    String jobStatus
    String pipelineStartTime
    String pipelineEndTime
    String testStartTime
    String testEndTime
    Map<String, String> parameters
    String jobLink
    String jobUrl
    String testStatus
    Map<String, String> testSummary
    String logLocation

    FrameworkStatus(script) {
        this.buildName = "${script.currentBuild.displayName}"
        this.runId = Base.runId
        this.productName = Base.productName
        this.releaseVersion = Base.fmwProductReleaseVersion
        this.jobStatus = "${script.currentBuild.currentResult}"
        this.pipelineStartTime = Base.pipelineStartTime
        this.pipelineEndTime = Base.pipelineEndTime
        this.testStartTime = Base.testStartTime
        this.testEndTime = Base.testEndTime

        parameters = new LinkedHashMap<>()
        parameters.put("cloud", Base.cloud)
        parameters.put("operatorVersion", Base.operatorVersion)
        parameters.put("productName", Base.productName)
        parameters.put("domainType", Base.domainType)
        parameters.put("productImage", Base.productImage)
        parameters.put("databaseVersion", Base.databaseVersion)
        parameters.put("loadBalancer", Base.lbType)
        parameters.put("testImage", Base.testImage)
        parameters.put("testType", Base.testType)
        parameters.put("testTarget", Base.testTarget)
        parameters.put("hoursAfter", Base.hoursAfter)

        this.jobLink = "${script.env.JENKINS_URL}/blue/organizations/jenkins/${script.env.JOB_NAME}/detail/${script.env.JOB_NAME}/${script.env.BUILD_NUMBER}/pipeline"
        this.jobUrl = "${script.env.JOB_URL}"
        this.testStatus = Test.testStatus.equalsIgnoreCase("completed") ? (ReportUtility.difCountValue > 0 ? "FAIL" : "PASS") : Test.testStatus.toUpperCase()

        testSummary = new LinkedHashMap<>()
        ReportUtility.overallExecutedTestCaseList.split().each {
            testSummary.put("${it}".toString().substring(0,"${it}".toString().lastIndexOf(".")), "${it}".contains(".suc") ? "PASS" : ("${it}".contains(".dif") ? "FAIL" : "SKIP"))
        }

        this.logLocation = Logging.artifactoryServer + Logging.fmwk8sArtifactoryLogLocation
    }

    static getFrameworkStatusJson(script) {
        Base.pipelineEndTime = Base.getCurrentDateTime()
        FrameworkStatus frameworkStatus = new FrameworkStatus(script);
        def json = JsonOutput.toJson(frameworkStatus)
        def prettyJson = JsonOutput.prettyPrint(json)

        script.sh label: "print framework status json",
                script: "echo \"${prettyJson}\""
    }
}


