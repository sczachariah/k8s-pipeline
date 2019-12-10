package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.env.Database
import com.oracle.fmwk8s.env.Domain
import com.oracle.fmwk8s.env.Logging
import com.oracle.fmwk8s.env.Operator

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
                        sed -i \"s|%PRODUCT_ID%|${productId}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%PRODUCT_IMAGE%|${productImage}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%OPERATOR_NS%|${Operator.operatorNamespace}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%OPERATOR_SA%|${Operator.operatorServiceAccount}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%OPERATOR_HELM_RELEASE%|${Operator.operatorHelmRelease}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DOMAIN_NAME%|${Domain.domainName}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DOMAIN_NS%|${Domain.domainNamespace}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%FMWK8S_NFS_HOME%|${fmwk8sNfsHome}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DOMAIN_HOME%|${Domain.nfsDomainPath}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DOMAIN_TYPE%|${Domain.domainType}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%ADMIN_SERVER_NAME%|${yamlUtility.domainInputsMap.get("adminServerName")}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%ADMIN_PORT%|${yamlUtility.domainInputsMap.get("adminPort")}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%MANAGED_SERVER_NAME_BASE%|${yamlUtility.domainInputsMap.get("managedServerNameBase")}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%WEBLOGIC_CREDENTIALS_SECRET_NAME%|${Domain.weblogicCredentialsSecretName}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%ADMIN_T3_CHANNEL_PORT%|${yamlUtility.domainInputsMap.get("t3ChannelPort")}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%TEST_TYPE%|${testType}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%LOG_DIRECTORY%|${logDirectory}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%HOURS_AFTER_SECONDS%|${hoursAfterSeconds}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%CONNECTION_STRING%|${Database.dbName}.${Domain.domainNamespace}:${Database.dbPort}/${Database.dbName}pdb.us.oracle.com|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DB_HOST%|${Database.dbName}.${Domain.domainNamespace}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DB_PORT%|${Database.dbPort}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DB_SCHEMA_PASSWORD%|Welcome1|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DB_SERVICE%|${Database.dbName}pdb.us.oracle.com|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DB_SID%|${Database.dbName}.us.oracle.com|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DB_IMAGE%|container-registry.oracle.com/database/${databaseVersion}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DB_SECRET%|${registrySecret}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%JDBC_URL%|jdbc:oracle:thin:@${Database.dbName}.${Domain.domainNamespace}:${Database.dbPort}/${Database.dbName}pdb.us.oracle.com|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%RCUPREFIX%|${domainName}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%TEST_IMAGE%|${Common.testImage}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%OPERATOR_VERSION%|${Common.operatorVersion}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%OPERATOR_BRANCH%|${Common.operatorBranch}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%OPERATOR_IMAGE_VERSION%|${Common.operatorImageVersion}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%SAMPLES_REPO%|${Common.samplesRepo}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%SAMPLES_DIRECTORY%|${Common.samplesDirectory}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%SAMPLES_BRANCH%|${Common.samplesBranch}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        cat fmwk8s-${testId}-env-configmap.yaml"

            script.sh label: "create env variables configmap",
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
