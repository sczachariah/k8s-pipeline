package com.oracle.fmwk8s.utility

import com.oracle.fmwk8s.common.Base
import groovy.json.JsonOutput

class FrameworkStatus {
    String runId
    String cloud
    Map<String, String> parameters
    String jobStatus

    FrameworkStatus(script) {
        this.runId = Base.runId
        this.cloud = Base.cloud

        parameters = new HashMap<>()
        parameters.put("operatorVersion", Base.operatorVersion)
        parameters.put("productName", Base.productName)
        parameters.put("domainType", Base.domainType)
        parameters.put("productImage", Base.productImage)

        this.jobStatus = "${script.currentBuild.currentResult}"
    }

    static getFrameworkStatusJson(script) {
        FrameworkStatus frameworkStatus = new FrameworkStatus(script);
        def json = JsonOutput.toJson(frameworkStatus)
        def prettyJson = JsonOutput.prettyPrint(json)

        script.sh label: "print framework status json",
                script: "echo \"${prettyJson}\""
    }
}


