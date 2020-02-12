package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.EnvironmentSetup
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.env.Database
import com.oracle.fmwk8s.env.Domain
import com.oracle.fmwk8s.env.Logging
import com.oracle.fmwk8s.utility.ReportUtility

class Test extends Common {
    static def testId
    static def testPodName
    static def testStatus = "nil"

    static def logDirectory

    static invokeTest() {
        logDirectory = "/logs/${runId}"

        if (testType != null && !testType.toString().isEmpty()) {
            if (!testType.matches("N/A")) {
                testStatus = "init"
                doTestHarnessSetup()
                createEnvConfigMap()
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
                    Log.info("no matching testType found.")
                }
            } else {
                Log.info("no tests to run.")
            }
        }

        if (testStatus.equalsIgnoreCase("failed")) {
            throw new Exception("test has failed.")
        }
    }

    static doTestHarnessSetup() {
        try {
            Log.info("begin test harness setup.")

            script.git branch: 'master',
                    credentialsId: "${sshCredentialId}",
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            /*script.sh label: "replace values in docker-entrypoint.sh",
                    script: "sed -i \"s|%LOG_DIRECTORY%|${logDirectory}|g\" docker-entrypoint.sh && \
                 sed -i \"s|%HOURS_AFTER_SECONDS%|${hoursAfterSeconds}|g\" docker-entrypoint.sh && \
                 cat docker-entrypoint.sh"

            script.sh label: "create a configmap with above given docker-entrypoint.sh script",
                    script: "kubectl create configmap fmwk8s-utility-configmap -n ${Domain.domainNamespace} --from-file=docker-entrypoint.sh && \
                kubectl get configmap -n ${Domain.domainNamespace}"

            script.sh "kubectl get configmap fmwk8s-utility-configmap -n ${Domain.domainNamespace} -o yaml"*/

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

    static createEnvConfigMap() {
        try {
            Log.info("begin create env configmap.")

            script.git branch: 'master',
                    credentialsId: "${sshCredentialId}",
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh label: "configure env variables configmap",
                    script: "cd kubernetes/framework/test/ && \
                        sed -i \"s|%PRODUCT_ID%|${productId}|g\" fmwk8s-tests-env-configmap.yaml && \
                        sed -i \"s|%PRODUCT_NAME%|${productName}|g\" fmwk8s-tests-env-configmap.yaml && \
                        sed -i \"s|%PRODUCT_IMAGE%|${productImage}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%OPERATOR_VERSION%|${operatorVersion}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%OPERATOR_IMAGE_VERSION%|${operatorImageVersion}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%OPERATOR_NS%|${operatorNamespace}|g\" fmwk8s-tests-env-configmap.yaml && \
                        sed -i \"s|%OPERATOR_SA%|${operatorServiceAccount}|g\" fmwk8s-tests-env-configmap.yaml && \
                        sed -i \"s|%OPERATOR_HELM_RELEASE%|${operatorHelmRelease}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%OPERATOR_BRANCH%|${operatorBranch}|g\" fmwk8s-tests-env-configmap.yaml && \
                        sed -i \"s|%DOMAIN_NAME%|${domainName}|g\" fmwk8s-tests-env-configmap.yaml && \
                        sed -i \"s|%DOMAIN_NS%|${domainNamespace}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%DOMAIN_HOME%|${fmwk8sNfsHome}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%DOMAIN_TYPE%|${domainType}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%ADMIN_SERVER_NAME_SVC%|${domainName}-${yamlUtility.domainInputsMap.get("adminServerName")}.${domainNamespace}.svc.cluster.local|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%ADMIN_SERVER_NAME%|${yamlUtility.domainInputsMap.get("adminServerName")}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%ADMIN_PORT%|${yamlUtility.domainInputsMap.get("adminPort")}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%ADMIN_T3_CHANNEL_PORT%|${yamlUtility.domainInputsMap.get("t3ChannelPort")}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%ADMIN_USER%|${weblogicUsername}|g\" fmwk8s-tests-env-configmap.yaml && \
                        sed -i \"s|%ADMIN_PASSWORD%|${weblogicPassword}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%WEBLOGIC_CREDENTIALS_SECRET_NAME%|${Domain.weblogicCredentialsSecretName}|g\" fmwk8s-tests-env-configmap.yaml && \
                        sed -i \"s|%CLUSTER_NAME%|${yamlUtility.domainInputsMap.get("clusterName")}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%MANAGED_SERVER_NAME_SVC%|${domainName}-cluster-${yamlUtility.domainInputsMap.get("clusterName")}.${domainNamespace}.svc.cluster.local|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%MANAGED_SERVER_NAME_BASE%|${yamlUtility.domainInputsMap.get("managedServerNameBase")}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%MANAGED_SERVER_PORT%|${yamlUtility.domainInputsMap.get("managedServerPort")}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%DB_HOST%|${Database.dbName}.${domainNamespace}|g\" fmwk8s-tests-env-configmap.yaml && \
                        sed -i \"s|%DB_PORT%|${Database.dbPort}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%JDBC_URL%|jdbc:oracle:thin:@${Database.dbName}.${domainNamespace}:${Database.dbPort}/${Database.dbName}pdb.us.oracle.com|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%CONNECTION_STRING%|${Database.dbName}.${domainNamespace}:${Database.dbPort}/${Database.dbName}pdb.us.oracle.com|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%DB_IMAGE%|container-registry.oracle.com/database/${databaseVersion}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%DB_SERVICE%|${Database.dbName}pdb.us.oracle.com|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%DB_NAME%|${Database.dbName}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%DB_SID%|${Database.dbName}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%DB_SCHEMA_PASSWORD%|${Database.dbSchemaPassword}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%RCUPREFIX%|${domainName}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%REGISTRY_SECRET%|${registrySecret}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%K8S_MASTER_URL%|${k8sMasterUrl}|g\" fmwk8s-tests-env-configmap.yaml && \
                        sed -i \"s|%FMWK8S_NFS_HOME%|${fmwk8sNfsHome}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%SAMPLES_REPO%|${samplesRepo}|g\" fmwk8s-tests-env-configmap.yaml && \
                        sed -i \"s|%SAMPLES_DIRECTORY%|${samplesDirectory}|g\" fmwk8s-tests-env-configmap.yaml && \
                        sed -i \"s|%SAMPLES_BRANCH%|${samplesBranch}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%TEST_IMAGE%|${testImage}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%TEST_TYPE%|${testType}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%TEST_TARGET%|${testTarget}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%LOG_DIRECTORY%|${logDirectory}|g\" fmwk8s-tests-env-configmap.yaml && \
						sed -i \"s|%HOURS_AFTER_SECONDS%|${hoursAfterSeconds}|g\" fmwk8s-tests-env-configmap.yaml && \
                        cat fmwk8s-tests-env-configmap.yaml"

            script.sh label: "create env variables configmap",
                    script: "kubectl apply -f kubernetes/framework/test/fmwk8s-tests-env-configmap.yaml -n ${domainNamespace}"

            Log.info("create env configmap success.")
        }
        catch (exc) {
            Log.error("create env configmap failed.")
            throw exc
        }
        finally {
        }
    }

    /** Method to wait for test execution and completion based on the file fmwk8s.completed if exists or not */
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
                        testInit=`echo \\`kubectl get pods -n ${domainNamespace} 2>&1 | grep ${testId}-test\\``\n \
                        done"

            /** wait in loop for fmwk8s.completed file*/
            Boolean waitforfile = true
            Boolean fmwk8sCompletedFileExists = true
            Boolean fmwk8sCompletedFileNotExists = false
            Integer countOfLooping = 0
            while (waitforfile) {
                /** Logic to check if the fmwk8s.completed file exists and is created after test execution */
                def fileExists = script.sh(
                        label: "check if the fmwk8s.completed file exists and is created after test execution",
                        script: "test -f ${Test.logDirectory}/fmwk8s.completed && echo ${fmwk8sCompletedFileExists} || echo ${fmwk8sCompletedFileNotExists}",
                        returnStdout: true).trim()
                Log.info("value  of fileExists :: ${fileExists}")
                if (fileExists == "true") {
                    waitforfile = false
                } else if (fileExists == "false") {
                    script.sh "sleep 120"
                    countOfLooping++
                    Log.info("Iteration :: ${countOfLooping}")
                    waitforfile = true
                    continue
                }
            }

            /** if the fmwk8s.completed file exists, then we calculate test Status and wait for hoursAfter*/
            if (!waitforfile) {
                def testContainerStatus = script.sh(
                        label: "get test status",
                        script: "kubectl get pods -n ${domainNamespace} 2>&1 | grep ${testId}-test",
                        returnStdout: true
                ).trim()
                if (testContainerStatus.toString().contains("Error")) {
                    testStatus = "failed"
                } else if (testContainerStatus.toString().contains("Completed")) {
                    testStatus = "completed"
                } else {
                    testStatus = "completed"
                }
                /** To fetch the test logs, archive it and publish the test
                 * logs to artifactory before email notification is sent to user after test execution before the hours after wait period */
                Logging.getTestLogsArchiveAndPublishTestLogsToArtifactory()
                /** Logic to evaluate the count of *.suc, *.dif & *.skip files in the test_logs folder after test runs */
                ReportUtility.countOfSucDifFilesAfterTestRunsAndGenerateTestSummaryReport(script)
                /** if (file found){  wait for hoursAfter (to be safe. and not rely on timer in container to finish) - reuse EnvironmentSetup.waitHoursAfter}*/
                EnvironmentSetup.waitHoursAfter()
                Log.info("wait for test completion success.")
            }
        }
        catch (exc) {
            Log.error("wait for test completion failed.")
            throw exc
        }
    }

    static cleanup() {
        if (EnvironmentSetup.isWaiting) {
            Log.info("bypassing hoursAfter and cleaning test resources.")
        }
        if (!testStatus.toString().equalsIgnoreCase("nil")) {
            try {
                Log.info("begin cleanup test resources.")
                script.sh label: "cleanup test pod",
                        script: "kubectl delete -f kubernetes/framework/test/${testId}/fmwk8s-${testId}-test-pod.yaml -n ${domainNamespace} --grace-period=0 --force --cascade"
                sleep 30
                Log.info("cleanup test resources success.")
            }
            catch (exc) {
                Log.error("cleanup test resources failed.")
                exc.printStackTrace()
            }
            finally {
                Log.info("cleanup test pv/pvc.")
                script.sh label: "cleanup test pv/pvc",
                        script: "kubectl delete pvc fmwk8s-tests-pvc-${runId} -n ${domainNamespace} --grace-period=0 --force --cascade && \
                             sleep 30 && \
                             kubectl delete pv fmwk8s-tests-pv-${runId} -n ${domainNamespace} --grace-period=0 --force --cascade"
            }
        } else {
            Log.info("skipping test resource cleanup since no tests executed.")
        }
    }

    static dockerInspectTestImageAndCreateWrapperDockerEntryPointScript() {
        try {
            Log.info("begin Docker Inspect TestImage And Create Wrapper DockerEntryPointScript")

            /** Pull docker test image into DIND Container */
            script.sh label: "Pull docker test image into DIND Container",
                    script: "docker pull ${testImage}"

            /** Run a docker inspect command on this test image to retrieve the specified entrypoint script details of test image.*/
            def entryPointScriptFileNameAndPath = script.sh(
                    label: "Run a docker inspect command on this test image to retrieve the specified entrypoint script details",
                    script: "echo `docker inspect  -f '{{.ContainerConfig.Entrypoint}}' ${testImage} | tr '[' ' '|tr ']' ' ' |sed 's/ //g'`",
                    returnStdout: true)

            Log.info("entryPointScriptFileNameAndPath :: ${entryPointScriptFileNameAndPath}")

            /** Create a new wrapper docker entrypoint script for the test image for maintaining hoursafter environment */
            script.sh label: "Create a new wrapper docker entrypoint script for the test image",
                    script: "touch ${script.env.WORKSPACE}/docker-entrypoint.sh && \
                              echo \"#!/bin/bash\" >> ${script.env.WORKSPACE}/docker-entrypoint.sh && \
                              echo \"\" >> ${script.env.WORKSPACE}/docker-entrypoint.sh && \
                              echo \"sh ${entryPointScriptFileNameAndPath}\" >> ${script.env.WORKSPACE}/docker-entrypoint.sh && \
                              echo \"touch %LOG_DIRECTORY%/fmwk8s.completed\" >> ${script.env.WORKSPACE}/docker-entrypoint.sh && \
                              echo \"echo Retaining Environment for %HOURS_AFTER_SECONDS% seconds...\" >> ${script.env.WORKSPACE}/docker-entrypoint.sh && \
                              echo \"sleep %HOURS_AFTER_SECONDS%\" >> ${script.env.WORKSPACE}/docker-entrypoint.sh"

            /** Check the contents of the new docker entrypoint script */
            script.sh label: "Check contents of a new wrapper docker entrypoint script for the test image",
                    script: "chmod 777 ${script.env.WORKSPACE}/docker-entrypoint.sh && \
                              cat ${script.env.WORKSPACE}/docker-entrypoint.sh && \
                              ls -ltr ${script.env.WORKSPACE}"

            Log.info("Docker Inspect TestImage And Create Wrapper DockerEntryPointScript is Success")
        }
        catch (exc) {
            Log.info("Docker Inspect TestImage And Create Wrapper DockerEntryPointScript is Failed")
            throw exc
        }
        finally {
        }
    }
}
