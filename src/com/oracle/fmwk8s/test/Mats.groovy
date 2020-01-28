package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Base
import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.env.Database
import com.oracle.fmwk8s.env.Domain
import com.oracle.fmwk8s.env.Logging

class Mats extends Test {
    static fireTest() {
        try {
            Log.info("begin ${Common.productId} product fireTest.")
            testId = "mats"
            runTests()
            Log.info("${Common.productId} product fireTest success.")
        }
        catch (exc) {
            Log.error("${Common.productId} product fireTest failed.")
            testStatus = "failed"
        }
    }

    static runTests() {
        try {
            Log.info("begin run test.")

            script.git branch: 'master',
                    credentialsId: "${sshCredentialId}",
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh label: "configure test pod",
                    script: "cd kubernetes/framework/test/${testId} && \
                        sed -i \"s|%PRODUCT_ID%|${productId}|g\" fmwk8s-${testId}-test-pod.yaml && \
                        sed -i \"s|%TEST_IMAGE%|${testImage}|g\" fmwk8s-${testId}-test-pod.yaml && \
                        sed -i \"s|%HOURS_AFTER_SECONDS%|${hoursAfterSeconds}|g\" fmwk8s-${testId}-test-pod.yaml && \
                        sed -i \"s|%LOG_DIRECTORY%|${logDirectory}|g\" fmwk8s-${testId}-test-pod.yaml && \
                        sed -i \"s|%RUN_ID%|${Common.runId}|g\" fmwk8s-${testId}-test-pod.yaml && \
                        sed -i \"s|%FMWK8S_NFS_HOME%|${fmwk8sNfsHome}|g\" fmwk8s-${testId}-test-pod.yaml && \
                        sed -i \"s|%DOMAIN_PVC%|${domainName}-${domainNamespace}-pvc|g\" fmwk8s-${testId}-test-pod.yaml && \
                        cat fmwk8s-${testId}-test-pod.yaml"

            script.sh "kubectl apply -f kubernetes/framework/test/${testId}/fmwk8s-${testId}-test-pod.yaml -n ${Domain.domainNamespace} && \
                       kubectl get all -n ${Domain.domainNamespace}"

            testStatus = "started"
            Base.testStartTime = Base.getCurrentDateTime()
            Test.waitForTests()

            Log.info("run test success.")
        }
        catch (exc) {
            Log.error("run test failed.")
            throw exc
        }
        finally {
            Base.testEndTime = Base.getCurrentDateTime()
            testPodName = script.sh(
                    label: "get test pod name",
                    script: "kubectl get pods -o go-template --template \'{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}\' -n ${Domain.domainNamespace} | grep ${testId}-test",
                    returnStdout: true
            ).trim()
            Log.info("begin fetch test pod logs.")
            Logging.getPodLogs(testPodName, Domain.domainNamespace)
            Log.info("fetch test pod logs success.")
        }
    }
}
