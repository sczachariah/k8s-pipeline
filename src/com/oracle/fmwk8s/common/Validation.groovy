package com.oracle.fmwk8s.common

class Validation {

    static def validateInputs(script, operatorVersion, productName) {
        try {
            Log.info(script, "begin validate inputs.")
            if ("${operatorVersion}" == "2.1" && "${productName}" == "WLS-INFRA" ) {
                Log.error(script, "Operator version 2.1 is not applicable for the product ${productName} .")
                System.exit(1)
            }
        }
        catch (exc) {
            Log.error(script, "input validation failed.")
            throw exc
        }

        Log.info(script, "validate inputs success.")
    }
}
