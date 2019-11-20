package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.env.Domain

class Test extends Common {
    static def testId
    static def testPodName
    static def testStatus = "init"

    static def logDirectory

    static invokeTest() {
        logDirectory = "/logs/${runId}"
        doTestHarnessSetup()

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

        if (testStatus.equalsIgnoreCase("failure")) {
            throw new Exception("test has failed.")
        }
    }

    static doTestHarnessSetup() {
        try {
            Log.info("begin test harness setup.")

            script.git branch: 'master',
                    credentialsId: 'fmwk8sval_ww.ssh',
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh label: "create fmwk8s utility configmap",
                    script: "kubectl apply -f kubernetes/framework/fmwk8s-utility-configmap.yaml -n ${Domain.domainNamespace}"

            script.sh label: "configure test pv/pvc",
                    script: "cd kubernetes/framework/test && \
                       sed -i \"s|%RUN_ID%|${Common.runId}|g\" fmwk8s-tests-pv.yaml && \
                       sed -i \"s|%RUN_ID%|${Common.runId}|g\" fmwk8s-tests-pvc.yaml && \
                       cat fmwk8s-tests-pv.yaml && \
                       cat fmwk8s-tests-pvc.yaml"

            script.sh label: "create test pv/pvc",
                    script: "kubectl apply -f kubernetes/framework/test/fmwk8s-tests-pv.yaml -n ${Domain.domainNamespace} && \
                       kubectl apply -f kubernetes/framework/test/fmwk8s-tests-pvc.yaml -n ${Domain.domainNamespace}"

            Log.info("test harness setup success.")
        }
        catch (exc) {
            Log.error("test harness setup failed.")
            throw exc
        }
        finally {
        }
    }

    static cleanup() {
        try {
            Log.info("begin cleanup test resources.")

            script.sh label: "cleanup test pod",
                    script: "kubectl delete -f kubernetes/framework/test/${testId}/fmwk8s-${testId}-test-pod.yaml -n ${Domain.domainNamespace} --grace-period=0 --force --cascade"
            sleep 30
            script.sh label: "cleanup test pv/pvc",
                    script: "kubectl delete fmwk8s-tests-pvc-${Common.runId} -n ${Domain.domainNamespace} --grace-period=0 --force --cascade && \
                             sleep 30 && \
                             kubectl delete fmwk8s-tests-pv-${Common.runId} -n ${Domain.domainNamespace} --grace-period=0 --force --cascade"

            Log.info("cleanup test resources success.")
        }
        catch (exc) {
            Log.error("cleanup test resources failed.")
            exc.printStackTrace()
        }
    }
}
