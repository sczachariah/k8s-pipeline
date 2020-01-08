package com.oracle.fmwk8s.env


import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.test.Test

class Logging extends Common {

    static def buildSuffix
    static def productImageVersion
    static def artifactoryServer = "http://artifacthub.oraclecorp.com/"
    static def fmwk8sArtifactoryLogLocation

    static configureLogstashConfigmap() {
        try {
            Log.info("begin configure logstash configmap.")

            script.sh label: "create logstash configmap",
                    script: "pwd && \
                       ls && \
                       cd kubernetes/framework/logging && \
                       sed -i \"s#%DOMAIN_NAME%#${domainName}#g\" logstash-configmap.yaml && \
                       sed -i \"s#%RUN_ID%#${runId}#g\" logstash-configmap.yaml && \
                       sed -i \"s#%ELASTICSEARCH_HOST%#${elasticSearchHost}:${elasticSearchPort}#g\" logstash-configmap.yaml && \
                       cat logstash-configmap.yaml && \
                       kubectl apply -f logstash-configmap.yaml -n ${domainNamespace} && \
                       sleep 60"

            Log.info("configure logstash configmap success.")
        }
        catch (exc) {
            Log.error("configure logstash configmap failed.")
            throw exc
        }
    }

    static configureLogstash() {
        try {
            Log.info("begin configure logstash.")

            script.sh label: "create logstash configuration pod",
                    script: "cd kubernetes/framework/logging && \
                       sed -i \"s#%DOMAIN_NAME%#${domainName}#g\" fmwk8s-logstash-config-pod.yaml && \
                       sed -i \"s#%DOMAIN_PVC%#${domainName}-${domainNamespace}-pvc#g\" fmwk8s-logstash-config-pod.yaml && \
                       sed -i \"s#%RUN_ID%#${runId}#g\" fmwk8s-logstash-config-pod.yaml && \
                       cat fmwk8s-logstash-config-pod.yaml && \
                       kubectl apply -f fmwk8s-logstash-config-pod.yaml -n ${domainNamespace} && \
                       sleep 60"

            Log.info("configure logstash success.")
        }
        catch (exc) {
            Log.error("configure logstash failed.")
            throw exc
        }

    }

    static updateLogstashDeployment() {
        try {
            Log.info("begin update and deploy logstash.")

            script.sh label: "create logstash pod",
                    script: "cd kubernetes/framework/logging && \
                       sed -i \"s#%DOMAIN_NAME%#${domainName}#g\" logstash-deployment.yaml && \
                       sed -i \"s#%DOMAIN_NAMESPACE%#${domainNamespace}#g\" logstash-deployment.yaml && \
                       sed -i \"s#%DOMAIN_PVC%#${domainName}-${domainNamespace}-pvc#g\" logstash-deployment.yaml && \
                       sed -i \"s#%RUN_ID%#${runId}#g\" logstash-deployment.yaml && \
                       cat logstash-deployment.yaml && \
                       kubectl apply -f logstash-deployment.yaml -n ${domainNamespace} && \
                       sleep 60"

            Log.info("update and deploy logstash success.")
        }
        catch (exc) {
            Log.error("update and deploy logstash failed.")
            throw exc
        }

    }

    static deployLogstash() {
        try {
            Log.info("begin deploy logstash.")

            script.git branch: 'master',
                    credentialsId: "${sshCredentialId}",
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            if ("${elkEnable}" == "true") {
                Log.info("elk is enabled.")
                configureLogstashConfigmap()
                configureLogstash()
                updateLogstashDeployment()
                Log.info("deploy logstash success.")
            } else {
                Log.info("elk is disabled.")
            }
        }
        catch (exc) {
            Log.error("deploy logstash failed.")
            throw exc
        }


    }

    static getLogs() {
        getEventLogs(operatorNamespace)
        getEventLogs(domainNamespace)
        getOperatorLogs(operatorNamespace)
        getDomainLogs(domainName, domainNamespace)
        archiveLogs()
        publishLogsToArtifactory()
    }

