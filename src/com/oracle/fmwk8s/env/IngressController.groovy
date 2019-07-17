package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Log

class IngressController {

    static def httplbPort
    static def httpslbPort

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

        getLoadBalancerPort(script, lbHelmRelease, domainNamespace)
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

    static deployApache(script, lbHelmRelease, domainNamespace) {
        try {
            Log.info(script, "begin deploy apache ingress controller.")
            script.sh "helm init && \
                   helm repo update && \
                   helm install bitnami/apache --name ${lbHelmRelease} --namespace ${domainNamespace}"
            Log.info(script, "deploy apache ingress controller success.")
        }
        catch (exc) {
            Log.error(script, "deploy apache ingress controller failed.")
            throw exc
        }
    }

    static deployVoyager(script, lbHelmRelease, domainNamespace) {
        try {
            Log.info(script, "begin deploy apache ingress controller.")
            script.sh "helm init && \
                   helm repo update && \
                   helm install stable/voyager --name ${lbHelmRelease} --namespace ${domainNamespace} \
                   --set ingressClass={${domainNamespace}}"
            Log.info(script, "deploy apache ingress controller success.")
        }
        catch (exc) {
            Log.error(script, "deploy apache ingress controller failed.")
            throw exc
        }
    }

    static deployNginx(script, lbHelmRelease, domainNamespace) {
        try {
            Log.info(script, "begin deploy nginx ingress controller.")
            script.sh "helm init && \
                   helm repo update && \
                   helm install stable/nginx-ingress --name ${lbHelmRelease} --namespace ${domainNamespace} \
                   --set controller.scope.namespace={${domainNamespace}}"
            Log.info(script, "deploy nginx ingress controller success.")
        }
        catch (exc) {
            Log.error(script, "deploy nginx ingress controller failed.")
            throw exc
        }
    }

    static getLoadBalancerPort(script, lbHelmRelease, domainNamespace) {
        try {
            Log.info(script, "begin get load balancer port.")
            script.sh "export KUBECONFIG=${script.env.KUBECONFIG}"
            script.sh "http=`kubectl describe service ${lbHelmRelease} --namespace ${domainNamespace}  | grep -i nodeport | grep 'http ' | awk -F/ '{print \$1}' | awk -F' ' '{print \$3}'` &&\
                       ${this.httplbPort}=\$http"
            script.sh "https=`kubectl describe service ${lbHelmRelease} --namespace ${domainNamespace}  | grep -i nodeport | grep 'https' | awk -F/ '{print \$1}' | awk -F' ' '{print \$3}'` &&\
                       ${this.httpslbPort}=\${https}"
            Log.info(script, ${this.httplbPort})
            Log.info(script, ${this.httpslbPort})
            Log.info(script, "get load balancer port success.")
        }
        catch (exc) {
            Log.error(script, "get load balancer port failed.")
        }
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
