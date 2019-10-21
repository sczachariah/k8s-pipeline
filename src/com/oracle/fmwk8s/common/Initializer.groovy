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
        Log.info(script,"Starting E2E pipeline for " + Base.productName + ' with unique runId ' + Base.runId)

        common.getDomainName()
        common.getProductIdentifier()
        common.getSamplesRepoDetails()
        common.getOperatorVarNames()
        common.gerDomainVarNames()
        common.getNfsPathNames()
        common.getLoadBalancerNames()
        displayInitializedParameterValues(script)

    }

    static def displayInitializedParameterValues(def script){
        Log.info(script, "Domain Name:::"+Base.domainName)
        Log.info(script, "ProductId:::"+Base.productId)
        Log.info(script, "Product image:::"+Base.productImage)
        Log.info(script, "Sample repo:::"+Base.samplesRepo)
        Log.info(script, "Samples Directory:::"+Base.samplesDirectory)
        Log.info(script, "Operator Namespace:::"+Base.OPERATOR_NS)
        Log.info(script, "Operator SA::::"+Base.OPERATOR_SA)
        Log.info(script, "Operator Helm Release:::"+Base.OPERATOR_HELM_RELEASE)
        Log.info(script, "Domain Namespace:::"+Base.DOMAIN_NS)
        Log.info(script, "Weblogic User:::"+Base.WEBLOGIC_USER)
        Log.info(script, "Admin Pwd:::"+Base.ADMIN_PASSWORD)
        Log.info(script, "NFS Home:::"+Base.FMWK8S_NFS_HOME)
        Log.info(script, "NFS Domain Direcory:::"+Base.NFS_DOMAIN_DIR)
        Log.info(script, "NFS Domain Path:::"+Base.NFS_DOMAIN_PATH)
        Log.info(script, "Load balancer Helm Release:::"+Base.LB_HELM_RELEASE)
        Log.info(script, "Initialization completed")
    }
}
