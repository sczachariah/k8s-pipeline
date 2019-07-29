package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Common
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
            script.sh "helm init --client-only --skip-refresh --wait && \
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
            Log.info(script, Common.operatorBranch)
            script.sh "export KUBECONFIG=${script.env.KUBECONFIG} && \
                   helm init --client-only --skip-refresh --wait && \
                   helm repo update && \
                   helm install ../fmwk8s/kubernetes/framework/ingress-controller/apache-webtier --name ${lbHelmRelease} --namespace ${domainNamespace} --set image=fmwk8s-dev.dockerhub-den.oraclecorp.com/oracle/apache:12.2.1.3,imagePullSecrets=${Common.denRegistrySecret}"

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
            script.sh "helm init --client-only --skip-refresh --wait && \
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
            script.sh "helm init --client-only --skip-refresh --wait && \
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

            this.httplbPort = script.sh(
                    script: "kubectl describe service ${lbHelmRelease} --namespace ${domainNamespace}  | grep -i nodeport | grep \'http \' | awk -F/ \'{print \$1}\' | awk -F\' \' \'{print \$3}\'",
                    returnStdout: true
            ).trim()

            this.httpslbPort = script.sh(
                    script: "kubectl describe service ${lbHelmRelease} --namespace ${domainNamespace}  | grep -i nodeport | grep \'https\' | awk -F/ \'{print \$1}\' | awk -F\' \' \'{print \$3}\'",
                    returnStdout: true
            ).trim()

            Log.info(script, "${this.httplbPort}")
            Log.info(script, "${this.httpslbPort}")
            Log.info(script, "get load balancer port success.")
        }
        catch (exc) {
            Log.error(script, "get load balancer port failed.")
            throw exc
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
