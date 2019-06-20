package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Log

class Domain {
    static configureDomainSecret(script) {}

    static preparePersistentVolume(script) {}

    static prepareDomain(script) {}

    static createDomain(script) {}

    static isDomainReady(script) {}

    static createNamespace(script, namespace) {
        try {
            Log.info(script, "create domain namespace!!")
            script.sh "export KUBECONFIG=${script.env.KUBECONFIG}"
            script.sh "kubectl create ns ${namespace}"
        }
        catch (exc) {
            Log.error(script, "Create Domain namespace failed!!.")
        }
        finally {
            Log.info(script, "initialize helm!!")
            script.sh "export KUBECONFIG=${script.env.KUBECONFIG}"
            script.sh "helm init"
        }
    }

    static cleanDomain(script, domainName, namespace) {
        try {
            script.sh "kubectl delete jobs --all -n ${namespace} && \
                       kubectl delete services --all -n ${namespace} && \
                       kubectl delete pods --all -n ${namespace}"
        }
        catch (exc) {
            Log.error(script, "Cleanup domain pods and services failed!!")
        }
        finally {
            sleep 10
        }

        try {
            script.sh "kubectl delete configmaps --all -n ${namespace}"
            script.sh "kubectl delete statefulsets --all -n ${namespace}"
        }
        catch (exc) {
            Log.error(script, "Cleanup domain configmap and stateful sets failed!!")
        }
        finally {
            sleep 30
        }

        try {
            script.sh "kubectl delete domain ${domainName} -n ${namespace}"
        }
        catch (exc) {
            Log.error(script, "Cleanup domain resource failed!!")
        }
        finally {
            sleep 30
        }

        try {
            script.sh "kubectl delete pvc ${domainName}-${namespace}-pvc -n ${namespace}"
            sleep 10
            script.sh "kubectl delete pv ${domainName}-${namespace}-pv -n ${namespace}"
        }
        catch (exc) {
            Log.error(script, "Cleanup domain persistent volume failed!!")
        }
        finally {
            sleep 10
        }
    }

    static cleanDomainNamespace(script, namespace) {
        try {
            script.sh "kubectl delete ns ${namespace}"
        }
        catch (exc) {
            Log.error(script, "Cleanup domain namespace failed!!")
        }
    }
}
