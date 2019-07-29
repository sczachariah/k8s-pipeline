package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log

class Operator {
    static def operatorNamespace
    static def operatorServiceAccount
    static def operatorHelmRelease

    static deployOperator(script, operatorVersion, operatorHelmRelease, operatorNamespace, operatorServiceAccount, elkEnable) {
        Common.getOperatorVersions(operatorVersion)
        try {
            Log.info(script, "begin deploy kubernetes operator.")
            Log.info(script, elkEnable)

            createNamespace(script, operatorNamespace)
            this.operatorNamespace = operatorNamespace
            this.operatorServiceAccount = operatorServiceAccount
            this.operatorHelmRelease = operatorHelmRelease

            script.git branch: "${Common.operatorBranch}",
                    url: 'https://github.com/oracle/weblogic-kubernetes-operator'

            script.sh "retVal==`echo \\`helm ls ${operatorHelmRelease}\\``"



            script.sh "if [[ ${elkEnable} == \"FALSE\" ]]; then\n \
                           if [[ \$retVal ]]; then\n \
                               helm upgrade --reuse-values --wait ${operatorHelmRelease} kubernetes/charts/weblogic-operator \n \
                           else\n \
                               helm install kubernetes/charts/weblogic-operator --name ${operatorHelmRelease} --namespace ${operatorNamespace} \
                                        --set serviceAccount=${operatorServiceAccount} --set domainNamespaces={} \
                                        --set image=oracle/weblogic-kubernetes-operator:${Common.operatorImageVersion} --wait\n \
                           fi \n \
                       else \n \
                           echo \"True elkEnable\" \n \
                           if [[ \$retVal ]]; then\n \
                               helm upgrade --reuse-values --wait ${operatorHelmRelease} kubernetes/charts/weblogic-operator \n \
                           else\n \
                               helm install kubernetes/charts/weblogic-operator --name ${operatorHelmRelease} --namespace ${operatorNamespace} \
                                        --set serviceAccount=${operatorServiceAccount} --set domainNamespaces={} \
                                        --set image=oracle/weblogic-kubernetes-operator:${Common.operatorImageVersion} \
                                        --set elkIntegrationEnabled=true --set elasticSearchHost=elasticsearch.logging.svc.cluster.local --set elasticSearchPort=9200 --wait\n \
                           fi \n \
                       fi"


            Log.info(script, "deploy kubernetes operator success.")
        }
        catch (exc) {
            Log.error(script, "deploy kubernetes operator failed.")
            throw exc
        }
    }

    static verifyOperator(script, operatorNamespace) {
        try {
            Log.info(script, "begin verify kubernetes operator.")

            script.sh "kubectl get pods -n ${operatorNamespace} | grep weblogic-operator | grep Running | grep 1/1"

            Log.info(script, "verify kubernetes operator success.")
        }
        catch (exc) {
            Log.error(script, "verify kubernetes operator failed.")
            throw exc
        }
    }

    static setDomainNamespace(script, operatorHelmRelease, domainNamespace) {
        try {
            Log.info(script, "begin set domain namespace.")

            script.sh "export KUBECONFIG=${script.env.KUBECONFIG}"
            script.sh "helm upgrade \
                       --reuse-values \
                       --set \"domainNamespaces={$domainNamespace}\" \
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

    static createNamespace(script, namespace) {
        try {
            Log.info(script, "begin create kubernetes operator namespace.")

            script.sh "export KUBECONFIG=${script.env.KUBECONFIG}"
            script.sh "kubectl create ns ${namespace}"

            Log.info(script, "create kubernetes operator namespace success.")
        }
        catch (exc) {
            Log.error(script, "create kubernetes operator namespace failed.")
            throw exc
        }
    }

    static cleanOperator(script, operatorHelmRelease) {
        try {
            Log.info(script, "begin clean kubernetes operator.")

            script.sh "helm delete --purge ${operatorHelmRelease}"

            Log.info(script, "clean kubernetes operator success.")
        }
        catch (exc) {
            Log.error(script, "clean kubernetes operator failed.")
        }
    }

    static cleanOperatorNamespace(script, operatorNamespace) {
        try {
            Log.info(script, "begin clean kubernetes operator namespace.")

            script.sh "kubectl delete configmaps --all -n ${operatorNamespace}"
            script.sh "kubectl delete all --all -n ${operatorNamespace}"
            sleep 30
            script.sh "kubectl delete ns ${operatorNamespace}"
            sleep 30
            script.sh "kubectl get ns ${operatorNamespace} -o json | jq '.spec.finalizers=[]' > ns-without-finalizers.json && \
                       curl -k -X PUT https://fmwk8s.us.oracle.com:6443/api/v1/namespaces/${operatorNamespace}/finalize \
                               -H \"Content-Type: application/json\" --data-binary @ns-without-finalizers.json"

            Log.info(script, "clean kubernetes operator namespace success.")
        }
        catch (exc) {
            Log.error(script, "clean kubernetes operator namespace failed.")
        }
    }
}
