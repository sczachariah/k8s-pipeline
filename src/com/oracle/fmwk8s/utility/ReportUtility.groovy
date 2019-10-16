package com.oracle.fmwk8s.utility

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.env.Domain
import com.oracle.fmwk8s.env.IngressController

class ReportUtility {

    static printDomainUrls(script) {
        String domainURLs = """
-----------Domain URL's------------
"""
        if (Common.productId.toString().equalsIgnoreCase("weblogic") ||
                Common.productId.toString().equalsIgnoreCase("fmw-infrastructure")) {
            domainURLs == domainURLs + """
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/console
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/weblogic/ready
"""
        } else if (Common.productId.toString().equalsIgnoreCase("soa")) {
            if (Domain.domainType.toString().equalsIgnoreCase("soa")) {
                domainURLs == domainURLs + """
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/console
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/em
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/weblogic/ready
"""
            }
        }
        domainURLs == domainURLs + """
-----------------------------------
"""
        script.sh "printf \"%s\\n\" \"${domainURLs}\""
    }
}
