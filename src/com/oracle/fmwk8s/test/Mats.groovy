package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.env.Database
import com.oracle.fmwk8s.env.Domain
import com.oracle.fmwk8s.env.Logging
import com.oracle.fmwk8s.utility.ReportUtility

class Mats extends Test {
    static fireTest() {
        try {
            Log.info("begin ${Common.productId} product fireTest.")
            testId = Common.productId
            createEnvConfigMap()
            runTests()
            Log.info("${Common.productId} product fireTest success.")
        }
        catch (exc) {
            Log.error("${Common.productId} product fireTest failed.")
            testStatus = "failed"
        }
        finally {
            /** Trying to collect all the test logs under the test_logs directory after successful test runs */
            Logging.getTestLogs()
        }
    }

    static createEnvConfigMap() {
        try {
            Log.info("begin create env configmap.")

            script.git branch: 'master',
                    credentialsId: 'fmwk8sval_ww.ssh',
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh "cd kubernetes/framework/test/${Common.productId} && \
                        sed -i \"s|%ADMIN_SERVER_NAME_SVC%|${Domain.domainName}-adminserver.${Domain.domainNamespace}|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%MANAGED_SERVER_NAME_SVC%|${Domain.domainName}-cluster-${Common.productId}-cluster.${Domain.domainNamespace}|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%ADMIN_USER%|${Domain.weblogicUsername}|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%ADMIN_PASSWORD%|${Domain.weblogicPassword}|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%ADMIN_PORT%|7001|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%ADMIN_SERVER_NAME%|${yamlUtility.domainInputsMap.get("adminServerName")}|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%ADMIN_SSL_PORT%||g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DOMAIN_HOME%|${Domain.nfsDomainPath}|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DOMAIN_NAME%|${Domain.domainName}|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DOMAIN_NAMESPACE%|${Domain.domainNamespace}|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%CONNECTION_STRING%|${Database.dbName}.${Domain.domainNamespace}:${Database.dbPort}/${Database.dbName}pdb.us.oracle.com|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_HOST%|${Database.dbName}.${Domain.domainNamespace}|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_PORT%|${Database.dbPort}|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_SCHEMA_PASSWORD%|Welcome1|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_SID%|${Database.dbName}.us.oracle.com|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%JDBC_URL%|jdbc:oracle:thin:@${Database.dbName}.${Domain.domainNamespace}:${Database.dbPort}/${Database.dbName}pdb.us.oracle.com|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%RCUPREFIX%|${domainName}|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%MANAGED_SERVER_NAME_BASE%|${Common.productId}_server|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%MANAGED_SERVER_PORT%|8001|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        cat fmwk8s-${Common.productId}-env-configmap.yaml"

            script.sh "kubectl apply -f kubernetes/framework/test/${Common.productId}/fmwk8s-${Common.productId}-env-configmap.yaml -n ${Domain.domainNamespace}"

            Log.info("create env configmap success.")
        }
        catch (exc) {
            Log.error("create env configmap failed.")
        }
        finally {
        }
    }

    static runTests() {
        try {
            Log.info("begin run test.")

            script.git branch: 'master',
                    credentialsId: 'fmwk8sval_ww.ssh',
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh label: "configure test pod",
                    script: "cd kubernetes/framework/test/${Common.productId} && \
                        sed -i \"s|%TEST_IMAGE%|${testImage}|g\" fmwk8s-${Common.productId}-mats-pod.yaml && \
                        sed -i \"s|%HOURS_AFTER_SECONDS%|${hoursAfterSeconds}|g\" fmwk8s-${Common.productId}-mats-pod.yaml && \
                        sed -i \"s|%LOG_DIRECTORY%|${logDirectory}|g\" fmwk8s-${Common.productId}-mats-pod.yaml && \
                        sed -i \"s|%RUN_ID%|${Common.runId}|g\" fmwk8s-${Common.productId}-mats-pod.yaml && \
                        sed -i \"s|%FMWK8S_NFS_DOMAIN_HOME%|/scratch/u01/DockerVolume/domains|g\" fmwk8s-${Common.productId}-mats-pod.yaml && \
                        sed -i \"s|%DOMAIN_PVC%|${domainName}-${domainNamespace}-pvc|g\" fmwk8s-${Common.productId}-mats-pod.yaml && \
                        cat fmwk8s-${Common.productId}-mats-pod.yaml"

            script.sh "kubectl apply -f kubernetes/framework/test/${Common.productId}/fmwk8s-${Common.productId}-mats-pod.yaml -n ${Domain.domainNamespace} && \
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
