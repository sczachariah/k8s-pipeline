package com.oracle.fmwk8s.common

class Initializer {

    static def initialize(def script) {
        Log.info(script, "Inside the Initializer")
        script.sh "echo Initializing Validation Framework"
        script.sh "touch /logs/jenkinsSample"
        Base.productName = script.env.PRODUCT_NAME
        Log.info(script, Base.productName)
        Common common = new Common()
        Log.info(script,"Calling UniqueID")
        common.getUniqueId(script)
    }
}
