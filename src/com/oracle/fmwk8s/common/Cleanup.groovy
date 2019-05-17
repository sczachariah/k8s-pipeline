package com.oracle.fmwk8s.common

class Cleanup {
    static cleanOperator(String release) {
        try {
            sh label: 'clean soa operator', script: '''
                        helm delete --purge ${release}
                        '''
        }
        catch (exc) {
            Log.error("Cleanup operator failed!!.")
        }
    }

    static cleanOperatorNamespace(String namespace) {
        try {
            sh label: 'clean operator namespace', script: '''
                        kubectl delete configmaps --all -n ${namespace}
                        kubectl delete all --all -n ${namespace}
                        sleep 10
                        kubectl delete ns ${namespace}
                        '''
        }
        catch (exc) {
            Log.error("Cleanup operator namespace failed!!.")
        }
    }

    static cleanDomain(String domainName, String namespace) {
        try {
            sh label: 'clean domain pods and services', script: '''
                        kubectl delete jobs --all -n ${namespace}
                        kubectl delete services --all -n ${namespace}
                        kubectl delete pods --all -n ${namespace}
                        '''
        }
        catch (exc) {
            Log.error("Cleanup domain pods and services failed!!")
        }
        finally {
            sleep 10
        }

        try {
            sh label: 'clean domain configmap and stateful sets', script: '''
                        kubectl delete configmaps --all -n ${namespace}
                        kubectl delete statefulsets --all -n ${namespace}
                        '''
        }
        catch (exc) {
            Log.error("Cleanup domain configmap and stateful sets failed!!")
        }
        finally {
            sleep 30
        }

        try {
            sh label: 'clean domain resource', script: '''
                        kubectl delete domain ${domainName} -n ${namespace}
                        '''
        }
        catch (exc) {
            Log.error("Cleanup domain resource failed!!")
        }
        finally {
            sleep 30
        }

        try {
            sh label: 'clean domain persistent volume', script: '''
                        kubectl delete pvc ${domainName}-${namespace}-pvc -n ${namespace}
                        sleep 10
                        kubectl delete pv ${domainName}-${namespace}-pv -n ${namespace}
                        '''
        }
        catch (exc) {
            Log.error("Cleanup domain persistent volume failed!!")
        }
        finally {
            sleep 10
        }
    }

    static cleanDomainNamespace(String namespace) {
        try {
            sh label: 'clean domain namespace', script: '''
                        kubectl delete ns ${namespace}
                        '''
        }
        catch (exc) {
            Log.error("Cleanup domain namespace failed!!")
        }
    }

    static def cleanDatabase(String namespace) {}
}
