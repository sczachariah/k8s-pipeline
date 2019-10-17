package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Base
import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log

class Operator extends Base{
    static def operatorNamespace
    static def operatorServiceAccount
    static def operatorHelmRelease

    static deployOperator(script) {
        Common.getOperatorVersions(script.env.OPERATOR_VERSION)
        try {
            Log.info(script, "begin deploy kubernetes operator.")
            Log.info(script, script.env.ELK_ENABLE)

            createNamespace(script, Base.OPERATOR_NS)
            this.operatorNamespace = Base.OPERATOR_NS
            this.operatorServiceAccount = Base.OPERATOR_SA
            this.operatorHelmRelease = Base.OPERATOR_HELM_RELEASE

            script.git branch: "${Base.operatorBranch}",
                    url: 'https://github.com/oracle/weblogic-kubernetes-operator'

            script.sh "retVal==`echo \\`helm ls ${Base.OPERATOR_HELM_RELEASE}\\``"

            if ("${script.env.ELK_ENABLE}" == "false") {
                script.sh "if [[ \$retVal ]]; then\n \
                               helm upgrade --reuse-values --wait ${Base.OPERATOR_HELM_RELEASE} kubernetes/charts/weblogic-operator \n \
                           else\n \
                               helm install kubernetes/charts/weblogic-operator --name ${Base.OPERATOR_HELM_RELEASE} --namespace ${Base.OPERATOR_NS} \
                                        --set serviceAccount=${Base.OPERATOR_SA} --set domainNamespaces={} \
                                        --set image=oracle/weblogic-kubernetes-operator:${Base.operatorImageVersion} --wait\n \
                           fi"
            } else {
                Log.info(script, "elk is enabled")
                script.sh "if [[ \$retVal ]]; then\n \
                               helm upgrade --reuse-values --wait ${Base.OPERATOR_HELM_RELEASE} kubernetes/charts/weblogic-operator \n \
                           else\n \
                               helm install kubernetes/charts/weblogic-operator --name ${Base.OPERATOR_HELM_RELEASE} --namespace ${Base.OPERATOR_NS} \
                                        --set serviceAccount=${Base.OPERATOR_SA} --set domainNamespaces={} \
                                        --set image=oracle/weblogic-kubernetes-operator:${Base.operatorImageVersion} \
                                        --set elkIntegrationEnabled=true --set elasticSearchHost=${Base.elasticSearchHost} --set elasticSearchPort=${Base.elasticSearchPort} --set logStashImage=logstash:6.4.3 --wait\n \
                           fi"
            }

            Log.info(script, "deploy kubernetes operator success.")

            verifyOperator(script, Base.OPERATOR_NS, script.env.ELK_ENABLE)
        }
        catch (exc) {
            Log.error(script, "deploy kubernetes operator failed.")
            throw exc
        }
    }

    static verifyOperator(script) {
        try {
            Log.info(script, "begin verify kubernetes operator.")

            if ("${script.env.ELK_ENABLE}" == "false") {
                script.sh "kubectl get pods -n ${Base.OPERATOR_NS} | grep weblogic-operator | grep Running | grep 1/1"
            } else {
                script.sh "kubectl get pods -n ${Base.OPERATOR_NS} | grep weblogic-operator | grep Running | grep 2/2"
            }

            Log.info(script, "verify kubernetes operator success.")
        }
        catch (exc) {
            Log.error(script, "verify kubernetes operator failed.")
            throw exc
        }
    }

    static setDomainNamespace(script) {
        try {
            Log.info(script, "begin set domain namespace.")

            script.sh "helm upgrade \
                       --reuse-values \
                       --set \"domainNamespaces={$Base.DOMAIN_NS}\" \
                       --wait \
                       ${Base.OPERATOR_HELM_RELEASE} \
                       kubernetes/charts/weblogic-operator"

            Log.info(script, "set domain namespace success.")
        }
        catch (exc) {
            Log.error(script, "set domain namespace failed.")
            throw exc
        }
    }

    static createNamespace(script) {
        try {
            Log.info(script, "begin create kubernetes operator namespace.")

            script.sh "kubectl create ns ${Base.OPERATOR_NS} --v=8"

            Log.info(script, "create kubernetes operator namespace success.")
        }
        catch (exc) {
            Log.error(script, "create kubernetes operator namespace failed.")
            throw exc
        }
    }

    static cleanOperator(script) {
        try {
            Log.info(script, "begin clean kubernetes operator.")

            script.sh "helm delete --purge ${Base.OPERATOR_HELM_RELEASE}"

            Log.info(script, "clean kubernetes operator success.")
        }
        catch (exc) {
            Log.error(script, "clean kubernetes operator failed.")
        }
    }

    static cleanOperatorNamespace(script) {
        try {
            Log.info(script, "begin clean kubernetes operator namespace.")

            script.sh "kubectl delete configmaps --all -n ${Base.OPERATOR_NS}"
            script.sh "kubectl delete all --all -n ${Base.OPERATOR_NS}"
            sleep 30
            script.sh "kubectl delete ns ${Base.OPERATOR_NS}"
            sleep 30

            Log.info(script, "clean kubernetes operator namespace success.")
        }
        catch (exc) {
            Log.error(script, "clean kubernetes operator namespace failed.")
        }
        finally {
            try {
                script.sh "kubectl get ns ${Base.OPERATOR_NS} -o json | jq '.spec.finalizers=[]' > ns-without-finalizers.json && \
                       curl -k -X PUT ${Common.k8sMasterUrl}/api/v1/namespaces/${Base.OPERATOR_NS}/finalize \
                               -H \"Content-Type: application/json\" --data-binary @ns-without-finalizers.json"

            }
            catch (exc) {
            }
        }
    }
}
