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
                    '''

                    sh label: 'deploy operator', script: '''
                    retVal=`echo \\`helm ls wls-operator\\``

                    if [[ !  -z  "$retVal" ]]; then
                        helm upgrade \
                            --reuse-values \
                            --set "domainNamespaces={}" \
                            --wait \
                            wls-operator \
                            kubernetes/charts/weblogic-operator
                    else
                        helm install kubernetes/charts/weblogic-operator \
                            --name wls-operator \
                            --namespace weblogic-operator-ns \
                            --set serviceAccount=weblogic-operator-ns \
                            --set "domainNamespaces={}" \
                            --wait
                    fi
                    '''

                    sh label: 'verify operator', script: '''
                    kubectl get pods -n weblogic-operator-ns
                    '''
                }
            }
        }

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
                         retVal=`echo \\`kubectl get secret $WLS_DOMAIN_NAME-weblogic-credentials -n $WLS_DOMAIN_NAME\\``
                         if [[ "$retVal" == *"not found"* ]]; then
                            kubernetes/samples/scripts/create-weblogic-domain-credentials/create-weblogic-credentials.sh -u weblogic -p welcome1 -n $WLS_DOMAIN_NAME -d $WLS_DOMAIN_NAME
                         fi
                        '''

                        sh label: 'prepare domain files', script: '''
                        cd kubernetes/samples/scripts/create-weblogic-domain/domain-home-in-image                  
                        cp create-domain-inputs.yaml create-domain-inputs.yaml.orig
                        cat create-domain-inputs.yaml

                        sed -i "s#domainUID: domain1#domainUID: $WLS_DOMAIN_NAME#g" create-domain-inputs.yaml                  
                        sed -i "s#namespace: default#namespace: $WLS_DOMAIN_NAME#g" create-domain-inputs.yaml
                        sed -i "s#weblogicCredentialsSecretName: domain1-weblogic-credentials#weblogicCredentialsSecretName: $WLS_DOMAIN_NAME-weblogic-credentials#g" create-domain-inputs.yaml
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
                        
                        sed -i "s#domain-home-in-image:12.2.1.3#cisystem.docker.oraclecorp.com/domain-home-in-image:${WLS_DOMAIN_NAME}#g" ${WORKSPACE}/domain.yaml
                        cat ${WORKSPACE}/domain.yaml
                        kubectl apply -n $WLS_DOMAIN_NAME -f ${WORKSPACE}/domain.yaml
                        
                        kubectl get pods -n $WLS_DOMAIN_NAME
                        kubectl get services -n $WLS_DOMAIN_NAME
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

                    sh 'kubectl apply -n fmwk8s -f kubernetes/tools/selenium/'

                    sh label: 'generate test props', script: '''
                    cat <<EOF > ${WORKSPACE}/test.props
SELENIUM_HUB_HOST=selenium-standalone-firefox.fmwk8s
SELENIUM_HUB_PORT=4444
WLS_ADMIN_HOST=$WLS_DOMAIN_NAME-admin-server.$WLS_DOMAIN_NAME
WLS_ADMIN_PORT=7001
WLS_CLUSTER_HOST=$WLS_DOMAIN_NAME-cluster-cluster-1.$WLS_DOMAIN_NAME
WLS_CLUSTER_PORT=8001
WLS_ADMIN_USERNAME=weblogic
WLS_ADMIN_PASSWORD=welcome1
EOF
                    
                    cat ${WORKSPACE}/test.props
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
//                    sh 'mvn clean test -Dmyproperty=${WORKSPACE}/test.props'
                }
            }
        }
    }

//    post {
//        always {
//            container(name: 'jnlp') {
//                sh label: 'clean weblogic domain', script: '''
//                kubectl delete pods --all -n $WLS_DOMAIN_NAME
//                kubectl delete jobs --all -n $WLS_DOMAIN_NAME
//                kubectl delete secret $WLS_DOMAIN_NAME-weblogic-credentials -n $WLS_DOMAIN_NAME
//                '''
//
//                sh label: 'clean weblogic operator', script: '''
//                retVal=`echo \\`helm ls wls-operator\\``
//                if [[ !  -z  "$retVal" ]]; then
//                    helm delete --purge wls-operator
//                fi
//                '''
//            }
//        }
//    }
}
