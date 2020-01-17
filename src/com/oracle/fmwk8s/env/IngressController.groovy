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
        try {
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
        } catch (exc) {
        }
        finally {
            sleep 60
            getLoadBalancerPort()
        }
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
     * configureApacheConfFileConfigmap - The configMap constructed in this method is having conf file required for
     * apache deployment and url access
     * @return
     */
    static configureApacheConfFileConfigmap() {
        try {
            Log.info("begin configure apache conf file configmap.")
            Log.info("Common.productName.toString() :: ${Common.productName}")

            if (Common.productName.toString().equalsIgnoreCase("SOA")) {
                script.sh label: "create apache conf file configmap",
                        script: "cd ${script.env.WORKSPACE}/kubernetes/framework/ingress-controller/apache-webtier && \
                       sed -i \"s#%ADMIN_SERVER_PORT%#${yamlUtility.domainInputsMap.get("adminPort")}#g\" custom-mod-wl-apache-for-soa-configmap.yaml && \
                       sed -i \"s#%ADMIN_SERVER_NAME%#${yamlUtility.domainInputsMap.get("adminServerName")}#g\" custom-mod-wl-apache-for-soa-configmap.yaml && \
                       sed -i \"s#%DOMAIN_NAME%#${Domain.domainName}#g\" custom-mod-wl-apache-for-soa-configmap.yaml && \
                       sed -i \"s#%CLUSTER_NAME%#${yamlUtility.domainInputsMap.get("clusterName")}#g\" custom-mod-wl-apache-for-soa-configmap.yaml && \
                       sed -i \"s#%MANAGED_SERVER_PORT%#${yamlUtility.domainInputsMap.get("managedServerPort")}#g\" custom-mod-wl-apache-for-soa-configmap.yaml && \
                       cat custom-mod-wl-apache-for-soa-configmap.yaml && \
                       kubectl apply -f custom-mod-wl-apache-for-soa-configmap.yaml -n ${domainNamespace} && \
                       sleep 60"
            } else {
                script.sh label: "create apache conf file configmap",
                        script: "cd ${script.env.WORKSPACE}/kubernetes/framework/ingress-controller/apache-webtier && \
                       sed -i \"s#%ADMIN_SERVER_PORT%#${yamlUtility.domainInputsMap.get("adminPort")}#g\" custom-mod-wl-apache-configmap.yaml && \
                       sed -i \"s#%ADMIN_SERVER_NAME%#${yamlUtility.domainInputsMap.get("adminServerName")}#g\" custom-mod-wl-apache-configmap.yaml && \
                       sed -i \"s#%DOMAIN_NAME%#${Domain.domainName}#g\" custom-mod-wl-apache-configmap.yaml && \
                       cat custom-mod-wl-apache-configmap.yaml && \
                       kubectl apply -f custom-mod-wl-apache-configmap.yaml -n ${domainNamespace} && \
                       sleep 60"
            }
            Log.info("configure apache conf file configmap success.")
        }
        catch (exc) {
            Log.error("configure apache conf file configmap failed.")
            throw exc
        }
    }

    /**
     * createCertificatePrivateKeySecretYAMLForApacheWebtier - Create certificate , private key & secret yaml file for apache webtier
     * @return
     */
    static def createCertificatePrivateKeySecretYAMLForApacheWebtier() {
        /** Variables required is declared */
        def sslCertFileName = "apache-sample.crt"
        def sslCertKeyFileName = "apache-sample.key"

        /**Prepare your own certificate and private key*/
        script.sh label: "Prepare your own certificate and private key",
                script: "cd ${script.env.WORKSPACE}/kubernetes/framework/ingress-controller/apache-webtier && \
                             ls && \
                             export VIRTUAL_HOST_NAME=apache-sample-host && \
                             export SSL_CERT_FILE=apache-sample.crt && \
                             export SSL_CERT_KEY_FILE=apache-sample.key && \
                             ls && \
                             sh certgen.sh && \
                             ls"

        /**Prepare the input values for the Apache webtier Helm chart as described in this step.*/
        def customSSLCertValue = script.sh(label: "get customSSLCertValue",
                script: "cd ${script.env.WORKSPACE}/kubernetes/framework/ingress-controller/apache-webtier && \
                                                    base64 -i ${sslCertFileName} | tr -d '\\n'",
                returnStdout: true
        ).trim()
        Log.info("customSSLCertValue :: ${customSSLCertValue}")

        def customSSLKeyValue = script.sh(label: "get customSSLKeyValue",
                script: "cd ${script.env.WORKSPACE}/kubernetes/framework/ingress-controller/apache-webtier && \
                                                    base64 -i ${sslCertKeyFileName} | tr -d '\\n'",
                returnStdout: true
        ).trim()
        Log.info("customSSLKeyValue :: ${customSSLKeyValue}")

        script.sh label: "create apache webtier secret yaml",
                script: "cd ${script.env.WORKSPACE}/kubernetes/framework/ingress-controller/apache-webtier && \
                       sed -i \"s#%LB_HELM_RELEASE_NAME%#${lbHelmRelease}#g\" secret.yaml && \
                       sed -i \"s#%DOMAIN_NAMESPACE%#${Domain.domainNamespace}#g\" secret.yaml && \
                       sed -i \"s#%CUSTOM_SSL_CERT%#${customSSLCertValue}#g\" secret.yaml && \
                       sed -i \"s#%CUSTOM_SSL_KEY%#${customSSLKeyValue}#g\" secret.yaml && \
                       cat secret.yaml && \
                       kubectl apply -f secret.yaml -n ${Domain.domainNamespace} && \
                       sleep 60"
    }

    /**
     * createServiceYAMLForApacheWebtier - Create service yaml file for apache webtier
     * @return
     */
    static def createServiceYAMLForApacheWebtier() {
        /** Variables declared */
        def apacheVirtualHostName = "apache-sample-host"
        def securePort = (!"${apacheVirtualHostName}".isEmpty()) ? 4433 : 443

        script.sh label: "create apache webtier service yaml",
                script: "cd ${script.env.WORKSPACE}/kubernetes/framework/ingress-controller/apache-webtier && \
                       sed -i \"s#%LB_HELM_RELEASE_NAME%#${lbHelmRelease}#g\" service.yaml && \
                       sed -i \"s#%DOMAIN_NAMESPACE%#${Domain.domainNamespace}#g\" service.yaml && \
                       sed -i \"s#%SECURE_PORT%#${securePort}#g\" service.yaml && \
                       cat service.yaml && \
                       kubectl apply -f service.yaml -n ${Domain.domainNamespace} && \
                       sleep 60"
    }

    /**
     * createDeploymentYAMLForApacheWebtier - Create deployment yaml file for apache webtier
     * @return
     */
    static def createDeploymentYAMLForApacheWebtier() {
        /** Variables declared */
        def apacheVirtualHostName = "apache-sample-host"
        def sslCertFileMountedPath = "/var/serving-cert/tls.crt"
        def sslKeyFileMountedPath = "/var/serving-cert/tls.key"
        def customImageForApacheWebtier = "fmwk8s-dev.dockerhub-den.oraclecorp.com/oracle/apache:12.2.1.3"
        def confFileConfigMapName = Common.productName.toString().equalsIgnoreCase("SOA") ?
                "custom-mod-wl-apache-for-soa-configmap" :
                "custom-mod-wl-apache-configmap"

        script.sh label: "create apache webtier deployment yaml",
                script: "cd ${script.env.WORKSPACE}/kubernetes/framework/ingress-controller/apache-webtier && \
                       sed -i \"s#%DOMAIN_NAMESPACE%#${Domain.domainNamespace}#g\" deployment.yaml && \
                       sed -i \"s#%LB_HELM_RELEASE_NAME%#${lbHelmRelease}#g\" deployment.yaml && \
                       sed -i \"s#%APACHE_WEBTIER_CUSTOM_IMAGE%#${customImageForApacheWebtier}#g\" deployment.yaml && \
                       sed -i \"s#%CUSTOM_CONF_FILE_CONFIGMAP%#${confFileConfigMapName}#g\" deployment.yaml && \
                       sed -i \"s#%APACHE_VIRTUAL_HOST_NAME%#${apacheVirtualHostName}#g\" deployment.yaml && \
                       sed -i \"s#%CUSTOM_SSL_CERT_FILE%#${sslCertFileMountedPath}#g\" deployment.yaml && \
                       sed -i \"s#%CUSTOM_SSL_KEY_FILE%#${sslKeyFileMountedPath}#g\" deployment.yaml && \
                       cat deployment.yaml && \
                       kubectl apply -f deployment.yaml -n ${Domain.domainNamespace} && \
                       sleep 60"
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

            /** The configMap constructed in this method is having conf file required for apache deployment and url access */
            configureApacheConfFileConfigmap()

            /** Create certificate , private key & secret yaml file for apache webtier */
            createCertificatePrivateKeySecretYAMLForApacheWebtier()

            /** Create service yaml file for apache webtier */
            createServiceYAMLForApacheWebtier()

            /** Create deployment yaml file for apache webtier */
            createDeploymentYAMLForApacheWebtier()

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
     * @param lbHelmRelease the name given to load balancer
     * @param domainNamespace the domain namespace to use to deploy the load balancer
     */
    static deployNginx() {
        try {
            Log.info("begin deploy nginx ingress controller.")
            script.sh label: "deploy nginx",
                    script: "helm init --client-only --skip-refresh --wait && \
                   helm repo update && \
                   helm install stable/nginx-ingress --name ${lbHelmRelease} --namespace ${domainNamespace} \
                   --set controller.scope.enabled=true \
                   --set controller.scope.namespace=${domainNamespace} --debug"
            Log.info("deploy nginx ingress controller success.")
        }
        catch (exc) {
            Log.error("deploy nginx ingress controller failed.")
            //throw exc
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
            script.sh label: "delete helm configmap",
                    script: "kubectl delete cm -n kube-system --selector=NAME=${lbHelmRelease}"

            Log.info("clean kubernetes ingress controller success.")
        }
        catch (exc) {
            Log.error("clean kubernetes ingress controller failed.")
        }
    }
}
