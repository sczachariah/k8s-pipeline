package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Log

class Tools {
    static deploySelenium(script, testNamespace) {
        try {
            Log.info(script, "begin deploy selenium.")

            script.git branch: 'master',
                    credentialsId: 'sandeep.zachariah.ssh',
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh "kubectl apply -n ${testNamespace} -f kubernetes/tools/selenium/"

            Log.info(script, "deploy selenium success.")
        }
        catch (exc) {
            Log.error(script, "deploy selenium failed.")
        }
        finally {
        }
    }
}
