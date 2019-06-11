package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Log

class Operator {
    static buildOperator(script,docker_username,docker_password,https_proxy,KUBECONFIG,SOA_OPERATOR_NS,SOA_DOMAIN_NS) {
        try {
            Log.info(script, "Build soa operator image!!!")
            withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: 'DockerHub',
                              usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD'],
                             [$class          : 'UsernamePasswordMultiBinding', credentialsId: 'sandeep.zachariah.docker',
                              usernameVariable: 'DOCKER_USERNAME_CISYSTEM', passwordVariable: 'DOCKER_PASSWORD_CISYSTEM']]) {
                Log.info(script, "Build soa operator image11111")

                container('dind') {
                    git branch: 'master',
                            credentialsId: 'sandeep.zachariah.ssh',
                            url: 'git@orahub.oraclecorp.com:tooling/soa-kubernetes-operator.git'

                    script {
                        try {
                            script label: 'create operator namespace', script: '''
                                export KUBECONFIG=${KUBECONFIG}
                                kubectl create ns ${SOA_OPERATOR_NS}
                                #kubectl create sa ${SOA_OPERATOR_SA} -n ${SOA_OPERATOR_NS}
                                '''
                        }
                        catch (exc) {
                        }
                    }

                    script {
                        try {
                            script label: 'create domain namespace', script: '''
                                export KUBECONFIG=${KUBECONFIG}
                                kubectl create ns ${SOA_DOMAIN_NS}
                                '''
                        }
                        catch (exc) {
                        }
                        finally {
                            script label: 'initialize helm', script: '''
                                export KUBECONFIG=${KUBECONFIG}
                                helm init
                                '''
                        }
                    }
                    script.sh "docker login http://container-registry.oracle.com -u ${DOCKER_USERNAME_CISYSTEM} -p ${DOCKER_PASSWORD_CISYSTEM}"

                    script.sh "docker pull container-registry.oracle.com/java/serverjre:latest"
                    script.sh "docker tag container-registry.oracle.com/java/serverjre:latest store/oracle/serverjre:8"

                    script.sh "docker pull fmw-cert-docker.dockerhub-den.oraclecorp.com/soaoperatorpoc/weblogic-kubernetes-operator:2.1"
                    script.sh "docker tag fmw-cert-docker.dockerhub-den.oraclecorp.com/soaoperatorpoc/weblogic-kubernetes-operator:2.1 weblogic-kubernetes-operator:2.1"


                    script.sh "docker build --build-arg https_proxy=${https_proxy} -t soa-kubernetes-operator:2.1 --no-cache=true ."

                    Log.info(script, "Push soa operator image!!!")
                    script.sh "docker tag soa-kubernetes-operator:2.1 cisystem.docker.oraclecorp.com/soa-kubernetes-operator:2.1"
                    script.sh "docker login cisystem.docker.oraclecorp.com -u ${docker_username} -p ${docker_password}"
                    script.sh "docker push cisystem.docker.oraclecorp.com/soa-kubernetes-operator:2.1"
                }
            }
        }
        catch (exc) {
            Log.error(script, "Build operator failed!!.")
        }
    }

    static deployOperator(script,operator_rel,domainns,operatorns,operatorsa) {
        try {
            Log.info(script, "Deploy operator !!!")
            script.sh "retVal=`echo \\`helm ls ${operator_rel}\\``"

            script.sh "if [[ !  -z  "$retVal" ]]; then"
                            "helm upgrade \
				              --reuse-values \
				              --set domainNamespaces={$domainns} \
				              --wait \
				              ${operator_rel} \
				              kubernetes/charts/soa-kubernetes-operator"
                      "else"
                        	"helm install kubernetes/charts/soa-kubernetes-operator \
					             --name ${operator_rel} \
					             --set image=cisystem.docker.oraclecorp.com/soa-kubernetes-operator:2.1 \
					             --namespace ${operatorns} \
					             --set serviceAccount=${operatorsa} \
			    	             --set domainNamespaces={} \
					             --wait"
                      "fi"

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
