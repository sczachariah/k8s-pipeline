package com.oracle.fmwk8s.common

import com.oracle.fmwk8s.utility.K8sUtility
import com.oracle.fmwk8s.utility.YamlUtility

class Base {
    static def script
    static def yamlUtility = new YamlUtility()
    static def K8sUtility = new K8sUtility()

    static def k8sMasterUrl = ""
    static def k8sMasterIP = "100.111.149.15"

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
    static def elasticSearchHost = "elasticsearch.logging.svc.cluster.local"
    static def elasticSearchPort = "9200"

    static getInputVariables() {
        registryAuthUsr = script.env.REGISTRY_AUTH_USR
        registryAuthPsw = script.env.REGISTRY_AUTH_PSW
        operatorVersion = script.env.OPERATOR_VERSION
        productName = script.env.PRODUCT_NAME
        domainType = script.env.DOMAIN_TYPE
        databaseVersion = script.env.DATABASE_VERSION
        lbType = script.env.K8S_LOADBALANCER
        testImage = script.env.TEST_IMAGE_TAG
        testType = script.env.TEST_TYPE
        elkEnable = script.env.ELK_ENABLE
        hoursAfter = script.env.HOURS_AFTER
    }

    static getDomainVariables() {
        switch ("${productName}") {
            case "WLS":
                domainName = "weblogic"
                break
            case "WLS-INFRA":
                domainName = "wlsinfra"
                break
            case "SOA":
                domainName = "soainfra"
                break
            case "OIG":
                domainName = "oim"
            default:
                domainName = "unknown"
                break
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
        switch ("${operatorVersion}") {
            case "2.1":
                operatorBranch = "release/2.1"
                operatorImageVersion = "2.1"
                samplesBranch = "release/2.1"
                break
            case "2.2":
                operatorBranch = "release/2.2"
                operatorImageVersion = "2.2.0"
                samplesBranch = "release/2.2"
                break
            case "2.2.1":
                operatorBranch = "release/2.2.1"
                operatorImageVersion = "2.2.1"
                samplesBranch = "release/2.2.1"
                switch (productName) {
                    case "SOA":
                        samplesBranch = "soa-2.2.1-dev"
                }
                break
            case "2.3.0":
                operatorBranch = "release/2.3.0"
                operatorImageVersion = "2.3.0"
                samplesBranch = "release/2.3.0"
                switch (productName) {
                    case "SOA":
                        samplesBranch = "soa-2.2.1-dev"
                }
                break
            default:
                operatorBranch = "develop"
                operatorImageVersion = "develop"
                samplesBranch = "develop"
                break
        }
        getSamplesRepoDetails()
    }

    static getProductIdentifier() {
        switch ("${productName}") {
            case "WLS":
                productId = "weblogic"
                productImage = "container-registry.oracle.com/middleware/weblogic:12.2.1.3"
                break
            case "WLS-INFRA":
                productId = "fmw-infrastructure"
                productImage = "container-registry.oracle.com/middleware/fmw-infrastructure:12.2.1.3"
                break
            case "SOA":
                productId = "soa"
                productImage = "container-registry.oracle.com/middleware/soasuite:12.2.1.3"
                break
            case "OIG":
                productId = "oim"
                productImage = "fmw-paas-sandbox-cert-docker.dockerhub-den.oraclecorp.com/oracle/oig:12.2.1.4.0-190725.1317.317"
            default:
                productId = "unknown"
                break
        }
    }

    static getSamplesRepoDetails() {
        switch ("${productName}") {
            case "WLS":
                samplesRepo = "https://github.com/oracle/weblogic-kubernetes-operator"
                samplesDirectory = "domain-home-on-pv"
                break
            case "WLS-INFRA":
                samplesRepo = "https://github.com/oracle/weblogic-kubernetes-operator"
                samplesDirectory = ""
                switch (operatorVersion) {
                    case "2.3.0":
                        samplesDirectory = "domain-home-on-pv"
                }
                break
            case "SOA":
                samplesRepo = "https://github.com/sbattagi/weblogic-kubernetes-operator"
                samplesDirectory = ""
                break
            case "OIG":
                samplesRepo = "git@orahub.oraclecorp.com:idm/oim-kubernetes-operator.git"
                samplesDirectory = ""
            default:
                samplesRepo = "unknown"
                samplesDirectory = "unknown"
                break
        }
    }
}
