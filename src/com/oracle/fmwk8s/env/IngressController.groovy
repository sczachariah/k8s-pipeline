package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Log

class IngressController {

    static lbPort

    static deployLoadBalancer(script, lbType, lbHelmRelease, domainNamespace) {
        switch ("${lbType}") {
            case "TRAEFIK":
                deployTraefik(script, lbHelmRelease, domainNamespace)
                break
            case "APACHE":
                deployApache(script, lbHelmRelease, domainNamespace)
                break
            case "VOYAGER":
                deployVoyager(script, lbHelmRelease, domainNamespace)
                break
            case "NGINX":
                deployNginx(script, lbHelmRelease, domainNamespace)
                break
            default:
                deployTraefik(script, lbHelmRelease, domainNamespace)
                break
        }

        getLoadBalancerPort(script)
    }

    static deployTraefik(script, lbHelmRelease, domainNamespace) {
        try {
            Log.info(script, "begin deploy traefik ingress controller.")
            script.sh "helm init && \
                   helm repo update && \
                   helm install stable/traefik --name ${lbHelmRelease} --namespace ${domainNamespace} \
                    --set kubernetes.namespaces={${domainNamespace}} \
                    --set serviceType=NodePort \
                    --set ssl.enabled=true \
                    --set ssl.insecureSkipVerify=true \
                    --set ssl.tlsMinVersion=VersionTLS12 \
                    --wait"
            Log.info(script, "deploy traefik ingress controller success.")
        }
        catch (exc) {
            Log.error(script, "deploy traefik ingress controller failed.")
            throw exc
        }
    }

    static deployApache(script, domainNamespace) {}

    static deployVoyager(script, domainNamespace) {}

    static deployNginx(script, lbHelmRelease, domainNamespace) {
        try {
            Log.info(script, "begin deploy nginx ingress controller.")
            script.sh "helm init && \
                   helm repo update && \
                   helm install stable/nginx-ingress --name ${lbHelmRelease} --namespace ${domainNamespace} \
                    --set kubernetes.namespaces={${domainNamespace}} \
                    --set serviceType=NodePort \
                    --set ssl.enabled=true \
                    --set ssl.insecureSkipVerify=true \
                    --set ssl.tlsMinVersion=VersionTLS12 \
                    --wait"
            Log.info(script, "deploy nginx ingress controller success.")
        }
        catch (exc) {
            Log.error(script, "deploy nginx ingress controller failed.")
            throw exc
        }
    }

    static getLoadBalancerPort(script) {
        script.sh ""
    }

    static undeployLoadBalancer(script, lbHelmRelease) {
        try {
            Log.info(script, "begin clean kubernetes ingress controller.")

            script.sh "helm delete --purge ${lbHelmRelease}"

            Log.info(script, "clean kubernetes ingress controller success.")
        }
        catch (exc) {
            Log.error(script, "clean kubernetes ingress controller failed.")
        }
    }
}
