package com.oracle.fmwk8s.common

import com.oracle.fmwk8s.env.IngressController

class Mapping {
    static Map<String, String> productIdMap = new HashMap<>()
    static Map<String, String> domainNameMap = new HashMap<>()
    static Map<String, Object> loadBalancerMap = new HashMap<>()
    static Map<String, String> operatorVersionMap = new HashMap<>()
    static Map<String, String> operatorBranchMap = new HashMap<>()

    static {
        productIdMap.put("WLS", "weblogic")
        productIdMap.put("WLS-INFRA", "fmw-infrastructure")
        productIdMap.put("SOA", "soa")
        productIdMap.put("WCP", "wcp")
        productIdMap.put("OIG", "oim")

        domainNameMap.put("WLS", "weblogic")
        domainNameMap.put("WLS-INFRA", "wlsinfra")
        domainNameMap.put("SOA", "soainfra")
        domainNameMap.put("WCP", "wcpinfra")
        domainNameMap.put("OIG", "oim")

        loadBalancerMap.put("TRAEFIK", IngressController.deployTraefik())
        loadBalancerMap.put("APACHE", IngressController.deployApache())
        loadBalancerMap.put("VOYAGER", IngressController.deployVoyager())
        loadBalancerMap.put("NGINX", IngressController.deployNginx())

        operatorVersionMap.put("2.3.0", "2.3.0")
        operatorVersionMap.put("2.4.0", "2.4.0")

        operatorBranchMap.put("2.3.0", "release/2.3.0")
        operatorBranchMap.put("2.4.0", "release/2.4.0")
        operatorBranchMap.put("default", "default")
    }
}
