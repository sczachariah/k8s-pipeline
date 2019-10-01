package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.EnvironmentSetup
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.env.Domain

class Test {
    static def testId
    static def testStatus = "init"

    static cleanup(script) {
        if (testStatus.equalsIgnoreCase("completed") && !EnvironmentSetup.isWaiting) {
            try {
                Log.info(script, "begin cleanup test resources.")

                script.sh "kubectl delete -f kubernetes/framework/test/${testId}/fmwk8s-${testId}-test-pod.yaml -n ${Domain.domainNamespace}"
                sleep 30
                script.sh "kubectl delete -f kubernetes/framework/test/${testId}/fmwk8s-${testId}-pvc.yaml -n ${Domain.domainNamespace}"
                sleep 30
                script.sh "kubectl delete -f kubernetes/framework/test/${testId}/fmwk8s-${testId}-pv.yaml -n ${Domain.domainNamespace}"

                Log.info(script, "cleanup test resources success.")
            }
            catch (exc) {
                Log.error(script, "cleanup test resources failed.")
                exc.printStackTrace()
            }
        }
    }
}
