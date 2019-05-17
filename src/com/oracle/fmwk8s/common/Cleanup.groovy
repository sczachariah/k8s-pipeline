package com.oracle.fmwk8s.common

class Cleanup {
    static cleanOperator(String release) {
        try {
            sh label: 'clean soa operator', script: '''
                        helm delete --purge ${release}
                        '''
        }
        catch (exc) {
            echo "Cleanup operator failed!!."
        }
    }

    static def cleanDomain(String domainName, String namespace) {
        try {
            sh label: 'clean domain pods and services', script: '''
                        kubectl delete jobs --all -n ${namespace}
                        kubectl delete services --all -n ${namespace}
                        kubectl delete pods --all -n ${namespace}
                        '''
        }
        catch (exc) {
            echo "Cleanup domain pods and services failed!!"
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
            echo "Cleanup domain configmap and stateful sets failed!!"
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
            echo "Cleanup domain resource failed!!"
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
            echo "Cleanup domain persistent volume failed!!"
        }
        finally {
            sleep 10
        }
    }

    static def cleanDatabase(String namespace) {}
}
