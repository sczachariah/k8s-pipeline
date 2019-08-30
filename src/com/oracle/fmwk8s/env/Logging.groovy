package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.utility.YamlUtility


class Logging {
    
    static def yamlUtility = new YamlUtility()

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

    static fetchDomainLogs(script, domainName, domainNamespace) {
        try {
            Log.info(script, "begin get domain logs.")
            script.sh "mkdir -p ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs && \
                       chmod 777 ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs && \
                       kubectl cp ${domainNamespace}/${domainName}-${YamlUtility.domainInputsMap.get("adminServerName")}:${YamlUtility.domainInputsMap.get("logHome")} ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs && \
                       ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs && \
                       cd ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}"
            script.zip zipFile: "domain_logs.zip", archive: true, dir: "${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/domain_logs"
            script.sh "ls ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}"
            Log.info(script, "get domain logs success.")
        }
        catch (exc) {
            Log.error(script, "get domain logs failed.")
        }
    }

    static fetchTestLogs(script, domainNamespace) {
        try {
            Log.info(script, "begin get test logs.")

            script.sh "mkdir -p ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs && \
                       chmod 777 ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs && \
                       ls -ltr /logs && \
                       cp -r /logs/ ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs/ && \
                       ls -ltr ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs && \
                       cd ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}"
            script.zip zipFile: "test_logs.zip", archive: true, dir: "${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/test_logs"
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
            script.sh "cd ${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}"
            script.zip zipFile: "event_logs.zip", archive: true, dir: "${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/event_logs"
            script.zip zipFile: "pod_logs.zip", archive: true, dir: "${script.env.WORKSPACE}/${script.env.BUILD_NUMBER}/pod_logs"
            script.archiveArtifacts artifacts: '**/event_logs.zip'
            script.archiveArtifacts artifacts: '**/pod_logs.zip'
            script.archiveArtifacts artifacts: '**/domain_logs.zip'
            Log.info(script, "archive logs success.")
        }
        catch (exc) {
            Log.error(script, "archive logs failed.")
        }
    }
}
