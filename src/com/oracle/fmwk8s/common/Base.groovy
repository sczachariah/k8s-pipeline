package com.oracle.fmwk8s.common

import com.oracle.fmwk8s.utility.K8sUtility
import com.oracle.fmwk8s.utility.YamlUtility

class Base {
    static def script
    static def yamlUtility = new YamlUtility()
    static def k8sUtility = new K8sUtility()

    static def cloud
    static def k8sMasterUrl = ""
    static def k8sMasterIP = ""

    static def runId
    static def registrySecret = "regcred"
    static def denRegistrySecret = "denregcred"
    static def registryAuthUsr
    static def registryAuthPsw

    static def operatorVersion
    static def operatorBranch
    static def operatorImageVersion
    static def operatorNamespace
    static def operatorServiceAccount
    static def operatorHelmRelease

    static def productId
    static def productName
    static def productImage

    static def testImage
    static def testType
    static def hoursAfter
    static def hoursAfterSeconds

    static def domainType
    static def domainName
    static def domainNamespace
    static def weblogicUsername
    static def weblogicPassword

    static def databaseVersion

    static def samplesRepo
    static def samplesBranch
    static def samplesDirectory

    static def fmwk8sNfsHome
    static def nfsDomainDir
    static def nfsDomainPath

    static def lbType
    static def lbHelmRelease

    static def elkEnable
    static def elasticSearchHost = "elasticsearch-master.logging"
    static def elasticSearchPort = "9200"

    static def kibanaUrl = "http://fmwk8s-stage.us.oracle.com:30444/app/kibana"

    static def emailRecipients

    static getInputVariables() {
        cloud = script.env.CLOUD
        registryAuthUsr = script.env.REGISTRY_AUTH_USR
        registryAuthPsw = script.env.REGISTRY_AUTH_PSW
        operatorVersion = script.env.OPERATOR_VERSION
        productName = script.env.PRODUCT_NAME
        domainType = script.env.DOMAIN_TYPE
        productImage = script.env.PRODUCT_IMAGE_TAG
        databaseVersion = script.env.DATABASE_VERSION
        lbType = script.env.K8S_LOADBALANCER
        testImage = script.env.TEST_IMAGE_TAG
        testType = script.env.TEST_TYPE
        elkEnable = script.env.ELK_ENABLE
        hoursAfter = Long.valueOf(script.env.HOURS_AFTER)
        hoursAfterSeconds = hoursAfter * 60 * 60
        emailRecipients = script.env.EMAIL_TO
    }

    static getDomainVariables() {
        if (!Mapping.domainNameMap.containsKey(productName)) {
            domainName = "unknown"
        }else {
            domainName = Mapping.domainNameMap.get(productName)
        }
        domainNamespace = "${domainName}-domain-ns-${runId}"
        weblogicUsername = 'weblogic'
        weblogicPassword = 'Welcome1'
    }

    static getOperatorVariables() {
        operatorNamespace = "${domainName}-operator-ns-${runId}"
        operatorServiceAccount = 'default'
        operatorHelmRelease = "op-${runId}"
    }

    static getDatabaseVariables() {
    }

    static getNfsVariables() {
        fmwk8sNfsHome = "/scratch/u01/DockerVolume/domains"
        nfsDomainDir = "${domainNamespace}"
        nfsDomainPath = "${fmwk8sNfsHome}/${nfsDomainDir}"
    }

    static getLoadBalancerVariables() {
        lbHelmRelease = "lb-${runId}"
    }

    static getOperatorVersionMappings() {
        samplesRepo = "https://github.com/oracle/weblogic-kubernetes-operator"
        samplesDirectory = "domain-home-on-pv"

        switch ("${operatorVersion}") {
            case "2.3.0":
                operatorBranch = "release/2.3.0"
                operatorImageVersion = "2.3.0"
                samplesBranch = "release/2.3.0"
                switch (productName) {
                    case "SOA":
                        samplesRepo = "https://github.com/sbattagi/weblogic-kubernetes-operator"
                        samplesBranch = "soa-2.3.0"
                }
                break
            case "2.4.0":
                operatorBranch = "release/2.4.0"
                operatorImageVersion = "2.4.0"
                samplesBranch = "release/2.4.0"
                break
            default:
                operatorBranch = "develop"
                operatorImageVersion = "develop"
                samplesBranch = "develop"
                break
        }
    }

    static getProductIdentifier() {
        if (!Mapping.productIdMap.containsKey(productName)) {
            productId = "unknown"
        }else {
            productId = Mapping.productIdMap.get(productName)
        }
    }

    static getSamplesRepoDetails() {
        switch ("${productName}") {
            case "WCP":
                samplesRepo = "git@orahub.oraclecorp.com:tooling/wcp-kubernetes-operator.git"
                samplesBranch = "PS3"
                samplesDirectory = ""
                break
            case "OIG":
                samplesRepo = "git@orahub.oraclecorp.com:idm/oim-kubernetes-operator.git"
                samplesDirectory = ""
                break
        }
    }
}
