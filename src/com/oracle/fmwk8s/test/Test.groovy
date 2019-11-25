package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.EnvironmentSetup
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.env.Domain
import com.oracle.fmwk8s.utility.ReportUtility

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
                        testInit=`echo \\`kubectl get pods -n ${Domain.domainNamespace} 2>&1 | grep fmwk8s-${testId}-test\\``\n \
                        done"

            script.sh "test -f ${Test.logDirectory}/fmwk8s.completed && echo 'file exists' || echo 'file not exists'"

            /** wait in loop for fmwk8s.completed file*/
            Boolean waitforfile = true
            while(waitforfile){
                /** Logic to check if the fmwk8s.completed file exists and is created after test execution */
                def fileExists = script.sh(
                        label: "check if the fmwk8s.completed file exists and is created after test execution",
                        script: "test -f ${Test.logDirectory}/fmwk8s.completed && echo 'file_exist' || echo 'file_not_exist'",
                        returnStdout: true).trim()
                if(fileExists == 'file_exist') {
                    waitforfile  = false
                }else if(fileExists == 'file_not_exist'){
                    continue
                }
            }

            /** if the fmwk8s.completed file exists, then we calculate test Status and wait for hoursAfter*/
            if(!waitforfile) {
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
        if (!EnvironmentSetup.isWaiting) {
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
}
