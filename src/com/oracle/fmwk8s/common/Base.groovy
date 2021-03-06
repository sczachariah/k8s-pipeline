package com.oracle.fmwk8s.common

import com.oracle.fmwk8s.utility.CommonUtility
import com.oracle.fmwk8s.utility.K8sUtility
import com.oracle.fmwk8s.utility.YamlUtility

import java.text.SimpleDateFormat

class Base {
    static def script
    static def yamlUtility = new YamlUtility()
    static def commonUtility = new CommonUtility()
    static def k8sUtility = new K8sUtility()

    static def pipelineStartTime
    static def pipelineEndTime
    static def testStartTime
    static def testEndTime

    static def sshCredentialId = "fmwk8sval_ww.ssh"
    static def cloud
    static def k8sMasterUrl = ""
    static def k8sMasterIP = ""

    static def runId
    static def buildName
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

    static def fmwProductReleaseVersion
    static def productId
    static def productName
    static def productImage

    static def testImage
    static def testType
    static def testTarget
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

//    /scratch/u01/DockerVolume/domains
    static def fmwk8sNfsHome
//    wls-domain-ns-1234
    static def nfsDomainDir
//    /scratch/u01/DockerVolume/domains/wls-domain-ns-1234
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
        testTarget = script.env.TEST_TARGET
        elkEnable = script.env.ELK_ENABLE
        hoursAfter = Long.valueOf(script.env.HOURS_AFTER)
        hoursAfterSeconds = hoursAfter * 60 * 60
        emailRecipients = script.env.EMAIL_TO
    }

    static defineKibanaUrl() {
        if ("${elkEnable}" == "true") {
            if ("${cloud}".equalsIgnoreCase("oci-v1.12.9")) {
                kibanaUrl = "https://fmwk8s.us.oracle.com/kibana"
            } else if ("${cloud}".equalsIgnoreCase("vm-based-k8s")) {
                kibanaUrl = "https://fmwk8s-stage.us.oracle.com/kibana"
            }
        } else {
            kibanaUrl = ""
        }
    }

    static getDomainVariables() {
        if (!Mapping.domainNameMap.containsKey(productName)) {
            domainName = "unknown"
        } else {
            domainName = Mapping.domainNameMap.get(productName)
        }
        domainNamespace = "${domainName}-domain-ns-${runId}"
        weblogicUsername = 'weblogic'
        weblogicPassword = commonUtility.generatePassword()
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

        operatorImageVersion = Mapping.operatorVersionMap.get(operatorVersion)
        operatorBranch = Mapping.operatorBranchMap.get(operatorVersion)
        samplesBranch = Mapping.operatorBranchMap.get(operatorVersion)

        if (("${productName}" == "SOA") && ("${operatorVersion}" == "2.3.0")) {
            samplesRepo = "https://github.com/sbattagi/weblogic-kubernetes-operator"
            samplesBranch = "soa-2.3.0"
        }
        // temp condition to test bug 30433422
        //if (("${productName}" == "SOA") && ("${operatorVersion}" == "2.4.0")) {
        //    samplesRepo = "https://github.com/sbattagi/weblogic-kubernetes-operator"
        //    samplesBranch = "soa-2.4.0-develop"
        //}
    }

    static getProductIdentifier() {
        if (!Mapping.productIdMap.containsKey(productName)) {
            productId = "unknown"
        } else {
            productId = Mapping.productIdMap.get(productName)
        }
    }

    static getSamplesRepoDetails() {
        switch ("${productName}") {
            case "SOA":
                if (productImage.toString().contains("12.2.1.4")) {
                    samplesRepo = "https://github.com/sbattagi/weblogic-kubernetes-operator"
                    samplesBranch = "soa-2.4.0-develop-12.2.1.4.0"
                }
                break
            case "WCP":
                samplesRepo = "git@orahub.oraclecorp.com:tooling/wcp-kubernetes-operator.git"
                samplesBranch = "master"
                if (productImage.toString().contains("12.2.1.3")) {
                    samplesBranch = "PS3"
                    samplesDirectory = ""
                }
                break
            case "WC-SITES":
                samplesRepo = "git@orahub.oraclecorp.com:tooling/wcsites-k8s-operator.git"
                samplesBranch = "2.4.0"
                break
            case "OIG":
                samplesRepo = "git@orahub.oraclecorp.com:idm/oim-kubernetes-operator.git"
                samplesBranch = "release/2.2.1"
                samplesDirectory = ""
                break
        }
    }

    static getCurrentDateTime() {
        def date = new Date()
        def sdf = new SimpleDateFormat("MMM-dd-yyyy HH:mm:ss Z")
        return sdf.format(date).toString()
    }
}
