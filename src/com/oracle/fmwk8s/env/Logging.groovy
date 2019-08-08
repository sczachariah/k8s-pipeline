package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log

class Logging {

    static configureLogstashConfigmap(script, domainName, domainNamespace) {
        try {
            Log.info(script, "begin configure logstash configmap.")

            script.sh "export KUBECONFIG=${script.env.KUBECONFIG} && \
                       cd ../fmwk8s/kubernetes/framework/logging && \
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

            script.sh "export KUBECONFIG=${script.env.KUBECONFIG} && \
                       cd ../fmwk8s/kubernetes/framework/logging && \
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

            script.sh "export KUBECONFIG=${script.env.KUBECONFIG} && \
                       cd ../fmwk8s/kubernetes/framework/logging && \
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
}
