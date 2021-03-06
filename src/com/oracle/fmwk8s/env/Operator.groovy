package com.oracle.fmwk8s.env


import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log

class Operator extends Common {

    static deployOperator() {
        try {
            Log.info("begin deploy kubernetes operator.")

            createNamespace()

            script.git branch: "${operatorBranch}",
                    url: 'https://github.com/oracle/weblogic-kubernetes-operator'

            script.sh label: "get operator helm release",
                    script: "retVal==`echo \\`helm ls ${operatorHelmRelease}\\``"

            if ("${elkEnable}" == "false") {
                script.sh label: "deploy operator without elk",
                        script: "if [[ \$retVal ]]; then\n \
                               helm upgrade --reuse-values --wait ${operatorHelmRelease} kubernetes/charts/weblogic-operator \n \
                           else\n \
                               helm install kubernetes/charts/weblogic-operator --name ${operatorHelmRelease} --namespace ${operatorNamespace} \
                                        --set serviceAccount=${operatorServiceAccount} --set domainNamespaces={} \
                                        --set image=oracle/weblogic-kubernetes-operator:${operatorImageVersion} --wait\n \
                           fi"
            } else {
                Log.info("elk is enabled")
                script.sh label: "deploy operator with elk enabled",
                        script: "if [[ \$retVal ]]; then\n \
                               helm upgrade --reuse-values --wait ${operatorHelmRelease} kubernetes/charts/weblogic-operator \n \
                           else\n \
                               helm install kubernetes/charts/weblogic-operator --name ${operatorHelmRelease} --namespace ${operatorNamespace} \
                                        --set serviceAccount=${operatorServiceAccount} --set domainNamespaces={} \
                                        --set image=oracle/weblogic-kubernetes-operator:${operatorImageVersion} \
                                        --set elkIntegrationEnabled=true --set elasticSearchHost=${elasticSearchHost} --set elasticSearchPort=${elasticSearchPort} --set logStashImage=logstash:6.4.3 --wait\n \
                           fi"
            }

            Log.info("deploy kubernetes operator success.")

            verifyOperator()
        }
        catch (exc) {
            Log.error("deploy kubernetes operator failed.")
            throw exc
        }
    }

    static verifyOperator() {
        try {
            Log.info("begin verify kubernetes operator.")

            if ("${elkEnable}" == "false") {
                script.sh label: "verify operator",
                        script: "kubectl get pods -n ${operatorNamespace} | grep weblogic-operator | grep Running | grep 1/1"
            } else {
                script.sh label: "verify operator",
                        script: "kubectl get pods -n ${operatorNamespace} | grep weblogic-operator | grep Running | grep 2/2"
            }

            Log.info("verify kubernetes operator success.")
        }
        catch (exc) {
            Log.error("verify kubernetes operator failed.")
            throw exc
        }
    }

    static setDomainNamespace() {
        try {
            Log.info("begin set domain namespace.")

            script.sh label: "set operator to manager domain",
                    script: "helm upgrade \
                       --reuse-values \
                       --set \"domainNamespaces={${domainNamespace}}\" \
                       --wait \
                       ${operatorHelmRelease} \
                       kubernetes/charts/weblogic-operator"

            Log.info("set domain namespace success.")
        }
        catch (exc) {
            Log.error("set domain namespace failed.")
            throw exc
        }
    }

    static createNamespace() {
        try {
            Log.info("begin create kubernetes operator namespace.")

            script.sh label: "create operator namespace",
                    script: "kubectl create ns ${operatorNamespace} --v=8"

            Log.info("create kubernetes operator namespace success.")
        }
        catch (exc) {
            Log.error("create kubernetes operator namespace failed.")
            throw exc
        }
    }

    static cleanOperator() {
        try {
            Log.info("begin clean kubernetes operator.")

            script.sh label: "cleanup operator",
                    script: "helm delete --purge ${operatorHelmRelease}"
            script.sh label: "delete helm configmap",
                    script: "kubectl delete cm -n kube-system --selector=NAME=${operatorHelmRelease}"
            script.sh label: "delete operator cluster roles",
                    script: "kubectl delete clusterroles --selector=weblogic.operatorName=${operatorNamespace}"

            Log.info("clean kubernetes operator success.")
        }
        catch (exc) {
            Log.error("clean kubernetes operator failed.")
        }
    }

    static cleanOperatorNamespace() {
        try {
            Log.info("begin clean kubernetes operator namespace.")

            script.sh label: "cleanup operator configmaps",
                    script: "kubectl delete configmaps --all -n ${operatorNamespace} --cascade"
            script.sh label: "cleanup operator namespace",
                    script: "kubectl delete all --all -n ${operatorNamespace} --grace-period=0 --force --cascade"
            sleep 30
            script.sh label: "delete operator namespace",
                    script: "kubectl delete ns ${operatorNamespace} --grace-period=0 --force --cascade"
            sleep 30

            Log.info("clean kubernetes operator namespace success.")
        }
        catch (exc) {
            Log.error("clean kubernetes operator namespace failed.")
        }
        finally {
            try {
                script.sh label: "finalize operator namespace",
                        script: "kubectl get ns ${operatorNamespace} -o json | jq '.spec.finalizers=[]' > ns-without-finalizers.json && \
                       curl -k -X PUT ${k8sMasterUrl}/api/v1/namespaces/${operatorNamespace}/finalize \
                               -H \"Content-Type: application/json\" --data-binary @ns-without-finalizers.json"
            }
            catch (exc) {
            }
        }
    }
}
