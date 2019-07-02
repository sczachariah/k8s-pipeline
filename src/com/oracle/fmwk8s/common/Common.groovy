package com.oracle.fmwk8s.common

import java.text.SimpleDateFormat

class Common {
    static def domainName
    static def productId
    static def samplesRepo

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
            case "SOA":
                domainName = "soainfra"
                break
            case "WLS-INFRA":
                domainName = "wls-infra"
                break
            default:
                domainName = "unknown"
                break
        }

        return domainName
    }

    static def getProductIdentifier(productName) {
        switch ("${productName}") {
            case "SOA":
                productId = "soa"
                break
            case "WLS-INFRA":
                productId = "weblogic"
                break
            default:
                productId = "unknown"
                break
        }

        return productId
    }

    static def getSamplesRepo(productName) {
        switch ("${productName}") {
            case "SOA":
                samplesRepo = "git@orahub.oraclecorp.com:tooling/soa-kubernetes-operator.git"
                break
            case "WLS-INFRA":
                samplesRepo = "https://github.com/oracle/weblogic-kubernetes-operator"
                break
            default:
                samplesRepo = "unknown"
                break
        }

        return samplesRepo
    }
}
