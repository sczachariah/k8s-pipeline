package com.oracle.fmwk8s.common

import java.text.SimpleDateFormat

class Base {
    static def runId
    static def operatorVersion
    static def operatorBranch
    static def operatorImageVersion

    static def productId
    static def productName
    static def productImage
    static def registrySecret = "regcred"
    static def denRegistrySecret = "denregcred"

    static def domainName

    static def samplesRepo
    static def samplesBranch
    static def samplesDirectory

    static def elasticSearchHost = "elasticsearch.logging.svc.cluster.local"
    static def elasticSearchPort = "9200"
    static def OPERATOR_NS
    static def OPERATOR_SA
    static def OPERATOR_HELM_RELEASE
    static def DOMAIN_NAME
    static def DOMAIN_NS
    static def WEBLOGIC_USER
    static def ADMIN_PASSWORD
    static def FMWK8S_NFS_HOME
    static def NFS_DOMAIN_DIR
    static def NFS_DOMAIN_PATH
    static def REGISTRY_AUTH
    static def LB_HELM_RELEASE



    Base() {
        System.out.println("Inside constructor")
        getDomainName()
        getProductIdentifier()
        getSamplesRepo()
        getOperatorVarNames()
        gerDomainVarNames()
        getNfsPathNames()
        getLoadBalancerNames()
    }

    static def getOperatorVarNames(){
        REGISTRY_AUTH = credentials("sandeep.zachariah.docker")
        OPERATOR_NS = "${DOMAIN_NAME}-operator-ns-${runId}"
        OPERATOR_SA = 'default'
        OPERATOR_HELM_RELEASE = "op-${runId}"
    }

    static def gerDomainVarNames(){
        DOMAIN_NAME = getDomainName("${productName}")
        DOMAIN_NS = "${DOMAIN_NAME}-domain-ns-${runId}"
        WEBLOGIC_USER = 'weblogic'
        ADMIN_PASSWORD = 'Welcome1'
    }

    static def getNfsPathNames(){
        FMWK8S_NFS_HOME = "/scratch/u01/DockerVolume/domains"
        NFS_DOMAIN_DIR = "${DOMAIN_NS}"
        NFS_DOMAIN_PATH = "${FMWK8S_NFS_HOME}/${NFS_DOMAIN_DIR}"
    }

    static def getLoadBalancerNames(){
        LB_HELM_RELEASE = "lb-${runId}"
    }

    static def getOperatorVersions(operatorVersion) {
        switch ("${operatorVersion}") {
            case "2.1":
                this.operatorVersion = "2.1"
                operatorBranch = "release/2.1"
                operatorImageVersion = "2.1"
                samplesBranch = "release/2.1"
                break
            case "2.2":
                this.operatorVersion = "2.2"
                operatorBranch = "release/2.2"
                operatorImageVersion = "2.2.0"
                samplesBranch = "release/2.2"
                break
            case "2.2.1":
                this.operatorVersion = "2.2.1"
                operatorBranch = "release/2.2.1"
                operatorImageVersion = "2.2.1"
                samplesBranch = "release/2.2.1"
                switch (productName) {
                    case "SOA":
                        samplesBranch = "soa-2.2.1-dev"
                }
                break
            case "2.3.0":
                this.operatorVersion = "2.3.0"
                operatorBranch = "release/2.3.0"
                operatorImageVersion = "2.3.0"
                samplesBranch = "release/2.3.0"
                switch (productName) {
                    case "SOA":
                        samplesBranch = "soa-2.2.1-dev"
                }
                break
            default:
                this.operatorVersion = "develop"
                operatorBranch = "develop"
                operatorImageVersion = "develop"
                samplesBranch = "develop"
                break
        }
        getSamplesRepo(productName)
    }

    def getDomainName() {
        System.out.println("Inside the getDomainname")
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

        return domainName
    }

    static def getProductIdentifier() {
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

    static def getSamplesRepo() {
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
