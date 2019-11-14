package com.oracle.fmwk8s.env


import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log

/**
 * IngressController class deploys the different loadBalancers per domain that are required
 * in E2E execution of FMW in Docker/K8S environments
 */
class IngressController extends Common {

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
    static deployLoadBalancer() {
        switch ("${lbType}") {
            case "TRAEFIK":
                deployTraefik()
                break
            case "APACHE":
                deployApache()
                break
            case "VOYAGER":
                deployVoyager()
                break
            case "NGINX":
                deployNginx()
                break
            default:
                deployTraefik()
                break
        }

        getLoadBalancerPort()
        sleep 60
    }

    /**
     * deploys the Traefik load balancer
     *
     * @param script the workflow script of jenkins
     * @param lbHelmRelease the name given to load balacer
     * @param domainNamespace the domain namespace to use to deploy the load balancer
     */
    static deployTraefik() {
        try {
            Log.info("begin deploy traefik ingress controller.")
            script.sh label: "deploy traefik",
                    script: "helm init --client-only --skip-refresh --wait && \
                   helm repo update && \
                   helm install stable/traefik --name ${lbHelmRelease} --namespace ${domainNamespace} \
                    --set kubernetes.namespaces={${domainNamespace}} \
                    --set serviceType=NodePort \
                    --set ssl.enabled=true \
                    --set ssl.insecureSkipVerify=true \
                    --set ssl.tlsMinVersion=VersionTLS12 \
                    --wait"
            Log.info("deploy traefik ingress controller success.")
        }
        catch (exc) {
            Log.error("deploy traefik ingress controller failed.")
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
    static deployApache() {
        try {
            Log.info("begin deploy apache ingress controller.")
            Log.info(operatorBranch)
            script.sh label: "deploy apache",
                    script: "helm init --client-only --skip-refresh --wait && \
                       helm repo update && \
                       helm install ../fmwk8s/kubernetes/framework/ingress-controller/apache-webtier --name ${lbHelmRelease} --namespace ${domainNamespace} --set image=fmwk8s-dev.dockerhub-den.oraclecorp.com/oracle/apache:12.2.1.3,imagePullSecrets=${denRegistrySecret}"

            Log.info("deploy apache ingress controller success.")
        }
        catch (exc) {
            Log.error("deploy apache ingress controller failed.")
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
    static deployVoyager() {
        try {
            Log.info("begin deploy apache ingress controller.")
            script.sh label: "deploy voyager",
                    script: "helm init --client-only --skip-refresh --wait && \
                   helm repo update && \
                   helm install stable/voyager --name ${lbHelmRelease} --namespace ${domainNamespace} \
                   --set ingressClass={${domainNamespace}}"
            Log.info("deploy apache ingress controller success.")
        }
        catch (exc) {
            Log.error("deploy apache ingress controller failed.")
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
    static deployNginx() {
        try {
            Log.info("begin deploy nginx ingress controller.")
            script.sh label: "deploy nginx",
                    script: "helm init --client-only --skip-refresh --wait && \
                   helm repo update && \
                   helm install stable/nginx-ingress --name ${lbHelmRelease} --namespace ${domainNamespace} \
                   --set controller.scope.namespace={${domainNamespace}} --wait"
            Log.info("deploy nginx ingress controller success.")
        }
        catch (exc) {
            Log.error("deploy nginx ingress controller failed.")
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
    static getLoadBalancerPort() {
        try {
            Log.info("begin get load balancer port.")

            httplbPort = script.sh(
                    label: "get lb http port",
                    script: "kubectl describe service ${lbHelmRelease} --namespace ${domainNamespace}  | grep -i nodeport | grep \'http \' | awk -F/ \'{print \$1}\' | awk -F\' \' \'{print \$3}\'",
                    returnStdout: true
            ).trim()

            httpslbPort = script.sh(
                    label: "get lb https port",
                    script: "kubectl describe service ${lbHelmRelease} --namespace ${domainNamespace}  | grep -i nodeport | grep \'https\' | awk -F/ \'{print \$1}\' | awk -F\' \' \'{print \$3}\'",
                    returnStdout: true
            ).trim()

            Log.info("${httplbPort}")
            Log.info("${httpslbPort}")
            Log.info("get load balancer port success.")
        }
        catch (exc) {
            Log.error("get load balancer port failed.")
            throw exc
        }
    }

    /**
     * undeploys the load balancer
     *
     * @param script the workflow script of jenkins
     * @param lbHelmRelease the name of the load balacer
     */
    static undeployLoadBalancer() {
        try {
            Log.info("begin clean kubernetes ingress controller.")

            script.sh label: "undeploy ingress controller",
                    script: "helm delete --purge ${lbHelmRelease}"

            Log.info("clean kubernetes ingress controller success.")
        }
        catch (exc) {
            Log.error("clean kubernetes ingress controller failed.")
        }
    }
}
