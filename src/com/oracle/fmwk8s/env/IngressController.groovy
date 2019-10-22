package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log

/**
 * IngressController class deploys the different loadBalancers per domain that are required
 * in E2E execution of FMW in Docker/K8S environments
 */
class IngressController {

    /** the http port for the deployed load balancer */
    static def httplbPort
    /** the https port for the deployed load balancer */
    static def httpslbPort

    /**
     * deploys the load balancer that is selected as input to the E2E execution job
     *
     * @param script the workflow script of jenkins
     * @param lbType the load balancer type TRAFFIC/APACHE/NGINX/VOYAGER
     * @param lbHelmRelease the name given to load balacer
     * @param domainNamespace the domain namespace given to the product
     */
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

    /**
     * deploys the Traefik load balancer
     *
     * @param script the workflow script of jenkins
     * @param lbHelmRelease the name given to load balacer
     * @param domainNamespace the domain namespace to use to deploy the load balancer
     */
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

    /**
     * deploys the Apache load balancer
     *
     * @param script the workflow script of jenkins
     * @param lbHelmRelease the name given to load balacer
     * @param domainNamespace the domain namespace to use to deploy the load balancer
     */
    static deployApache(script, lbHelmRelease, domainNamespace) {
        try {
            Log.info(script, "begin deploy apache ingress controller.")
            Log.info(script, Common.operatorBranch)
            script.sh "helm init --client-only --skip-refresh --wait && \
                       helm repo update && \
                       helm install ../fmwk8s/kubernetes/framework/ingress-controller/apache-webtier --name ${lbHelmRelease} --namespace ${domainNamespace} --set image=fmwk8s-dev.dockerhub-den.oraclecorp.com/oracle/apache:12.2.1.3,imagePullSecrets=${Common.denRegistrySecret}"

            Log.info(script, "deploy apache ingress controller success.")
        }
        catch (exc) {
            Log.error(script, "deploy apache ingress controller failed.")
            throw exc
        }
    }

    /**
     * deploys the Voyager load balancer
     *
     * @param script the workflow script of jenkins
     * @param lbHelmRelease the name given to load balacer
     * @param domainNamespace the domain namespace to use to deploy the load balancer
     */
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

    /**
     * deploys the Nginx load balancer
     *
     * @param script the workflow script of jenkins
     * @param lbHelmRelease the name given to load balacer
     * @param domainNamespace the domain namespace to use to deploy the load balancer
     */
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

    /**
     * get the https,https load balancer port numbers
     *
     * @param script the workflow script of jenkins
     * @param lbHelmRelease the name given to load balacer
     * @param domainNamespace the domain namespace to use to deploy the load balancer
     */
    static getLoadBalancerPort(script, lbHelmRelease, domainNamespace) {
        try {
            Log.info(script, "begin get load balancer port.")

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

    /**
     * undeploys the load balancer
     *
     * @param script the workflow script of jenkins
     * @param lbHelmRelease the name of the load balacer
     */
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
