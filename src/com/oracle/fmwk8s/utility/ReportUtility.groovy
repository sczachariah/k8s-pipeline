package com.oracle.fmwk8s.utility

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.env.Domain
import com.oracle.fmwk8s.env.IngressController

class ReportUtility {

    static printDomainUrls(script) {
        String domainURLs = """
-----------Domain URL's------------
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/weblogic/ready
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/console
"""
        if (Common.productId.toString().equalsIgnoreCase("soa")) {
            if (Domain.domainType.toString().toLowerCase().contains("soa") ||
                    Domain.domainType.toString().toLowerCase().contains("osb")) {
                domainURLs = domainURLs + """
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/em
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/soa-infra/
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/soa/composer
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/integration/worklistapp
"""
            }
            if (Domain.domainType.toString().toLowerCase().contains("osb")) {
                domainURLs = domainURLs + """
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/servicebus
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/sbconsole
"""
            }
            if (Domain.domainType.toString().toLowerCase().contains("ess")) {
                domainURLs = domainURLs + """
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/ess
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/EssHealthCheck
"""
            }
        }
        domainURLs = domainURLs + """
-----------------------------------
"""
        script.sh label: "print domain url's",
                script: "printf \"%s\\n\" \"${domainURLs.toString()}\""
    }
}
