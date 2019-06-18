package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Log

class Operator {
    static buildOperator(script,https_proxy) {
        try {
            Log.info(script, "Build soa operator image!!")
            Log.info(script, "Before pwd display!!")
            //script.sh "echo '$REGISTRY_AUTH_USR'"
            //script.sh "echo '$REGISTRY_AUTH_PSW'"
            script.sh "docker login http://container-registry.oracle.com -u '${script.env.REGISTRY_AUTH_USR}' -p '${script.env.REGISTRY_AUTH_PSW}'"
            script.sh "docker pull container-registry.oracle.com/java/serverjre:latest"
            script.sh "docker tag container-registry.oracle.com/java/serverjre:latest store/oracle/serverjre:8"

            script.sh "docker pull fmw-cert-docker.dockerhub-den.oraclecorp.com/soaoperatorpoc/weblogic-kubernetes-operator:2.1"
            script.sh "docker tag fmw-cert-docker.dockerhub-den.oraclecorp.com/soaoperatorpoc/weblogic-kubernetes-operator:2.1 weblogic-kubernetes-operator:2.1"


            script.sh "docker build --build-arg https_proxy=${https_proxy} -t soa-kubernetes-operator:2.1 --no-cache=true ."

            Log.info(script, "Push soa operator image!!!")
            script.sh "docker tag soa-kubernetes-operator:2.1 cisystem.docker.oraclecorp.com/soa-kubernetes-operator:2.1"
            script.sh "docker login cisystem.docker.oraclecorp.com -u '${script.env.REGISTRY_AUTH_USR}' -p '${script.env.REGISTRY_AUTH_PSW}'"
            script.sh "docker push cisystem.docker.oraclecorp.com/soa-kubernetes-operator:2.1"
    }
        catch (exc) {
            Log.error(script, "Build operator failed!!.")
        }
    }

    static deployOperator(script) {
        try {
            Log.info(script, "Deploy operator !!!")
            script.sh "retVal==`echo \\`helm ls ${script.env.SOA_OPERATOR_REL}\\``"

            script.sh "if [[ \$retVal ]]; then\n \
                          helm upgrade --reuse-values --set domainNamespaces=${script.env.SOA_DOMAIN_NS} --wait ${script.env.SOA_OPERATOR_REL} kubernetes/charts/soa-kubernetes-operator\n \
                       else\n \
                          helm install kubernetes/charts/soa-kubernetes-operator --name ${script.env.SOA_OPERATOR_REL} --set image=cisystem.docker.oraclecorp.com/soa-kubernetes-operator:2.1 --namespace ${script.env.SOA_OPERATOR_NS} --set serviceAccount=${script.env.SOA_OPERATOR_SA} --set domainNamespaces={} --wait\n \
                       fi"
            Log.info(script, "Deploy operator Completed!!!")

        }
        catch (exc) {
            Log.error(script, "Deploy operator failed!!.")
        }
    }

    static verifyOperator(script,namespace) {
        try {
            Log.info(script, "Verify soa operator !!!")
            script.sh "kubectl get pods -n ${namespace}"
        }
        catch (exc) {
            Log.error(script, "Verify operator failed!!.")
        }
    }

    static createNamespace(script,namespace) {
        try {
            Log.info(script, "create operator namespace!!")
            script.sh "export KUBECONFIG=${script.env.KUBECONFIG}"
            script.sh "kubectl create ns ${namespace}"
        }
        catch (exc) {
            Log.error(script, "Create Operator namespace failed!!.")
        }
    }

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
}
