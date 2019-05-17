package com.oracle.fmwk8s.common

class Cleanup {
    static cleanOperator(script, release) {
        try {
            script.sh "helm delete --purge ${release}"
        }
        catch (exc) {
            Log.error(script, "Cleanup operator failed!!.")
        }
    }

    static cleanOperatorNamespace(script, namespace) {
        try {
            script.sh "kubectl delete configmaps --all -n ${namespace}"
            script.sh "kubectl delete all --all -n ${namespace}"
            sleep 10
            script.sh "kubectl delete ns ${namespace}"
        }
        catch (exc) {
            Log.error(script, "Cleanup operator namespace failed!!.")
        }
    }

    static cleanDomain(script, domainName, namespace) {
        try {
            script.sh "kubectl delete jobs --all -n ${namespace}"
            script.sh "kubectl delete services --all -n ${namespace}"
            script.sh "kubectl delete pods --all -n ${namespace}"
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

    static def cleanDatabase(script, namespace) {}
}
