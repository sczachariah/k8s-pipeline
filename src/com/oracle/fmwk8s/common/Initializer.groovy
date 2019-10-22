package com.oracle.fmwk8s.common

class Initializer {
    static def commonObj

    static def initialize(def script) {
        Base.script = script

        Log.info(Base.script, "begin initializing validation framework")
        Base.script.sh "touch /logs/jenkinsSample"

        commonObj = new Common()
        commonObj.getInputVariables()
        commonObj.getUniqueId()
        commonObj.getProductIdentifier()
        commonObj.getDomainVariables()
        commonObj.getOperatorVariables()
        commonObj.getSamplesRepoDetails()
        commonObj.getLoadBalancerVariables()
        commonObj.getNfsVariables()
        displayInitializedParameterValues()

        Log.info(Base.script, "starting E2E pipeline for " + Base.productName + ' with unique runId ' + Base.runId)
    }

    static def displayInitializedParameterValues() {
        Log.info(Base.script, "Domain Name:::" + Base.domainName)
        Log.info(Base.script, "ProductId:::" + Base.productId)
        Log.info(Base.script, "Product image:::" + Base.productImage)
        Log.info(Base.script, "Sample repo:::" + Base.samplesRepo)
        Log.info(Base.script, "Samples Directory:::" + Base.samplesDirectory)
        Log.info(Base.script, "Operator Namespace:::" + Base.operatorNamespace)
        Log.info(Base.script, "Operator SA::::" + Base.operatorServiceAccount)
        Log.info(Base.script, "Operator Helm Release:::" + Base.operatorHelmRelease)
        Log.info(Base.script, "Domain Namespace:::" + Base.domainNamespace)
        Log.info(Base.script, "Weblogic User:::" + Base.weblogicUsername)
        Log.info(Base.script, "Weblogic Pwd:::" + Base.weblogicPassword)
        Log.info(Base.script, "NFS Home:::" + Base.fmwk8sNfsHome)
        Log.info(Base.script, "NFS Domain Direcory:::" + Base.nfsDomainDir)
        Log.info(Base.script, "NFS Domain Path:::" + Base.nfsDomainPath)
        Log.info(Base.script, "Load Balancer Helm Release:::" + Base.lbHelmRelease)
        Log.info(Base.script, "validation framework initialization completed")
    }
}
