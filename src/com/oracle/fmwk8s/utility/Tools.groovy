package com.oracle.fmwk8s.utility

import com.oracle.fmwk8s.common.Log

class Tools {
    static deploySelenium(script, testNamespace) {
        try {
            Log.info("begin deploy selenium.")

            script.git branch: 'master',
                    credentialsId: 'fmwk8sval_ww.ssh',
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh label: "deploy selenium",
                    script: "kubectl apply -n ${testNamespace} -f kubernetes/tools/selenium/"

            Log.info("deploy selenium success.")
        }
        catch (exc) {
            Log.error("deploy selenium failed.")
        }
        finally {
        }
    }
}
