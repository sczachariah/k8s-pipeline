package com.oracle.fmwk8s.common

class Mapping {
    static Map<String, String> productIdMap = new HashMap<>()
    static Map<String, String> domainNameMap = new HashMap<>()
    static Map<String, String> loadBalancerMap = new HashMap<>()

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

        loadBalancerMap.put("TRAEFIK", "Traefik")
        loadBalancerMap.put("APACHE", "Apache")
        loadBalancerMap.put("VOYAGER", "Voyager")
        loadBalancerMap.put("NGINX", "Nginx")
    }
}
