package com.oracle.fmwk8s.common

class Mapping {
    static Map<String, String> productIdMap = new HashMap<>()

    static {
        productIdMap.put("WLS", "weblogic")
        productIdMap.put("WLS-INFRA", "fmw-infrastructure")
        productIdMap.put("SOA", "soa")
        productIdMap.put("OIG", "oim")
    }
}
