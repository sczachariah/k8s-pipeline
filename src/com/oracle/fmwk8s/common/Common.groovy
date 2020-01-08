package com.oracle.fmwk8s.common

import com.oracle.fmwk8s.env.Domain

import java.text.SimpleDateFormat

class Common extends Base {

    static getUniqueId() {
        def date = new Date()
        def sdf = new SimpleDateFormat("MMddHHmm")

        def buildNumber = "${script.env.BUILD_NUMBER}"
        runId = buildNumber + "-" + sdf.format(date)

        getKubernetesMasterUrl()
    }

    static createCommonK8SResources(){
        Domain.createNamespace()
        createUtilityConfigmap()
        configureRegistrySecret()
        EnvironmentSetup.createNfsFolder()
    }

    static configureRegistrySecret() {
        try {
            Log.info("begin configure registry secret.")

            script.sh label: "create container registry secret",
                    script: "retVal=`echo \\`kubectl get secret ${registrySecret} -n ${domainNamespace} 2>&1\\`` &&\
                       if echo \"\$retVal\" \\| grep -q 'not found'; then\n \
                          kubectl create secret docker-registry ${registrySecret} -n ${domainNamespace} --docker-server=http://container-registry.oracle.com --docker-username='${registryAuthUsr}' --docker-password='${registryAuthPsw}' --docker-email='${registryAuthUsr}'\n \
                       fi"
            script.sh label: "create denver registry secret",
                    script: "denRetVal=`echo \\`kubectl get secret ${denRegistrySecret} -n ${domainNamespace} 2>&1\\`` &&\
                       if echo \"\$denRetVal\" \\| grep -q 'not found'; then\n \
                          kubectl create secret docker-registry ${denRegistrySecret} -n ${domainNamespace} --docker-server=http://fmwk8s-dev.dockerhub-den.oraclecorp.com --docker-username='${registryAuthUsr}' --docker-password='${registryAuthPsw}' --docker-email='${registryAuthUsr}'\n \
                       fi"

            Log.info("configure registry secret success.")
        }
        catch (exc) {
            Log.error("configure registry secret failed.")
            throw exc
        }
    }

    static createUtilityConfigmap(){
        Log.info("begin create utility configmap.")

        script.git branch: 'master',
                credentialsId: "${sshCredentialId}",
                url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

        script.sh label: "create fmwk8s utility configmap",
                script: "kubectl apply -f kubernetes/framework/fmwk8s-utility-configmap.yaml -n ${Domain.domainNamespace}"

        Log.info("create utility configmap success.")
    }

    static getKubernetesMasterUrl() {
        Log.info("begin get k8s master url.")

        this.k8sMasterIP = script.sh(
                label: "get k8s master ip",
                script: "kubectl get nodes --selector=node-role.kubernetes.io/master " +
                        "-o jsonpath='{range .items[*]}{@.metadata.name}{\"\\t\"}{@.status.addresses[?(@.type==\"InternalIP\")].address}{\"\\n\"}{end}' " +
                        "| sed -n `echo \$((1 + (RANDOM % 10) % 3))`p | awk '{print \$2}'",
                returnStdout: true
        ).trim()
        this.k8sMasterUrl = "https://" + this.k8sMasterIP + ":6443"

        Log.info("k8s master ip  : ${k8sMasterIP}")
        Log.info("k8s master url : ${k8sMasterUrl}")
        Log.info("get k8s master url success.")
    }
}
