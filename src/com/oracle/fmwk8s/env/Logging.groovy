package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.common.Common

class Logging {

    static configureLogstashConfigmap(script, domainName) {
        try {
            Log.info(script, "begin configure logstash configmap.")

            script.sh "export KUBECONFIG=${script.env.KUBECONFIG} && \
                       cd kubernetes/framework/logging && \
                       sed -i \"s#%DOMAIN_NAME%#${domainName}#g\" logstash-configmap.yaml && \
                       sed -i \"s#%ELASTICSEARCH_HOST%#${Common.elasticSearchHost}:${Common.elasticSearchPort}#g\" logstash-configmap.yaml && \
                       cat logstash-configmap.yaml && \
                       sleep 60"

            Log.info(script, "configure logstash configmap success.")
        }
        catch (exc) {
            Log.error(script, "configure logstash configmap failed.")
        }
        finally {
        }
    }

    static configureLogstash(script, domainName, domainNamespace) {
        try {
            Log.info(script, "begin configure logstash.")

            script.sh "export KUBECONFIG=${script.env.KUBECONFIG} && \
                       cd kubernetes/framework/logging && \
                       sed -i \"s#%DOMAIN_NAME%#${domainName}#g\" fmwk8s-logstash-config-pod.yaml && \
                       sed -i \"s#%DOMAIN_PVC%#${domainName}-${domainNamespace}-pvc#g\" fmwk8s-logstash-config-pod.yaml && \
                       cat fmwk8s-logstash-config-pod.yaml && \
                       kubectl apply -f fmwk8s-logstash-config-pod.yaml -n ${domainNamespace} && \
                       sleep 60"

            Log.info(script, "configure logstash success.")
        }
        catch (exc) {
            Log.error(script, "configure logstash failed.")
        }
        finally {
        }
    }

    static updateLogstashDeployment(script, domainName, domainNamespace) {
        try {
            Log.info(script, "begin configure logstash.")

            script.sh "export KUBECONFIG=${script.env.KUBECONFIG} && \
                       cd kubernetes/framework/logging && \
                       sed -i \"s#%DOMAIN_NAME%#${domainName}#g\" logstash-deployment.yaml && \
                       sed -i \"s#%DOMAIN_NAMESPACE%#${domainNamespace}#g\" logstash-deployment.yaml && \
                       sed -i \"s#%DOMAIN_PVC%#${domainName}-${domainNamespace}-pvc#g\" logstash-deployment.yaml && \
                       cat logstash-deployment.yaml && \
                       sleep 60"

            Log.info(script, "configure logstash success.")
        }
        catch (exc) {
            Log.error(script, "configure logstash failed.")
        }
        finally {
        }
    }

    static deployLogstashDeployment(script, domainNamespace) {
        try {
            Log.info(script, "begin logstash deployment.")

            script.sh "export KUBECONFIG=${script.env.KUBECONFIG} && \
                       cd kubernetes/framework/logging && \
                       kubectl apply -f logstash-deployment.yaml -n ${domainNamespace} && \
                       sleep 60"

            Log.info(script, "logstash deployment success.")
        }
        catch (exc) {
            Log.error(script, "logstash deployment failed.")
        }
        finally {
        }
    }

    static deployLogstash(script, domainName, domainNamespace) {
        try {
            Log.info(script, "begin deploy logstash.")

            configureLogstashConfigmap(script, domainName)
            configureLogstash(script, domainName, domainNamespace)
            updateLogstashDeployment(script, domainName, domainNamespace)
            deployLogstashDeployment(script, domainNamespace)

            Log.info(script, "deploy logstash success.")
        }
        catch (exc) {
            Log.error(script, "deploy logstash failed.")
        }
        finally {
        }


    }
}
