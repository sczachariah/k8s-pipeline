package com.oracle.fmwk8s.test

class Tools {
    static deploySelenium(script, testNamespace) {
        script.git branch: 'master',
                credentialsId: 'sandeep.zachariah.ssh',
                url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

        script.sh "kubectl apply -n ${testNamespace} -f kubernetes/tools/selenium/"
    }
}
