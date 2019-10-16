package com.oracle.fmwk8s.common

class Initializer {

    static def initialize(def script) {
        Log.info(script, "Inside the Initializer")
        script.sh "echo Initializing Validation Framework"
        script.sh "touch /logs/jenkinsSample"
        Base.productName = this.env.PRODUCT_NAME
        Log.info(script, Base.productName)
        Common common = new Common()
        common.getUniqueId(this)
    }
}
