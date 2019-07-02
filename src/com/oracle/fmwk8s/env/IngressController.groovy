package com.oracle.fmwk8s.env

class IngressController {

    static deployLoadBalancer(script, lbType, domainNamespace) {
        switch ("${lbType}") {
            case "TRAEFIK":
                deployTraefik(domainNamespace)
                break
            case "APACHE":
                deployApache(domainNamespace)
                break
            case "VOYAGER":
                deployVoyager(domainNamespace)
                break
            case "NGINX":
                deployNginx(domainNamespace)
                break
            default:
                deployTraefik(domainNamespace)
                break
        }
    }

    static deployTraefik(script, domainNamespace) {}

    static deployApache(script, domainNamespace) {}

    static deployVoyager(script, domainNamespace) {}

    static deployNginx(script, domainNamespace) {}
}
