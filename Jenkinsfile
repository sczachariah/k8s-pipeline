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
//        stage('deploy weblogic operator') {
//            steps {
//                container('jnlp') {
//                    git branch: 'master',
//                            url: 'https://github.com/oracle/weblogic-kubernetes-operator'
//
//                    sh 'export KUBECONFIG=${KUBECONFIG}'
//
//                    sh label: 'init helm', script: '''
//                    helm init
//
//                    retVal=`echo \\`helm ls wls-operator\\``
//
//                    if [[ !  -z  "$retVal" ]]; then
//                     helm delete --purge wls-operator
//                     sleep 120
//                    fi
//                    '''
//
//                    sh label: 'deploy operator', script: '''
//                    helm install kubernetes/charts/weblogic-operator \
//                        --name wls-operator \
//                        --namespace weblogic-operator-ns \
//                        --set image=oracle/weblogic-kubernetes-operator:2.0.1 \
//                        --set serviceAccount=weblogic-operator-ns \
//                        --set "domainNamespaces={}" \
//                        --wait
//                    '''
//
//                    sh label: 'verify operator', script: '''
//                    kubectl get pods -n weblogic-operator-ns
//                    kubectl logs -n weblogic-operator-ns -c weblogic-operator deployments/weblogic-operator
//                    '''
//                }
//            }
//        }

        stage('deploy weblogic domain') {
            steps {
                withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: 'DockerHub',
                                  usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD'],
                                 [$class          : 'UsernamePasswordMultiBinding', credentialsId: 'sandeep.zachariah.docker',
                                  usernameVariable: 'DOCKER_USERNAME_CISYSTEM', passwordVariable: 'DOCKER_PASSWORD_CISYSTEM']]) {
                    container('dind') {
                        git branch: 'master',
                                url: 'https://github.com/oracle/weblogic-kubernetes-operator'

                        sh label: 'setup env', script: '''
                        export KUBECONFIG=${KUBECONFIG}
                        '''

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
                        cat create-domain-inputs.yaml

                        sed -i '/domainUID: domain1/c\\domainUID: $WLS_DOMAIN_NAME' create-domain-inputs.yaml                  
                        sed -i '/namespace: default/c\\namespace: $WLS_DOMAIN_NAME' create-domain-inputs.yaml
                        sed -i '/weblogicCredentialsSecretName: domain1-weblogic-credentials/c\\weblogicCredentialsSecretName: $WLS_DOMAIN_NAME-weblogic-credentials' create-domain-inputs.yaml
                        '''

                        sh label: 'create domain', script: '''
                        docker login -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD}
                        cd kubernetes/samples/scripts/create-weblogic-domain/domain-home-in-image
                        ./create-domain.sh -u weblogic -p welcome1 -i create-domain-inputs.yaml -o ${WORKSPACE}/weblogic-operator-output-directory

                        cp ${WORKSPACE}/weblogic-operator-output-directory/weblogic-domains/wls-domain1/domain.yaml ${WORKSPACE}
                        cat ${WORKSPACE}/domain.yaml
                    
                        docker images
                        docker tag domain-home-in-image:12.2.1.3 cisystem.docker.oraclecorp.com/domain-home-in-image:$WLS_DOMAIN_NAME
                        docker login cisystem.docker.oraclecorp.com -u ${DOCKER_USERNAME_CISYSTEM} -p ${DOCKER_PASSWORD_CISYSTEM}
                        docker push cisystem.docker.oraclecorp.com/domain-home-in-image:$WLS_DOMAIN_NAME
                        
                        sed -i '/image: \"domain-home-in-image:12.2.1.3\"/c\\  image: \"cisystem.docker.oraclecorp.com/domain-home-in-image:$WLS_DOMAIN_NAME\"' ${WORKSPACE}/domain.yaml
                        cat ${WORKSPACE}/domain.yaml
                        kubectl apply -f ${WORKSPACE}/domain.yaml
                        '''
                    }
                }
            }
        }

        stage('deploy tools') {
            steps {
                container(name: 'jnlp') {
                    git branch: 'master',
                            credentialsId: 'sandeep.zachariah.ssh',
                            url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

                    sh 'kubectl apply -f kubernetes/tools/selenium/'
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
//                    sh 'mvn clean install'
                }
            }
        }
    }

    post {
        always {
            container(name: 'jnlp') {
                echo '****Cleanup****'
                sh 'kubectl delete secret $WLS_DOMAIN_NAME-weblogic-credentials -n $WLS_DOMAIN_NAME'
                sh 'ls -ltr'
            }
        }
    }
}
