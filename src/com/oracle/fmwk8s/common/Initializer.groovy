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
        Log.info(script,Base.runId)

        Log.info(script,"Calling getDomainName")
        common.getDomainName()
        Log.info(script, Base.domainName)


        common.getProductIdentifier()
        Log.info(script, Base.productId)
        Log.info(script, Base.productImage)

        common.getSamplesRepo()
        Log.info(script, Base.samplesRepo)
        Log.info(script, Base.samplesDirectory)

        common.getOperatorVarNames()
        Log.info(script, Base.OPERATOR_NS)
        Log.info(script, Base.OPERATOR_SA)
        Log.info(script, Base.OPERATOR_HELM_RELEASE)

        common.gerDomainVarNames()
        Log.info(script, Base.DOMAIN_NS)
        Log.info(script, Base.WEBLOGIC_USER)
        Log.info(script, Base.ADMIN_PASSWORD)

        common.getNfsPathNames()
        Log.info(script, Base.FMWK8S_NFS_HOME)
        Log.info(script, Base.NFS_DOMAIN_DIR)
        Log.info(script, Base.NFS_DOMAIN_PATH)

        common.getLoadBalancerNames()
        Log.info(script, Base.LB_HELM_RELEASE)
    }
}
