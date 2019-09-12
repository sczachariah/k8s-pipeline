package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.test.Functional
import com.oracle.fmwk8s.utility.YamlUtility


class Logging {

    static def yamlUtility = new YamlUtility()
    static def buildSuffix
    static def productImageVersion
    static def logLocation

    static configureLogstashConfigmap(script, domainName, domainNamespace) {
        try {
            Log.info(script, "begin configure logstash configmap.")

            script.sh "cd ../fmwk8s/kubernetes/framework/logging && \
                       sed -i \"s#%DOMAIN_NAME%#${domainName}#g\" logstash-configmap.yaml && \
                       sed -i \"s#%ELASTICSEARCH_HOST%#${Common.elasticSearchHost}:${Common.elasticSearchPort}#g\" logstash-configmap.yaml && \
                       cat logstash-configmap.yaml && \
                       kubectl apply -f logstash-configmap.yaml -n ${domainNamespace} && \
                       sleep 60"

            Log.info(script, "configure logstash configmap success.")
        }
        catch (exc) {
            Log.error(script, "configure logstash configmap failed.")
            throw exc
        }
    }

    static configureLogstash(script, domainName, domainNamespace) {
        try {
            Log.info(script, "begin configure logstash.")

            script.sh "cd ../fmwk8s/kubernetes/framework/logging && \
                       sed -i \"s#%DOMAIN_NAME%#${domainName}#g\" fmwk8s-logstash-config-pod.yaml && \
                       sed -i \"s#%DOMAIN_PVC%#${domainName}-${domainNamespace}-pvc#g\" fmwk8s-logstash-config-pod.yaml && \
                       cat fmwk8s-logstash-config-pod.yaml && \
                       kubectl apply -f fmwk8s-logstash-config-pod.yaml -n ${domainNamespace} && \
                       sleep 60"

            Log.info(script, "configure logstash success.")
        }
        catch (exc) {
            Log.error(script, "configure logstash failed.")
            throw exc
        }

    }

    static updateLogstashDeployment(script, domainName, domainNamespace) {
        try {
            Log.info(script, "begin update and deploy logstash.")

            script.sh "cd ../fmwk8s/kubernetes/framework/logging && \
                       sed -i \"s#%DOMAIN_NAME%#${domainName}#g\" logstash-deployment.yaml && \
                       sed -i \"s#%DOMAIN_NAMESPACE%#${domainNamespace}#g\" logstash-deployment.yaml && \
                       sed -i \"s#%DOMAIN_PVC%#${domainName}-${domainNamespace}-pvc#g\" logstash-deployment.yaml && \
                       cat logstash-deployment.yaml && \
                       kubectl apply -f logstash-deployment.yaml -n ${domainNamespace} && \
                       sleep 60"

            Log.info(script, "update and deploy logstash success.")
        }
        catch (exc) {
            Log.error(script, "update and deploy logstash failed.")
            throw exc
        }

    }

    static deployLogstash(script, elkEnable, domainName, domainNamespace) {
        try {
            Log.info(script, "begin deploy logstash.")

            if ("${elkEnable}" == "true") {
                Log.info(script, "elk is enabled.")
                configureLogstashConfigmap(script, domainName, domainNamespace)
                configureLogstash(script, domainName, domainNamespace)
                updateLogstashDeployment(script, domainName, domainNamespace)
                Log.info(script, "deploy logstash success.")
            } else {
                Log.info(script, "elk is disabled.")
            }
        }
        catch (exc) {
            Log.error(script, "deploy logstash failed.")
            throw exc
        }


    }

    static getLogs(script) {
        //getEventLogs(script, Operator.operatorNamespace)
        //getEventLogs(script, Domain.domainNamespace)
        getDomainLogs(script, Domain.domainName, Domain.domainNamespace)
        //archiveLogs(script)
        //publishLogsToArtifactory(script)
    }

    static getEventLogs(script, namespace) {
        try {
            Log.info(script, "begin get event logs.")
            script.sh "mkdir -p ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/event_logs && \
                       chmod 777 ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/event_logs && \
                       kubectl get events --namespace=${namespace} > ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/event_logs/${namespace}-event.txt && \
                       ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/event_logs"
            Log.info(script, "get event logs success.")
        }
        catch (exc) {
            Log.error(script, "get event logs failed.")
        }
    }

