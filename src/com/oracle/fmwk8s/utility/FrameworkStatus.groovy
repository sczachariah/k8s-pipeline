package com.oracle.fmwk8s.utility

import com.oracle.fmwk8s.common.Base
import groovy.json.JsonOutput

class FrameworkStatus {
    String runId
    String cloud
    String operatorVersion
    String productName
    String domainType
    String productImage

    FrameworkStatus() {
        this.runId = Base.runId
        this.cloud = Base.cloud
        this.operatorVersion = Base.operatorVersion
        this.productName = Base.productName
        this.domainType = Base.domainType
        this.productImage = Base.productImage
    }

    static getFrameworkStatusJson(script) {
        FrameworkStatus frameworkStatus = new FrameworkStatus();
        def json = JsonOutput.toJson(frameworkStatus)

        script.sh label: "print framework status json",
                script: "echo \"${json}\""
    }
}


