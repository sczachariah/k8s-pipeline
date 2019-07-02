package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Log

class Operator {
    static deployOperator(script, operatorVersion, operatorHelmRelease, operatorNamespace, operatorServiceAccount) {
        try {
            Log.info(script, "begin deploy kubernetes operator.")

            createNamespace(script, operatorNamespace)

            script.git branch: 'release/' + "${operatorVersion}" + '',
                    url: 'https://github.com/oracle/weblogic-kubernetes-operator'

            script.sh "retVal==`echo \\`helm ls ${operatorHelmRelease}\\``"

            script.sh "if [[ \$retVal ]]; then\n \
                          helm upgrade --reuse-values --wait ${operatorHelmRelease} kubernetes/charts/weblogic-operator \n \
                       else\n \
                          helm install kubernetes/charts/weblogic-operator --name ${operatorHelmRelease} --namespace ${operatorNamespace} \
                                        --set serviceAccount=${operatorServiceAccount} --set domainNamespaces={} --wait\n \
                       fi"
            Log.info(script, "deploy kubernetes operator success.")

        }
        catch (exc) {
            Log.error(script, "deploy kubernetes operator failed.")
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
        }
    }

    static setDomainNamespace(script, domainNamespace, operatorHelmRelease) {
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
        }
    }

    static cleanOperator(script, operatorHelmRelease) {
        try {
            Log.info(script, "begin clean kubernetes operator.")
            script.sh "helm delete --purge ${operatorHelmRelease}"
            Log.info(script, "clean kubernetes operator success.")
        }
        catch (exc) {
            Log.info(script, "clean kubernetes operator failed.")
        }
    }

    static cleanOperatorNamespace(script, operatorNamespace) {
        try {
            Log.info(script, "begin clean kubernetes operator namespace.")
            script.sh "kubectl delete configmaps --all -n ${operatorNamespace}"
            script.sh "kubectl delete all --all -n ${operatorNamespace}"
            sleep 10
            script.sh "kubectl delete ns ${operatorNamespace}"
            Log.info(script, "clean kubernetes operator namespace success.")
        }
        catch (exc) {
            Log.info(script, "clean kubernetes operator namespace failed.")
        }
    }
}
