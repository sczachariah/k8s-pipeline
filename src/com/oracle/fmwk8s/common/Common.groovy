package com.oracle.fmwk8s.common

import java.text.SimpleDateFormat

class Common {
    static def getUniqueId(def script) {
        def date = new Date()
        def sdf = new SimpleDateFormat("MMddHHmm")

        def buildNumber = "${script.env.BUILD_NUMBER}"
        def uniqueId = buildNumber + "-" + sdf.format(date)

        return uniqueId
    }

    static def getDomainName(productName) {
        def domainName

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

    static def getSampleRepo(productName) {
        def repo

        switch ("${productName}") {
            case "SOA":
                repo = "git@orahub.oraclecorp.com:tooling/soa-kubernetes-operator.git"
                break
            default:
                repo = "unknown"
                break
        }

        return repo
    }
}
