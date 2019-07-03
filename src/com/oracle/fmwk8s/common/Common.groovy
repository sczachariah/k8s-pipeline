package com.oracle.fmwk8s.common

import java.text.SimpleDateFormat

class Common {
    static def domainName
    static def productId
    static def defaultProductImage

    static def samplesRepo
    static def samplesDirectory

    static def getUniqueId(def script, productName) {
        def date = new Date()
        def sdf = new SimpleDateFormat("MMddHHmm")

        def buildNumber = "${script.env.BUILD_NUMBER}"
        def uniqueId = buildNumber + "-" + sdf.format(date)

        getDomainName(productName)
        getProductIdentifier(productName)
        getSamplesRepo(productName)

        return uniqueId
    }

    static def getDomainName(productName) {
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
            default:
                domainName = "unknown"
                break
        }

        return domainName
    }

    static def getProductIdentifier(productName) {
        switch ("${productName}") {
            case "WLS":
                productId = "weblogic"
                defaultProductImage = "container-registry.oracle.com/middleware/weblogic:12.2.1.3"
                break
            case "WLS-INFRA":
                productId = "fmw-infrastructure"
                defaultProductImage = "container-registry.oracle.com/middleware/fmw-infrastructure:12.2.1.3"
                break
            case "SOA":
                productId = "soa"
                defaultProductImage = "container-registry.oracle.com/middleware/soasuite:12.2.1.3"
                break
            default:
                productId = "unknown"
                break
        }

        return productId
    }

    static def getSamplesRepo(productName) {
        switch ("${productName}") {
            case "WLS":
                samplesRepo = "https://github.com/oracle/weblogic-kubernetes-operator"
                samplesDirectory = "domain-home-on-pv"
                break
            case "WLS-INFRA":
                samplesRepo = "https://github.com/oracle/weblogic-kubernetes-operator"
                samplesDirectory = ""
                break
            case "SOA":
                samplesRepo = "git@orahub.oraclecorp.com:tooling/soa-kubernetes-operator.git"
                samplesDirectory = "domain-home-on-pv/multiple-Managed-servers"
                break
            default:
                samplesRepo = "unknown"
                samplesDirectory = "unknown"
                break
        }

        return samplesRepo
    }
}
