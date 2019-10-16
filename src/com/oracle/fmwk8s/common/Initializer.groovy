package com.oracle.fmwk8s.common

class Initializer {

    static def initialize(def script) {
        Log.info(script, "Inside the Initializer")
        script.sh "echo Initializing Validation Framework"
        script.sh "touch /logs/jenkinsSample"
        Base.productName = script.env.PRODUCT_NAME
        Log.info(script, Base.productName)
        Log.info(script,"Calling Common constructor")
        Common common = new Common()
        Log.info(script,"Calling UniqueID")
        common.getUniqueId(script)
        Log.info(script,common.runId)
        Log.info(script,Base.runId)

        Log.info(script,"Calling getDomainName")
        common.getDomainName()
        Log.info(script, common.domainName)
        Log.info(script, Base.domainName)

        common.getProductIdentifier()
        Log.info(script, Base.productId)
        Log.info(script, Base.productImage)

        common.getSamplesRepo()
        common.getOperatorVarNames()
        common.gerDomainVarNames()
        common.getNfsPathNames()
        common.getLoadBalancerNames()
    }
}