    static getPodLogs(script, podname, namespace) {
        try {
            Log.info(script, "begin get pod logs.")
            script.sh "mkdir -p ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/pod_logs && \
                       chmod 777 ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/pod_logs && \
                       kubectl logs ${podname} -n ${namespace} > ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/pod_logs/${podname}-pod.txt && \
                       ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/pod_logs"
            Log.info(script, "get pod logs success.")
        }
        catch (exc) {
            Log.error(script, "get pod logs failed.")
        }
    }

    static getDomainLogs(script, domainName, domainNamespace) {
        try {
            Log.info(script, "begin get domain logs sfdfsfsfs.")
            script.sh "mkdir -p ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs && \
                       chmod 777 ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs"
            script.sh "adminServer=`echo \\`kubectl get pods -n ${domainNamespace} 2>&1 | grep admin-server\\``\n \
                       echo \"\$adminServer\"\n \
                       if [[ \$adminServer ]]; then \n \
                             echo \"Domain Found\" \n \
                             kubectl cp ${domainNamespace}/${domainName}-${YamlUtility.domainInputsMap.get("adminServerName")}:${YamlUtility.domainInputsMap.get("logHome")} ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs \n \
                       fi"
            script.sh "ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs && \
                       cd ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}"
            script.sh "ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}"
            Log.info(script, "get domain logs success.")
        }
        catch (exc) {
            Log.error(script, "get domain logs failed.")
            throw exc
        }
    }

    static getTestLogs(script) {
        try {
            Log.info(script, "begin get test logs.")

            script.sh "mkdir -p ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs && \
                       chmod 777 ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs && \
                       ls -ltr ${Functional.logDirectory} && \
                       cp -r ${Functional.logDirectory}/** ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs/ && \
                       ls -ltr ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs && \
                       cd ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}"
            script.sh "ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}"

            Log.info(script, "get test logs success.")
        }
        catch (exc) {
            Log.error(script, "get test logs failed.")
        }
    }

    static archiveLogs(script) {
        try {
            Log.info(script, "archive logs.")
            buildSuffix = "${script.env.BUILD_NUMBER}-${Common.operatorVersion}-${Common.productName}"

            script.zip zipFile: "event_logs_${buildSuffix}.zip", archive: true, dir: "${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/event_logs"
            script.zip zipFile: "pod_logs_${buildSuffix}.zip", archive: true, dir: "${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/pod_logs"
            script.zip zipFile: "domain_logs_${buildSuffix}.zip", archive: true, dir: "${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs"
            script.zip zipFile: "test_logs_${buildSuffix}.zip", archive: true, dir: "${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs"

            script.archiveArtifacts artifacts: '**/*_logs_*.zip'
            //script.archiveArtifacts artifacts: '**/pod_logs_*.zip'
            //script.archiveArtifacts artifacts: '**/domain_logs_*.zip'
            //script.archiveArtifacts artifacts: '**/test_logs_*.zip'
            Log.info(script, "archive logs success.")
        }
        catch (exc) {
            Log.error(script, "archive logs failed.")
        }
    }

    static publishLogsToArtifactory(script) {
        try {
            Log.info(script, "publish logs to artifactory.")
            script.sh "pwd && \
                       ls"
            this.productImageVersion = script.sh(
                    script: "echo ${Common.productImage}| awk -F':' '{print \$2}'",
                    returnStdout: true
            ).trim()
            Log.info(script,this.productImageVersion)
            script.rtUpload(
                    serverId: "artifacthub.oraclecorp.com",
                    spec:
                            """{
                           "files": [
                             {
                                "pattern": "event_logs_*.zip",
                                "target": "fmwk8s-dev-local/com/oracle/fmwk8sval/logs/${Common.productName}/${this.productImageVersion}/${script.env.BUILD_NUMBER}/"
                             },
                             {
                                "pattern": "domain_logs_*.zip",
                                "target": "fmwk8s-dev-local/com/oracle/fmwk8sval/logs/${Common.productName}/${this.productImageVersion}/${script.env.BUILD_NUMBER}/"
                             },
                             {
                                "pattern": "pod_logs_*.zip",
                                "target": "fmwk8s-dev-local/com/oracle/fmwk8sval/logs/${Common.productName}/${this.productImageVersion}/${script.env.BUILD_NUMBER}/"
                             },
                             {
                                "pattern": "test_logs_*.zip",
                                "target": "fmwk8s-dev-local/com/oracle/fmwk8sval/logs/${Common.productName}/${this.productImageVersion}/${script.env.BUILD_NUMBER}/"
                             }                           
                           ]
                        }""",
                    failNoOp: true
            )
            Log.info(script, "publish logs to artifactory success.")
        }
        catch (exc) {
            Log.error(script, "publish logs to artifactory failed.")
        }
    }
}
