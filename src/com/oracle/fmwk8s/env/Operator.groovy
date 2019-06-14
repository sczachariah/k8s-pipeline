package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Log

class Operator {
    static buildOperator(script,REGISTRY_AUTH_USR,REGISTRY_AUTH_PSW,https_proxy) {
        try {
            Log.info(script, "Build soa operator image!!")

            script.sh "docker pull container-registry.oracle.com/java/serverjre:latest"
            script.sh "docker tag container-registry.oracle.com/java/serverjre:latest store/oracle/serverjre:8"

            script.sh "docker pull fmw-cert-docker.dockerhub-den.oraclecorp.com/soaoperatorpoc/weblogic-kubernetes-operator:2.1"
            script.sh "docker tag fmw-cert-docker.dockerhub-den.oraclecorp.com/soaoperatorpoc/weblogic-kubernetes-operator:2.1 weblogic-kubernetes-operator:2.1"


            script.sh "docker build --build-arg https_proxy=${https_proxy} -t soa-kubernetes-operator:2.1 --no-cache=true ."

            Log.info(script, "Push soa operator image!!!")
            script.sh "docker tag soa-kubernetes-operator:2.1 cisystem.docker.oraclecorp.com/soa-kubernetes-operator:2.1"
            //def REGISTRY_AUTH = credentials("sandeep.zachariah.docker")
            //Log.info(script,REGISTRY_AUTH)
            //script.sh "export REGISTRY_AUTH_USR=${REGISTRY_AUTH_USR}"
            //script.sh "export REGISTRY_AUTH_PSW=${REGISTRY_AUTH_PSW}"
            //script.sh "docker login cisystem.docker.oraclecorp.com -u ${REGISTRY_AUTH_USR} -p ${REGISTRY_AUTH_PSW}"
            //script.sh "docker push cisystem.docker.oraclecorp.com/soa-kubernetes-operator:2.1"
        }
        catch (exc) {
            Log.error(script, "Build operator failed!!.")
        }
    }

    static deployOperator(script,operator_rel,domainns,operatorns,operatorsa) {
        try {
            Log.info(script, "Deploy operator !!!")
            script.sh "`echo \\`helm ls ${operator_rel}\\``"
            String retVal = "";
           // System.out.println("Return val is ",retVal);
            if (!retVal?.trim()) {
                Log.info(script, "retVal is empty !!!")
                script.sh "helm install kubernetes/charts/soa-kubernetes-operator --name ${operator_rel} --set image=cisystem.docker.oraclecorp.com/soa-kubernetes-operator:2.1 --namespace ${operatorns} --set serviceAccount=${operatorsa} --set domainNamespaces={} --wait"
            } else {
                Log.info(script, "retVal is Not empty *****************")
                script.sh "helm upgrade --reuse-values --set domainNamespaces={$domainns} --wait ${operator_rel} kubernetes/charts/soa-kubernetes-operator"
            }
            Log.info(script, "Deploy operator Completed!!!")

        }
        catch (exc) {
            Log.error(script, "Deploy operator failed!!.")
        }
    }

    static verifyOperator(script,operatorns) {

    }

    static createNamespace(script,KUBECONFIG,operatorns) {
        try {
            Log.info(script, "create operator namespace!!")
            script.sh "export KUBECONFIG=${KUBECONFIG}"
            script.sh "kubectl create ns ${operatorns}"
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
