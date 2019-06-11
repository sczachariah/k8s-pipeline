package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Log

class Operator {
    static buildOperator(script,docker_username,docker_password) {
        try {
            sh label: 'build soa operator image', script: '''							
            docker login http://container-registry.oracle.com -u ${docker_username} -p ${docker_password}
			
			docker pull container-registry.oracle.com/java/serverjre:latest
			docker tag container-registry.oracle.com/java/serverjre:latest store/oracle/serverjre:8
						
			docker pull fmw-cert-docker.dockerhub-den.oraclecorp.com/soaoperatorpoc/weblogic-kubernetes-operator:2.1
			docker tag fmw-cert-docker.dockerhub-den.oraclecorp.com/soaoperatorpoc/weblogic-kubernetes-operator:2.1 weblogic-kubernetes-operator:2.1
						

			docker build --build-arg https_proxy=$https_proxy -t soa-kubernetes-operator:2.1 --no-cache=true .
			'''

            sh label: 'push soa operator image', script: '''
			docker tag soa-kubernetes-operator:2.1 cisystem.docker.oraclecorp.com/soa-kubernetes-operator:2.1
			docker login cisystem.docker.oraclecorp.com -u ${docker_username} -p ${docker_password}
			docker push cisystem.docker.oraclecorp.com/soa-kubernetes-operator:2.1
            '''
        }
        catch (exc) {
            Log.error(script, "Build operator failed!!.")
        }
    }

    static deployOperator(script,operator_rel,domainns,operatorns,operatorsa) {
        try {
            sh label: 'deploy operator', script: '''
			retVal=`echo \\`helm ls ${operator_rel}\\``

			if [[ !  -z  "$retVal" ]]; then
			    helm upgrade \
				   --reuse-values \
				   --set "domainNamespaces={$domainns}" \
				   --wait \
				   ${SOA_OPERATOR_REL} \
				   kubernetes/charts/soa-kubernetes-operator
			else
				helm install kubernetes/charts/soa-kubernetes-operator \
					--name ${operator_rel} \
					--set image=cisystem.docker.oraclecorp.com/soa-kubernetes-operator:2.1 \
					--namespace ${operatorns} \
					--set serviceAccount=${operatorsa} \
			    	--set "domainNamespaces={}" \
					--wait
			fi
			'''
        }
        catch (exc) {
            Log.error(script, "Deploy operator failed!!.")
        }
    }

    static verifyOperator(script,operatorns) {

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
