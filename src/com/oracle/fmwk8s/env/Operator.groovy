package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Log

class Operator {
    static buildOperator(script) {}

    static deployOperator(script) {}

    static verifyOperator(script) {}

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