    static getEventLogs(namespace) {
        try {
            Log.info("begin get event logs.")
            script.sh label: "get event logs",
                    script: "mkdir -p ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/event_logs && \
                       chmod 777 ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/event_logs && \
                       kubectl get events --namespace=${namespace} > ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/event_logs/${namespace}-event.txt && \
                       ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/event_logs"
            Log.info("get event logs success.")
        }
        catch (exc) {
            Log.error("get event logs failed.")
            exc.printStackTrace()
        }
    }

    static getPodLogs(podname, namespace) {
        try {
            Log.info("begin get pod logs.")
            script.sh label: "get pod logs",
                    script: "mkdir -p ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/pod_logs && \
                       chmod 777 ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/pod_logs && \
                       kubectl logs ${podname} -n ${namespace} > ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/pod_logs/${podname}-pod.txt && \
                       ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/pod_logs"
            Log.info("get pod logs success.")
        }
        catch (exc) {
            Log.error("get pod logs failed.")
            exc.printStackTrace()
        }
    }

    static getOperatorLogs(namespace) {
        try {
            Log.info("begin get operator logs.")
            def operatorPodName = script.sh(
                    label: "get operator pod name",
                    script: "kubectl get pods -o go-template --template \'{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}\' -n ${namespace} | grep operator",
                    returnStdout: true
            ).trim()

            script.sh label: "get operator logs",
                    script: "mkdir -p ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/operator_logs && \
                       chmod 777 ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/operator_logs && \
                       kubectl logs ${operatorPodName} -n ${namespace} > ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/operator_logs/${operatorPodName}-pod.txt && \
                       ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/operator_logs"
            Log.info("get operator logs success")
        }
        catch (exc) {
            Log.error("get operator logs failed.")
            exc.printStackTrace()
        }
    }

    static getDomainLogs(domainName, namespace) {
        try {
            Log.info("begin get domain logs.")
            if (yamlUtility.domainInputsMap != null) {
                script.sh label: "get domain logs",
                        script: "mkdir -p ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs && \
                       chmod 777 ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs"
                script.sh label: "verify domain was created",
                        script: "adminServer=`echo \\`kubectl get pods -n ${namespace} 2>&1 | grep admin-server\\``\n \
                       echo \"\$adminServer\"\n \
                       if [[ \$adminServer ]]; then \n \
                             echo \"Domain Found\" \n \
                             kubectl cp ${namespace}/${domainName}-${yamlUtility.domainInputsMap.get("adminServerName")}:${yamlUtility.domainInputsMap.get("logHome")} ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs \n \
                       fi"
                script.sh label: "verify logs",
                        script: "ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs && \
                       cd ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER} && \
                      ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}"
                Log.info("get domain logs success.")
            } else {
                Log.info("no domain logs exist.")
            }
        }
        catch (exc) {
            Log.error("get domain logs failed.")
            exc.printStackTrace()
        }
    }

    static getTestLogs() {
        try {
            Log.info("begin get test logs.")

            script.sh label: "get test logs",
                    script: "mkdir -p ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs && \
                       chmod 777 ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs && \
                       ls -ltr ${Test.logDirectory} && \
                       cp -r ${Test.logDirectory}/** ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs/ && \
                       ls -ltr ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs && \
                       cd ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER} && \
                       ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}"

            Log.info("get test logs success.")
        }
        catch (exc) {
            Log.error("get test logs failed.")
            exc.printStackTrace()
        }
    }

    static archiveTestLogs() {
        try {
            Log.info("archive test logs.")
            buildSuffix = "${script.env.BUILD_NUMBER}-${Common.operatorVersion}-${Common.productName}"

            script.zip zipFile: "test_logs_${buildSuffix}.zip", archive: true, dir: "${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs"

            script.archiveArtifacts artifacts: '**/*_logs_*.zip'
            Log.info("archive test logs success.")
        }
        catch (exc) {
            Log.error("archive test logs failed.")
            exc.printStackTrace()
        }
    }

