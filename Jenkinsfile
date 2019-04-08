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
  }
  stages {
      stage('deploy weblogic operator') {
      steps {
          container('jnlp') {
              git branch: 'master',
              url: 'https://github.com/oracle/weblogic-kubernetes-operator'
              
              sh 'export KUBECONFIG=${KUBECONFIG}'
              
              sh label: 'init helm', script: 
              '''
              helm init
              
              retVal=`echo \\`helm ls wls-operator\\``
              
              if [[ !  -z  "$retVal" ]]; then
               helm delete --purge wls-operator
               sleep 30
              fi
              '''
              
              sh label: 'deploy operator', script: 
              '''
              helm install kubernetes/charts/weblogic-operator \\
                --name wls-operator \\
                --namespace weblogic-operator-ns \\
                --set image=oracle/weblogic-kubernetes-operator:2.0.1 \\
                --set serviceAccount=weblogic-operator-ns \\
                --set "domainNamespaces={}" \\
                --wait
              '''
              
              sh label: 'verify operator', script: 
              '''
              kubectl get pods -n weblogic-operator-ns
              kubectl logs -n weblogic-operator-ns -c weblogic-operator deployments/weblogic-operator
              '''
          }
      }
    }
    stage('build maven test') {
      steps {
          container('jnlp') {
              git branch: 'master',
              credentialsId: 'sandeep.zachariah.ssh',
              url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw_k8s_wlstests.git'
              
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