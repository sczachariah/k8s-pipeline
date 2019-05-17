package com.oracle.fmwk8s.common

class Cleanup {
    static cleanOperator(script, release) {
        try {
            script.sh label: 'clean soa operator', script: '''
                        helm delete --purge ${release}
                        '''
        }
        catch (exc) {
            Log.error(script, "Cleanup operator failed!!.")
        }
    }

    static cleanOperatorNamespace(script, namespace) {
        try {
            script.sh label: 'clean operator namespace', script: '''
                        kubectl delete configmaps --all -n ${namespace}
                        kubectl delete all --all -n ${namespace}
                        sleep 10
                        kubectl delete ns ${namespace}
                        '''
        }
        catch (exc) {
            Log.error(script, "Cleanup operator namespace failed!!.")
        }
    }

    static cleanDomain(script, domainName, namespace) {
        try {
            script.sh label: 'clean domain pods and services', script: '''
                        kubectl delete jobs --all -n ${namespace}
                        kubectl delete services --all -n ${namespace}
                        kubectl delete pods --all -n ${namespace}
                        '''
        }
        catch (exc) {
            Log.error(script, "Cleanup domain pods and services failed!!")
        }
        finally {
            sleep 10
        }

        try {
            script.sh label: 'clean domain configmap and stateful sets', script: '''
                        kubectl delete configmaps --all -n ${namespace}
                        kubectl delete statefulsets --all -n ${namespace}
                        '''
        }
        catch (exc) {
            Log.error(script, "Cleanup domain configmap and stateful sets failed!!")
        }
        finally {
            sleep 30
        }

        try {
            script.sh label: 'clean domain resource', script: '''
                        kubectl delete domain ${domainName} -n ${namespace}
                        '''
        }
        catch (exc) {
            Log.error(script, "Cleanup domain resource failed!!")
        }
        finally {
            sleep 30
        }

        try {
            script.sh label: 'clean domain persistent volume', script: '''
                        kubectl delete pvc ${domainName}-${namespace}-pvc -n ${namespace}
                        sleep 10
                        kubectl delete pv ${domainName}-${namespace}-pv -n ${namespace}
                        '''
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
            script.sh label: 'clean domain namespace', script: '''
                        kubectl delete ns ${namespace}
                        '''
        }
        catch (exc) {
            Log.error(script, "Cleanup domain namespace failed!!")
        }
    }

    static def cleanDatabase(script, namespace) {}
}
