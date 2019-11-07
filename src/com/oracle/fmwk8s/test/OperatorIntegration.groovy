package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.env.Domain
import com.oracle.fmwk8s.env.Logging
import com.oracle.fmwk8s.env.Operator

class OperatorIntegration extends Test {
    static fireTest() {
        testId = "op-intg"
        createEnvConfigMap()
        createPersistentVolume()
        runTests()
        publishLogs()
        cleanup()
    }

    static createEnvConfigMap() {
        try {
            Log.info("begin create env configmap.")

            script.git branch: 'master',
                    credentialsId: 'fmwk8sval_ww.ssh',
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh "cd kubernetes/framework/test/${testId} && \
                        sed -i \"s|%PRODUCT_NAME%|${Common.productName}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%OPERATOR_NS%|${Operator.operatorNamespace}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%OPERATOR_SA%|${Operator.operatorServiceAccount}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%OPERATOR_HELM_RELEASE%|${Operator.operatorHelmRelease}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DOMAIN_NAME%|${Domain.domainName}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DOMAIN_NS%|${Domain.domainNamespace}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%ADMIN_SERVER_NAME%|${yamlUtility.domainInputsMap.get("adminServerName")}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%ADMIN_PORT%|${yamlUtility.domainInputsMap.get("adminPort")}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%MANAGED_SERVER_NAME_BASE%|${yamlUtility.domainInputsMap.get("managedServerNameBase")}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%WEBLOGIC_CREDENTIALS_SECRET_NAME%|${Domain.weblogicCredentialsSecretName}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%ADMIN_T3_CHANNEL_PORT%|${yamlUtility.domainInputsMap.get("t3ChannelPort")}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%TEST_TYPE%|${testType}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%LOG_DIRECTORY%|${logDirectory}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        cat fmwk8s-${testId}-env-configmap.yaml"

            script.sh "kubectl apply -f kubernetes/framework/test/${testId}/fmwk8s-${testId}-env-configmap.yaml -n ${Domain.domainNamespace}"

            Log.info("create env configmap success.")
        }
        catch (exc) {
            Log.error("create env configmap failed.")
            throw exc
        }
        finally {
        }
    }

    static createPersistentVolume() {
        try {
            Log.info("begin create persistent volume.")

            script.sh "cd kubernetes/framework/test/${testId} && \
                       sed -i \"s|%RUN_ID%|${Common.runId}|g\" fmwk8s-${testId}-pv.yaml && \
                       sed -i \"s|%RUN_ID%|${Common.runId}|g\" fmwk8s-${testId}-pvc.yaml && \
                       cat fmwk8s-${testId}-pv.yaml && \
                       cat fmwk8s-${testId}-pvc.yaml"

            script.sh "kubectl apply -f kubernetes/framework/test/${testId}/fmwk8s-${testId}-pv.yaml -n ${Domain.domainNamespace}"
            script.sh "kubectl apply -f kubernetes/framework/test/${testId}/fmwk8s-${testId}-pvc.yaml -n ${Domain.domainNamespace}"

            Log.info("create persistent volume success.")
        }
        catch (exc) {
            Log.error("create persistent volume failed.")
            throw exc
        }
        finally {
        }
    }

    static runTests() {
        try {
            Log.info("begin run test.")

            script.sh "cd kubernetes/framework/test/${testId} && \
                        sed -i \"s|%TEST_IMAGE%|${testImage}|g\" fmwk8s-${testId}-test-pod.yaml && \
                        sed -i \"s|%HOURS_AFTER_SECONDS%|144000|g\" fmwk8s-${testId}-test-pod.yaml && \
                        sed -i \"s|%LOG_DIRECTORY%|${logDirectory}|g\" fmwk8s-${testId}-test-pod.yaml && \
                        sed -i \"s|%RUN_ID%|${Common.runId}|g\" fmwk8s-${testId}-test-pod.yaml && \
                        cat fmwk8s-${testId}-test-pod.yaml"

            script.sh "kubectl apply -f kubernetes/framework/test/${testId}/fmwk8s-${testId}-test-pod.yaml -n ${Domain.domainNamespace} && \
                       kubectl get all -n ${Domain.domainNamespace}"

            testStatus = "started"
            waitForTests()

            Log.info("run test success.")
        }
        catch (exc) {
            Log.error("run test failed.")
            throw exc
        }
        finally {
            testPodName = script.sh(
                    script: "kubectl get pods -o go-template --template \'{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}\' -n ${Domain.domainNamespace} | grep ${testId}-test",
                    returnStdout: true
            ).trim()
            Log.info("begin fetch test pod logs.")
            Logging.getPodLogs(testPodName, Domain.domainNamespace)
            Log.info("fetch test pod logs success.")
        }
    }

    static waitForTests() {
        try {
            Log.info("begin wait for test completion.")

            script.sh "testInit='testInit' && \
                        i=0 && \
                        until `echo \$testInit | grep -q 1/1` > /dev/null\n \
                        do \n \
                            if [ \$i == 10 ]; then\n \
                                echo \"Timeout waiting for Test Initialization. Exiting!!.\"\n \
                                exit 1\n \
                            fi\n \
                        i=\$((i+1))\n \
                        echo \"Waiting for Test Initialization. Iteration \$i of 10. Sleeping\"\n \
                        sleep 60\n \
                        testInit=`echo \\`kubectl get pods -n ${Domain.domainNamespace} 2>&1 | grep fmwk8s-${testId}-test\\``\n \
                        done"

            script.sh "testStat='testStat' && \
                        i=0 && \
                        until `echo \$testStat | grep -q Completed` > /dev/null\n \
                        do \n \
                            if [ \$i == 50 ]; then\n \
                                echo \"Timeout waiting for Test Completion. Exiting!!.\"\n \
                                exit 1\n \
                            fi\n \
                        i=\$((i+1))\n \
                        echo \"Test is Running. Iteration \$i of 50. Sleeping\"\n \
                        sleep 120\n \
                        testStat=`echo \\`kubectl get pods -n ${Domain.domainNamespace} 2>&1 | grep fmwk8s-${testId}-test\\``\n \
                        done"

            testStatus = "completed"
            Log.info("wait for test completion success.")
        }
        catch (exc) {
            Log.error("wait for test completion failed.")
            throw exc
        }
    }

    static publishLogs() {
        Logging.getTestLogs()
    }
}
