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
            testStatus = "failure"
        }
        finally {
            publishLogsAndGenerateTestSummaryReport()
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
                        sed -i \"s|%SOA_HOST_SVC%|${Domain.domainName}-${Common.productId}_server2.${Domain.domainNamespace}|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%CONNECTION_STRING%|${Database.dbName}.${Domain.domainNamespace}:${Database.dbPort}/${Database.dbName}pdb.us.oracle.com|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_HOST%|${Database.dbName}.${Domain.domainNamespace}|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_PORT%|${Database.dbPort}|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_SCHEMA_PASSWORD%|Welcome1|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_SID%|${Database.dbName}.us.oracle.com|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%JDBC_URL%|jdbc:oracle:thin:@${Database.dbName}.${Domain.domainNamespace}:${Database.dbPort}/${Database.dbName}pdb.us.oracle.com|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%RCUPREFIX%|${domainName}|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_IAU_USER%|${Domain.domainName}_IAU|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_IAU_PASSWORD%|Welcome1|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_MDS_USER%|${Domain.domainName}_MDS|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_MDS_PASSWORD%|Welcome1|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_OPSS_USER%|${Domain.domainName}_OPSS|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_OPSS_PASSWORD%|Welcome1|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_SDPM_USER%|${Domain.domainName}_UMS|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_SDPM_PASSWORD%|Welcome1|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_SOA_USER%|${Domain.domainName}_SOAINFRA|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_SOA_PASSWORD%|Welcome1|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_STB_USER%|${Domain.domainName}_STB|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
                        sed -i \"s|%DB_STB_PASSWORD%|Welcome1|g\" fmwk8s-${Common.productId}-env-configmap.yaml && \
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
            waitForTests()

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

    // re design to check for creation of file fmwk8s.completed
    static waitForTests() {
        try {
            Log.info("begin wait for test completion.")

            script.sh label: "check test pod status",
                    script: "testInit='testInit' && \
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

            script.sh label: "check test status",
                    script: "testStat='testStat' && \
                        i=0 && \
                        until `echo \"\$testStat\" | grep -q Completed` > /dev/null || `echo \"\$testStat\" | grep -q Error` > /dev/null\n \
                        do \n \
                            if [ \$i == 100 ]; then\n \
                                echo \"Timeout waiting for Test Completion. Exiting!!.\"\n \
                                break\n \
                            fi\n \
                        i=\$((i+1))\n \
                        echo \"Test is Running. Iteration \$i of 100. Sleeping\"\n \
                        sleep 120\n \
                        testStat=`echo \\`kubectl get pods -n ${Domain.domainNamespace} 2>&1 | grep fmwk8s-${testId}-test\\``\n \
                        done"

            def testContainerStatus = script.sh(
                    label: "get test status",
                    script: "kubectl get pods -n ${Domain.domainNamespace} 2>&1 | grep fmwk8s-${testId}-test",
                    returnStdout: true
            ).trim()
            if (testContainerStatus.toString().contains("Error")) {
                testStatus = "failure"
            } else if (testContainerStatus.toString().contains("Completed")) {
                testStatus = "completed"
            } else {
                testStatus = "completed"
            }
            //here i should check for that file and put my logic here
            Log.info("wait for test completion success.")
        }
        catch (exc) {
            Log.error("wait for test completion failed.")
            throw exc
        }
    }

    static publishLogsAndGenerateTestSummaryReport() {
        /** Trying to collect all the test logs under the test_logs directory after successful test runs */
//        Logging.getTestLogs()

        /** Logic to evaluate the count of *.suc, *.dif & *.skip files in the test_logs folder after test runs */
//        ReportUtility.countOfSucDifFilesAfterTestRunsAndGenerateTestSummaryReport(script)
    }
}
