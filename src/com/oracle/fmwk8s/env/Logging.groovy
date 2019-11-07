package com.oracle.fmwk8s.env


import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.test.Test

class Logging extends Common {

    static def buildSuffix
    static def productImageVersion

    static configureLogstashConfigmap() {
        try {
            Log.info("begin configure logstash configmap.")

            script.sh "cd ../fmwk8s/kubernetes/framework/logging && \
                       sed -i \"s#%DOMAIN_NAME%#${domainName}#g\" logstash-configmap.yaml && \
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

            script.sh "cd ../fmwk8s/kubernetes/framework/logging && \
                       sed -i \"s#%DOMAIN_NAME%#${domainName}#g\" fmwk8s-logstash-config-pod.yaml && \
                       sed -i \"s#%DOMAIN_PVC%#${domainName}-${domainNamespace}-pvc#g\" fmwk8s-logstash-config-pod.yaml && \
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

            script.sh "cd ../fmwk8s/kubernetes/framework/logging && \
                       sed -i \"s#%DOMAIN_NAME%#${domainName}#g\" logstash-deployment.yaml && \
                       sed -i \"s#%DOMAIN_NAMESPACE%#${domainNamespace}#g\" logstash-deployment.yaml && \
                       sed -i \"s#%DOMAIN_PVC%#${domainName}-${domainNamespace}-pvc#g\" logstash-deployment.yaml && \
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

            if ("${elkEnable}" == "true") {
                Log.info("elk is enabled.")
                configureLogstashConfigmap(script, domainName, domainNamespace)
                configureLogstash(script, domainName, domainNamespace)
                updateLogstashDeployment(script, domainName, domainNamespace)
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
            script.sh "mkdir -p ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/event_logs && \
                       chmod 777 ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/event_logs && \
                       kubectl get events --namespace=${namespace} > ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/event_logs/${namespace}-event.txt && \
                       ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/event_logs"
            Log.info("get event logs success.")
        }
        catch (exc) {
            Log.error("get event logs failed.")
        }
    }

    static getPodLogs(podname, namespace) {
        try {
            Log.info("begin get pod logs.")
            script.sh "mkdir -p ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/pod_logs && \
                       chmod 777 ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/pod_logs && \
                       kubectl logs ${podname} -n ${namespace} > ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/pod_logs/${podname}-pod.txt && \
                       ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/pod_logs"
            Log.info("get pod logs success.")
        }
        catch (exc) {
            Log.error("get pod logs failed.")
        }
    }

    static getOperatorLogs(namespace) {
        try {
            Log.info("begin get operator logs.")
            def operatorPodName = script.sh(
                    script: "kubectl get pods -o go-template --template \'{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}\' -n ${namespace} | grep operator",
                    returnStdout: true
            ).trim()

            script.sh "mkdir -p ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/operator_logs && \
                       chmod 777 ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/operator_logs && \
                       kubectl logs ${operatorPodName} -n ${namespace} > ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/operator_logs/${operatorPodName}-pod.txt && \
                       ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/operator_logs"
            Log.info("get operator logs success")
        }
        catch (exc) {
            Log.error("get operator logs failed.")
        }
    }

    static getDomainLogs(domainName, namespace) {
        try {
            Log.info("begin get domain logs.")
            if (yamlUtility.domainInputsMap != null) {
                script.sh "mkdir -p ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs && \
                       chmod 777 ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs"
                script.sh "adminServer=`echo \\`kubectl get pods -n ${namespace} 2>&1 | grep admin-server\\``\n \
                       echo \"\$adminServer\"\n \
                       if [[ \$adminServer ]]; then \n \
                             echo \"Domain Found\" \n \
                             kubectl cp ${namespace}/${domainName}-${yamlUtility.domainInputsMap.get("adminServerName")}:${yamlUtility.domainInputsMap.get("logHome")} ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs \n \
                       fi"
                script.sh "ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs && \
                       cd ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}"
                script.sh "ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}"
                Log.info("get domain logs success.")
            } else {
                Log.info("no domain logs exist.")
            }
        }
        catch (exc) {
            Log.error("get domain logs failed.")
        }
    }

    static getTestLogs() {
        try {
            Log.info("begin get test logs.")

            script.sh "mkdir -p ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs && \
                       chmod 777 ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs && \
                       ls -ltr ${Test.logDirectory} && \
                       cp -r ${Test.logDirectory}/** ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs/ && \
                       ls -ltr ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs && \
                       cd ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}"
            script.sh "ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}"

            Log.info("get test logs success.")
        }
        catch (exc) {
            Log.error("get test logs failed.")
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
            script.zip zipFile: "test_logs_${buildSuffix}.zip", archive: true, dir: "${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs"

            script.archiveArtifacts artifacts: '**/*_logs_*.zip'
            Log.info("archive logs success.")
        }
        catch (exc) {
            Log.error("archive logs failed.")
        }
    }

    static publishLogsToArtifactory() {
        try {
            Log.info("publish logs to artifactory.")
            script.sh "pwd && \
                       ls"
            productImageVersion = script.sh(
                    script: "echo ${productImage}| awk -F':' '{print \$2}'",
                    returnStdout: true
            ).trim()
            Log.info(productImageVersion)
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
                                "pattern": "pod_logs_*.zip",
                                "target": "fmwk8s-dev-local/com/oracle/fmwk8sval/logs/${productName}/${
                                productImageVersion
                            }/${runId}/"
                             },
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
            Log.info("publish logs to artifactory success.")
        }
        catch (exc) {
            Log.error("publish logs to artifactory failed.")
        }
    }
}
