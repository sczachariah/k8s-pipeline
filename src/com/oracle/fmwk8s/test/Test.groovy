package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.EnvironmentSetup
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.env.Domain

class Test extends Common {
    static def testId
    static def testPodName
    static def testStatus = "init"

    static def logDirectory

    static invokeTest() {
        logDirectory = "/logs/${runId}"

        if (testType != null && !testType.toString().isEmpty()) {
            if (testType.matches("url-validation")) {
                Log.info("invoking ${testType} tests.")
                UrlValidation.fireTest()
            } else if (testType.matches("operator-integration-.*")) {
                Log.info("invoking ${testType} tests.")
                OperatorIntegration.fireTest()
            } else if (testType.matches("mats.*")) {
                Log.info("invoking ${testType} tests.")
                Mats.fireTest()
            } else {
                Log.info("no tests to run.")
            }
        }
    }

    static cleanup() {
        if (testStatus.equalsIgnoreCase("completed") && !EnvironmentSetup.isWaiting) {
            try {
                Log.info("begin cleanup test resources.")

                script.sh label: "cleanup test pod",
                        script: "kubectl delete -f kubernetes/framework/test/${testId}/fmwk8s-${testId}-test-pod.yaml -n ${Domain.domainNamespace}"
                sleep 30
                script.sh label: "cleanup test pv/pvc",
                        script: "kubectl delete -f kubernetes/framework/test/${testId}/fmwk8s-${testId}-pvc.yaml -n ${Domain.domainNamespace} && \
                                sleep 30 && \
                                kubectl delete -f kubernetes/framework/test/${testId}/fmwk8s-${testId}-pv.yaml -n ${Domain.domainNamespace}"

                Log.info("cleanup test resources success.")
            }
            catch (exc) {
                Log.error("cleanup test resources failed.")
                exc.printStackTrace()
            }
        }
    }

}