    static archiveLogs() {
        try {
            Log.info("archive logs.")
            buildSuffix = "${script.env.BUILD_NUMBER}-${Common.operatorVersion}-${Common.productName}"

            script.zip zipFile: "event_logs_${buildSuffix}.zip", archive: true, dir: "${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/event_logs"
            script.zip zipFile: "pod_logs_${buildSuffix}.zip", archive: true, dir: "${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/pod_logs"
            script.zip zipFile: "domain_logs_${buildSuffix}.zip", archive: true, dir: "${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs"
            script.zip zipFile: "operator_logs_${buildSuffix}.zip", archive: true, dir: "${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/operator_logs"

            script.archiveArtifacts artifacts: '**/*_logs_*.zip'
            Log.info("archive logs success.")
        }
        catch (exc) {
            Log.error("archive logs failed.")
            exc.printStackTrace()
        }
    }

    static publishTestLogsToArtifactory() {
        try {
            Log.info("publish Test logs to artifactory.")
            script.sh label: "get product image version qualifier",
                    script: "pwd && \
                       ls"
            productImageVersion = script.sh(
                    script: "echo ${productImage}| awk -F':' '{print \$2}'",
                    returnStdout: true
            ).trim()
            Log.info(productImageVersion)
            fmwk8sArtifactoryLogLocation = "fmwk8s-dev-local/com/oracle/fmwk8sval/logs/${productName}/${productImageVersion}/${runId}/"
            script.rtUpload(
                    serverId: "artifacthub.oraclecorp.com",
                    spec:
                            """{
                           "files": [
                             {
                                "pattern": "test_logs_*.zip",
                                "target": "fmwk8s-dev-local/com/oracle/fmwk8sval/logs/${productName}/${
                                productImageVersion
                            }/${runId}/"
                             }                           
                           ]
                        }""",
                    failNoOp: true
            )
            Log.info("publish test logs to artifactory success.")
        }
        catch (exc) {
            Log.error("publish test logs to artifactory failed.")
            throw exc
        }
    }

    static publishLogsToArtifactory() {
        try {
            Log.info("publish logs to artifactory.")
            script.sh label: "get product image version qualifier",
                    script: "pwd && \
                       ls"
            productImageVersion = script.sh(
                    script: "echo ${productImage}| awk -F':' '{print \$2}'",
                    returnStdout: true
            ).trim()
            Log.info(productImageVersion)
            fmwk8sArtifactoryLogLocation = "fmwk8s-dev-local/com/oracle/fmwk8sval/logs/${productName}/${productImageVersion}/${runId}/"
            script.rtUpload(
                    serverId: "artifacthub.oraclecorp.com",
                    spec:
                            """{
                           "files": [
                             {
                                "pattern": "event_logs_*.zip",
                                "target": "fmwk8s-dev-local/com/oracle/fmwk8sval/logs/${productName}/${
                                productImageVersion
                            }/${runId}/"
                             },
                             {
                                "pattern": "domain_logs_*.zip",
                                "target": "fmwk8s-dev-local/com/oracle/fmwk8sval/logs/${productName}/${
                                productImageVersion
                            }/${runId}/"
                             },
                             {
                                "pattern": "operator_logs_*.zip",
                                "target": "fmwk8s-dev-local/com/oracle/fmwk8sval/logs/${productName}/${
                                productImageVersion
                            }/${runId}/"
                             },
                             {
                                "pattern": "pod_logs_*.zip",
                                "target": "fmwk8s-dev-local/com/oracle/fmwk8sval/logs/${productName}/${
                                productImageVersion
                            }/${runId}/"
                             }                           
                           ]
                        }""",
                    failNoOp: true
            )
            Log.info("publish logs to artifactory success.")
        }
        catch (exc) {
            Log.error("publish logs to artifactory failed.")
            throw exc
        }
    }

    /**
     * getTestLogsArchiveAndPublishTestLogsToArtifactory - Method to fetch the test logs, archive it and publish the test
     * logs to artifactory before email notification is sent to user after test execution before the hours after wait period of time!.
     * @return
     */
    static getTestLogsArchiveAndPublishTestLogsToArtifactory() {
        /** Trying to collect all the test logs under the test_logs directory after successful test runs */
        getTestLogs()
        /** Archive the test logs collected */
        archiveTestLogs()
        /** Publish the test logs to artifactory */
        publishTestLogsToArtifactory()
    }

}
