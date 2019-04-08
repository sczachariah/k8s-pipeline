pipeline {
    agent {
        kubernetes {
            label 'fmwk8s-test-infra-slave'
            namespace 'fmwk8s'
            inheritFrom 'fmwk8s-test-infra-slave'
        }
    }
    environment {
        KUBECONFIG = credentials('admin.ci.kubeconfig')
        WLS_DOMAIN_NAME = 'wls-domain1'
    }
    stages {
        stage('deploy weblogic operator') {
            steps {
                container('jnlp') {
                    git branch: 'master',
                            url: 'https://github.com/oracle/weblogic-kubernetes-operator'

                    sh 'export KUBECONFIG=${KUBECONFIG}'

                    sh label: 'init helm', script: '''
                    helm init
              
                    retVal=`echo \\`helm ls wls-operator\\``
              
                    if [[ !  -z  "$retVal" ]]; then
                     helm delete --purge wls-operator
                     sleep 30
                    fi
                    '''

                    sh label: 'deploy operator', script: '''
                    helm install kubernetes/charts/weblogic-operator \
                        --name wls-operator \
                        --namespace weblogic-operator-ns \
                        --set image=oracle/weblogic-kubernetes-operator:2.0.1 \
                        --set serviceAccount=weblogic-operator-ns \
                        --set "domainNamespaces={}" \
                        --wait
                    '''

                    sh label: 'verify operator', script: '''
                    kubectl get pods -n weblogic-operator-ns
                    kubectl logs -n weblogic-operator-ns -c weblogic-operator deployments/weblogic-operator
                    '''
                }
            }
        }

        stage('deploy weblogic domain') {
            steps {
                container('dind') {
                    git branch: 'master',
                            url: 'https://github.com/oracle/weblogic-kubernetes-operator'

                    withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: 'DockerHub',
                                      usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD']]) {
                        sh 'export KUBECONFIG=${KUBECONFIG}'
                        sh 'docker login -u $DOCKER_USERNAME -p DOCKER_PASSWORD'
                    }

                    sh label: 'upgrade helm', script: '''
                    helm upgrade \
                        --reuse-values \
                        --set "domainNamespaces={$WLS_DOMAIN_NAME}" \
                        --wait \
                        wls-operator \
                        kubernetes/charts/weblogic-operator
                    '''

                    sh label: 'set domain secret', script: '''
                    kubernetes/samples/scripts/create-weblogic-domain-credentials/create-weblogic-credentials.sh -u weblogic -p welcome1 -n $WLS_DOMAIN_NAME -d $WLS_DOMAIN_NAME
                    '''

                    sh label: 'prepare domain files', script: '''
                    cd kubernetes/samples/scripts/create-weblogic-domain/domain-home-in-image                  
                    cp create-domain-inputs.yaml create-domain-inputs.yaml.orig

                    sed -i '/domainUID: domain1/c\\domainUID: $WLS_DOMAIN_NAME' create-domain-inputs.yaml                  
                    sed -i '/namespace: default/c\\namespace: $WLS_DOMAIN_NAME' create-domain-inputs.yaml
                    sed -i '/weblogicCredentialsSecretName: domain1-weblogic-credentials/c\\weblogicCredentialsSecretName: $WLS_DOMAIN_NAME-weblogic-credentials' create-domain-inputs.yaml
                    '''

                    sh label: 'create domain', script: '''
                    ./create-domain.sh -u weblogic -p welcome1 -i create-domain-inputs.yaml -o ${WORKSPACE}/weblogic-operator-output-directory

                    cp ${WORKSPACE}/weblogic-operator-output-directory/weblogic-domains/wls-domain1/domain.yaml ${WORKSPACE}
                    cat ${WORKSPACE}/domain.yaml
                  '''
                }
            }
        }

        stage('build maven test') {
            steps {
                container('jnlp') {
                    git branch: 'master',
                            credentialsId: 'sandeep.zachariah.ssh',
                            url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-wlstests.git'

                    sh 'ls -ltr'
                    sh 'mvn clean install'
                }
            }
        }
        stage('run selenium shell') {
            steps {
                container(name: 'jnlp') {
                    sh 'ls -ltr'
                }
            }
        }
    }
}
