package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.EnvironmentSetup
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.env.Domain
import com.oracle.fmwk8s.env.Logging
import com.oracle.fmwk8s.env.Operator
import com.oracle.fmwk8s.utility.ReportUtility

class OperatorIntegration extends Test {
    static fireTest() {
        try {
            Log.info("begin fireTest.")
            testId = "op-intg"
            createEnvConfigMap()
            runTests()
            Log.info("fireTest success.")
        }
        catch (exc) {
            Log.error("fireTest failed.")
            testStatus = "failed"
        }
        finally {
            Test.publishTestLogsAndDoNecessaryCleanup()
        }
    }

    static createEnvConfigMap() {
        try {
            Log.info("begin create env configmap.")

            script.git branch: 'master',
                    credentialsId: 'fmwk8sval_ww.ssh',
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh label: "configure env variables configmap",
                    script: "cd kubernetes/framework/test/${testId} && \
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
                        sed -i \"s|%HOURS_AFTER_SECONDS%|${hoursAfterSeconds}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        cat fmwk8s-${testId}-env-configmap.yaml"

            script.sh label: "create env varialbes configmap",
                    script: "kubectl apply -f kubernetes/framework/test/${testId}/fmwk8s-${testId}-env-configmap.yaml -n ${Domain.domainNamespace}"

            Log.info("create env configmap success.")
        }
        catch (exc) {
            Log.error("create env configmap failed.")
            throw exc
        }
        finally {
        }
    }

    static runTests() {
        try {
            Log.info("begin run test.")

            script.sh label: "configure test pod",
                    script: "cd kubernetes/framework/test/${testId} && \
                        sed -i \"s|%TEST_IMAGE%|${testImage}|g\" fmwk8s-${testId}-test-pod.yaml && \
                        sed -i \"s|%HOURS_AFTER_SECONDS%|${hoursAfterSeconds}|g\" fmwk8s-${testId}-test-pod.yaml && \
                        sed -i \"s|%LOG_DIRECTORY%|${logDirectory}|g\" fmwk8s-${testId}-test-pod.yaml && \
                        sed -i \"s|%RUN_ID%|${Common.runId}|g\" fmwk8s-${testId}-test-pod.yaml && \
                        cat fmwk8s-${testId}-test-pod.yaml"

            script.sh label: "create test pod",
                    script: "kubectl apply -f kubernetes/framework/test/${testId}/fmwk8s-${testId}-test-pod.yaml -n ${Domain.domainNamespace} && \
                       kubectl get all -n ${Domain.domainNamespace}"

            testStatus = "started"
            Test.waitForTests()

            Log.info("run test success.")
        }
        catch (exc) {
            Log.error("run test failed.")
            throw exc
        }
        finally {
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
