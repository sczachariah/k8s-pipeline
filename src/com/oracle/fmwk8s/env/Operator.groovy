package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Base
import com.oracle.fmwk8s.common.Log

class Operator extends Base {

    static deployOperator() {
        getOperatorVersionMappings()

        try {
            Log.info(script, "begin deploy kubernetes operator.")

            createNamespace()

            script.git branch: "${operatorBranch}",
                    url: 'https://github.com/oracle/weblogic-kubernetes-operator'

            script.sh "retVal==`echo \\`helm ls ${operatorHelmRelease}\\``"

            if ("${elkEnable}" == "false") {
                script.sh "if [[ \$retVal ]]; then\n \
                               helm upgrade --reuse-values --wait ${operatorHelmRelease} kubernetes/charts/weblogic-operator \n \
                           else\n \
                               helm install kubernetes/charts/weblogic-operator --name ${operatorHelmRelease} --namespace ${operatorNamespace} \
                                        --set serviceAccount=${operatorServiceAccount} --set domainNamespaces={} \
                                        --set image=oracle/weblogic-kubernetes-operator:${operatorImageVersion} --wait\n \
                           fi"
            } else {
                Log.info(script, "elk is enabled")
                script.sh "if [[ \$retVal ]]; then\n \
                               helm upgrade --reuse-values --wait ${operatorHelmRelease} kubernetes/charts/weblogic-operator \n \
                           else\n \
                               helm install kubernetes/charts/weblogic-operator --name ${operatorHelmRelease} --namespace ${operatorNamespace} \
                                        --set serviceAccount=${operatorServiceAccount} --set domainNamespaces={} \
                                        --set image=oracle/weblogic-kubernetes-operator:${operatorImageVersion} \
                                        --set elkIntegrationEnabled=true --set elasticSearchHost=${elasticSearchHost} --set elasticSearchPort=${elasticSearchPort} --set logStashImage=logstash:6.4.3 --wait\n \
                           fi"
            }

            Log.info(script, "deploy kubernetes operator success.")

            verifyOperator()
        }
        catch (exc) {
            Log.error(script, "deploy kubernetes operator failed.")
            throw exc
        }
    }

    static verifyOperator() {
        try {
            Log.info(script, "begin verify kubernetes operator.")

            if ("${elkEnable}" == "false") {
                script.sh "kubectl get pods -n ${operatorNamespace} | grep weblogic-operator | grep Running | grep 1/1"
            } else {
                script.sh "kubectl get pods -n ${operatorNamespace} | grep weblogic-operator | grep Running | grep 2/2"
            }

            Log.info(script, "verify kubernetes operator success.")
        }
        catch (exc) {
            Log.error(script, "verify kubernetes operator failed.")
            throw exc
        }
    }

    static setDomainNamespace() {
        try {
            Log.info(script, "begin set domain namespace.")

            script.sh "helm upgrade \
                       --reuse-values \
                       --set \"domainNamespaces={${domainNamespace}}\" \
                       --wait \
                       ${operatorHelmRelease} \
                       kubernetes/charts/weblogic-operator"

            Log.info(script, "set domain namespace success.")
        }
        catch (exc) {
            Log.error(script, "set domain namespace failed.")
            throw exc
        }
    }

    static createNamespace() {
        try {
            Log.info(script, "begin create kubernetes operator namespace.")

            script.sh "kubectl create ns ${operatorNamespace} --v=8"

            Log.info(script, "create kubernetes operator namespace success.")
        }
        catch (exc) {
            Log.error(script, "create kubernetes operator namespace failed.")
            throw exc
        }
    }

    static cleanOperator() {
        try {
            Log.info(script, "begin clean kubernetes operator.")

            script.sh "helm delete --purge ${operatorHelmRelease}"

            Log.info(script, "clean kubernetes operator success.")
        }
        catch (exc) {
            Log.error(script, "clean kubernetes operator failed.")
        }
    }

    static cleanOperatorNamespace() {
        try {
            Log.info(script, "begin clean kubernetes operator namespace.")

            script.sh "kubectl delete configmaps --all -n ${operatorNamespace}"
            script.sh "kubectl delete all --all -n ${operatorNamespace}"
            sleep 30
            script.sh "kubectl delete ns ${operatorNamespace}"
            sleep 30

            Log.info(script, "clean kubernetes operator namespace success.")
        }
        catch (exc) {
            Log.error(script, "clean kubernetes operator namespace failed.")
        }
        finally {
            try {
                script.sh "kubectl get ns ${operatorNamespace} -o json | jq '.spec.finalizers=[]' > ns-without-finalizers.json && \
                       curl -k -X PUT ${k8sMasterUrl}/api/v1/namespaces/${operatorNamespace}/finalize \
                               -H \"Content-Type: application/json\" --data-binary @ns-without-finalizers.json"
            }
            catch (exc) {
            }
        }
    }
}
