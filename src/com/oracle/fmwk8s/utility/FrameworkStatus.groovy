package com.oracle.fmwk8s.utility

import com.oracle.fmwk8s.common.Base
import groovy.json.JsonOutput

class FrameworkStatus {
    String runId
    String cloud
    Map<String, String> parameters

    FrameworkStatus() {
        this.runId = Base.runId
        this.cloud = Base.cloud

        parameters = new HashMap<>()
        parameters.put("operatorVersion", Base.operatorVersion)
        parameters.put("productName", Base.productName)
        parameters.put("domainType", Base.domainType)
        parameters.put("productImage", Base.productImage)
    }

    static getFrameworkStatusJson(script) {
        FrameworkStatus frameworkStatus = new FrameworkStatus();
        def json = JsonOutput.toJson(frameworkStatus)
        def prettyJson = JsonOutput.prettyPrint(json)

        script.sh label: "print framework status json",
                script: "echo \"${prettyJson}\""
    }
}


