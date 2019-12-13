package com.oracle.fmwk8s.common

class Initializer {
    static def commonObj

    static def initialize(def script) {
        Base.script = script

        Log.info("begin initializing validation framework")
        script.sh "touch /logs/jenkinsSample"

        commonObj = new Common()
        commonObj.getInputVariables()
        commonObj.getUniqueId()
        commonObj.getProductIdentifier()
        commonObj.getDomainVariables()
        commonObj.getOperatorVariables()
        commonObj.getOperatorVersionMappings()
        commonObj.getSamplesRepoDetails()
        commonObj.getLoadBalancerVariables()
        commonObj.getNfsVariables()
        //TODO : uncomment this once kibana is enabled using ingress
//        commonObj.defineKibanaUrl()
        displayInitializedParameterValues()

        Log.info("starting E2E pipeline for " + Base.productName + ' with unique runId ' + Base.runId)
    }

    static def displayInitializedParameterValues() {
        Log.info("Cloud:::" + Base.cloud)
        Log.info("Domain Name:::" + Base.domainName)
        Log.info("Product ID:::" + Base.productId)
        Log.info("Product Image:::" + Base.productImage)
        Log.info("Samples Repo:::" + Base.samplesRepo)
        Log.info("Samples Directory:::" + Base.samplesDirectory)
        Log.info("Operator Namespace:::" + Base.operatorNamespace)
        Log.info("Operator SA::::" + Base.operatorServiceAccount)
        Log.info("Operator Helm Release:::" + Base.operatorHelmRelease)
        Log.info("Domain Namespace:::" + Base.domainNamespace)
        Log.info("Weblogic User:::" + Base.weblogicUsername)
        Log.info("Weblogic Pwd:::" + Base.weblogicPassword)
        Log.info("NFS Home:::" + Base.fmwk8sNfsHome)
        Log.info("NFS Domain Direcory:::" + Base.nfsDomainDir)
        Log.info("NFS Domain Path:::" + Base.nfsDomainPath)
        Log.info("Load Balancer Helm Release:::" + Base.lbHelmRelease)
        Log.info("Hours After:::" + Base.hoursAfter)
        Log.info("Test Type:::" + Base.testType)
        Log.info("Test Target:::" + Base.testTarget)
        Log.info("validation framework initialization completed")
        try {
            Log.info(":::::::::::::" + Base.commonUtility.generatePassword())
        }
        catch (exc) {
            exc.printStackTrace()
        }
    }

    static def initializeCleanupParameterValues(def script) {
        Base.script = script
        Base.operatorNamespace = script.env.DOMAIN_NAME + "-operator-ns-" + script.env.RUN_SUFFIX
        Base.operatorHelmRelease = "op-" + script.env.RUN_SUFFIX
        Base.domainName = script.env.DOMAIN_NAME
        Base.domainNamespace = script.env.DOMAIN_NAME + "-domain-ns-" + script.env.RUN_SUFFIX
        Base.fmwk8sNfsHome = "/scratch/u01/DockerVolume/domains"
        Base.nfsDomainDir = Base.domainNamespace
        Base.nfsDomainPath = Base.fmwk8sNfsHome + "/" + Base.nfsDomainDir
        Base.lbHelmRelease = "lb-" + script.env.RUN_SUFFIX

        Log.info("Operator Namespace:::" + Base.operatorNamespace)
        Log.info("Operator Helm Release:::" + Base.operatorHelmRelease)
        Log.info("Domain Namespace:::" + Base.domainNamespace)
        Log.info("NFS Home:::" + Base.fmwk8sNfsHome)
        Log.info("NFS Domain Direcory:::" + Base.nfsDomainDir)
        Log.info("NFS Domain Path:::" + Base.nfsDomainPath)
        Log.info("Load Balancer Helm Release:::" + Base.lbHelmRelease)
        Log.info("Initialize completed")
    }
}
