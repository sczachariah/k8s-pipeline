package com.oracle.fmwk8s.common

class Validation {

    static def validateInputs(script, operatorVersion, productName, domainType) {
        try {
            Log.info(script, "begin validate inputs.")
            if ("${operatorVersion}" == "2.1" && "${productName}" == "WLS-INFRA" ) {
                Log.error(script, "Operator version 2.1 is not applicable for the product ${productName} .")
                throw new Exception("input validation for operator failed")
            }
            if ("${domainType}" == "N/A" && "${productName}" == "SOA" ) {
                Log.error(script, "Operator version 2.1 is not applicable for the product ${productName} .")
                throw new Exception("domain type validation failed")
            }
        }
        catch (exc) {
            Log.error(script, "input validation failed.")
            throw exc
        }

        Log.info(script, "validate inputs success.")
    }
}
