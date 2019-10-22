package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Base
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.utility.K8sUtility
import com.oracle.fmwk8s.utility.ReportUtility

/**
 * Domain class handles the common domain operations that are required
 * in E2E execution of FMW in Docker/K8S environments
 */
class Domain extends Base {
    static def weblogicCredentialsSecretName
    static def createDomainPodName
    static def adminServerPodName
    static def managedServerPodName
    static def replicaCount

    static pullSampleScripts() {
        script.git branch: "${samplesBranch}",
                credentialsId: 'sandeep.zachariah.ssh',
                url: "${samplesRepo}"
    }

    static configureRcuSecret() {
        try {
            if (productId != "weblogic") {
                Log.info(script, "begin configure rcu secrets.")

                script.sh "retVal=`echo \\`kubectl get secret ${domainName}-rcu-credentials -n ${domainNamespace} 2>&1\\`` &&\
                       if echo \"\$retVal\" | grep -q \"not found\"; then \n \
                          kubernetes/samples/scripts/create-rcu-credentials/create-rcu-credentials.sh -u ${domainName} -p Welcome1 -a sys -q ${Database.dbPassword} -d ${domainName} -n ${domainNamespace} \n \
                       fi"

                Log.info(script, "configure rcu secrets success.")
            }
        }
        catch (exc) {
            Log.error(script, "configure rcu secrets failed.")
            throw exc
        }
    }

    static preparRcu() {
        try {
            if (productId != "weblogic" && operatorVersion != "2.1") {
                Log.info(script, "begin prepare rcu.")

                script.git branch: 'master',
                        credentialsId: 'sandeep.zachariah.ssh',
                        url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

                script.sh "cd kubernetes/framework/db/rcu && \
                           sed -i \"s|%CONNECTION_STRING%|${Database.dbName}.${domainNamespace}:${Database.dbPort}/${Database.dbName}pdb.us.oracle.com|g\" ${productId}-rcu-configmap.yaml && \
                           sed -i \"s|%RCUPREFIX%|${domainName}|g\" ${productId}-rcu-configmap.yaml && \
                           sed -i \"s|%SYS_PASSWORD%|${Database.dbPassword}|g\" ${productId}-rcu-configmap.yaml && \
                           sed -i \"s|%PASSWORD%|Welcome1|g\" ${productId}-rcu-configmap.yaml && \
                           cat ${productId}-rcu-configmap.yaml"

                script.sh "cd kubernetes/framework/db/rcu && \
                           sed -i \"s|%DB_SECRET%|${registrySecret}|g\" fmwk8s-rcu-pod.yaml && \
                           sed -i \"s|%PRODUCT_ID%|${productId}|g\" fmwk8s-rcu-pod.yaml && \
                           sed -i \"s|%PRODUCT_IMAGE%|${productImage}|g\" fmwk8s-rcu-pod.yaml && \
                           cat fmwk8s-rcu-pod.yaml"

                script.sh "kubectl apply -f kubernetes/framework/db/rcu/${productId}-rcu-configmap.yaml -n ${domainNamespace} && \
                           kubectl apply -f kubernetes/framework/db/rcu/fmwk8s-rcu-pod.yaml -n ${domainNamespace}"

                script.sh "rcustat='rcustat' && \
                           i=0 && \
                           until `echo \$rcustat | grep -q Completed` > /dev/null\n \
                           do \n \
                               if [ \$i == 10 ]; then\n \
                                   echo \"Timeout waiting for RCU. Exiting!!.\"\n \
                                   exit 1\n \
                               fi\n \
                           i=\$((i+1))\n \
                           echo \"RCU in progress. Iteration \$i of 10. Sleeping\"\n \
                           sleep 60\n \
                           rcustat=`echo \\`kubectl get pods -n ${domainNamespace} 2>&1 | grep fmwk8s-rcu\\``\n \
                           done"


                Log.info(script, "prepare rcu success.")
            }
        }
        catch (exc) {
            Log.error(script, "prepare rcu failed.")
            throw exc
        }
        finally {
            Log.info(script, "begin fetch rcu pod logs.")
            Logging.getPodLogs(script, 'fmwk8s-rcu', domainName)
            Log.info(script, "fetch rcu pod logs success.")
        }
    }

    static configureDomainSecret(script) {
        try {
            Log.info(script, "begin configure domain secrets.")

            weblogicCredentialsSecretName = "${domainName}-weblogic-credentials"

            script.sh "retVal=`echo \\`kubectl get secret ${weblogicCredentialsSecretName} -n ${domainNamespace} 2>&1\\`` &&\
                       if echo \"\$retVal\" | grep -q \"not found\"; then \n \
                          kubernetes/samples/scripts/create-weblogic-domain-credentials/create-weblogic-credentials.sh -u ${weblogicUsername} -p ${weblogicPassword} -n ${domainNamespace} -d ${domainName} \n \
                       fi"

            Log.info(script, "configure domain secrets success.")

        }
        catch (exc) {
            Log.error(script, "configure domain secrets failed.")
            throw exc
        }
    }

    static preparePersistentVolume() {
        try {
            Log.info(script, "begin prepare persistent volume.")

            script.sh "cp -r kubernetes/samples/scripts/create-weblogic-domain-pv-pvc/create-pv-pvc-inputs.yaml create-pv-pvc-inputs && \
                       ls -ltr . && cat create-pv-pvc-inputs"

            yamlUtility.generatePeristentVolumeInputsYaml(script, domainName, domainNamespace, nfsDomainPath, "create-pv-pvc-inputs")

            script.sh "cat create-pv-pvc-inputs.yaml && \
                       ./kubernetes/samples/scripts/create-weblogic-domain-pv-pvc/create-pv-pvc.sh -i create-pv-pvc-inputs.yaml -o script-output-directory"

            script.sh "cp script-output-directory/pv-pvcs/${domainName}-${domainNamespace}-pv.yaml . && \
                       cp script-output-directory/pv-pvcs/${domainName}-${domainNamespace}-pvc.yaml . && \
                       cat ${domainName}-${domainNamespace}-pv.yaml && \
                       cat ${domainName}-${domainNamespace}-pvc.yaml"

            script.sh "kubectl apply -f ${domainName}-${domainNamespace}-pv.yaml -n ${domainNamespace} && \
                       kubectl apply -f ${domainName}-${domainNamespace}-pvc.yaml -n ${domainNamespace} && \
                       kubectl describe pv ${domainName}-${domainNamespace}-pv -n ${domainNamespace} && \
                       kubectl describe pvc ${domainName}-${domainNamespace}-pvc -n ${domainNamespace}"

            Log.info(script, "prepare persistent volume success.")

        }
        catch (exc) {
            Log.error(script, "prepare persistent volume failed.")
            throw exc
        }
    }

    static prepareDomain(script) {
        try {
            Log.info(script, "begin prepare domain.")

            if (domainType.toString().equalsIgnoreCase("N/A")) {
                domainType = "weblogic"
            }

            if (productImage?.trim()) {
                productImage = productImage
            }

            script.sh "cp -r kubernetes/samples/scripts/create-${productId}-domain/${samplesDirectory}/create-domain-inputs.yaml create-domain-inputs && \
                       cp -r kubernetes/samples/scripts/create-${productId}-domain/${samplesDirectory}/create-domain-job-template.yaml create-domain-job-template && \
                       ls -ltr . && cat create-domain-inputs"

            yamlUtility.generateDomainInputsYaml(script, domainType, domainName, domainNamespace, "create-domain-inputs")

            script.sh "cat create-domain-inputs.yaml"

            Log.info(script, "prepare domain success.")

        }
        catch (exc) {
            Log.error(script, "prepare domain failed.")
            throw exc
        }
    }

    static createDomain() {
        try {
            this.domainName = domainName
            this.domainNamespace = domainNamespace

            Log.info(script, "begin create " + productId + " domain.")
            script.sh "./kubernetes/samples/scripts/create-${productId}-domain/${samplesDirectory}/create-domain.sh -i create-domain-inputs.yaml -o script-output-directory"
            script.sh "mkdir -p ${domainName}-${domainNamespace} && \
                       ls -ltr script-output-directory/weblogic-domains/ && \
                       cp -r script-output-directory/weblogic-domains/${domainName}/domain.yaml domain && \
                       cp -r script-output-directory/weblogic-domains/${domainName}/domain.yaml domain" + productId + ""
            Log.info(script, "create " + productId + " domain success.")

            Log.info(script, "begin start " + productId + " domain")
            yamlUtility.generateDomainYaml(script, productId, "domain")
            script.sh "ls -ltr && cat domain*"
            script.sh "kubectl apply -f domain.yaml -n ${domainNamespace}"
            if ("${productId}" == "oim") {
                script.sh "kubectl apply -f domain" + productId + ".yaml -n ${domainNamespace} && \
                           sleep 480000"
            }
            Log.info(script, "start " + productId + " domain success.")

            ReportUtility.printDomainUrls(script)
            isDomainReady()

        }
        catch (exc) {
            Log.error(script, "create/start " + productId + " domain failed.")
            throw exc
        }
        finally {
            this.createDomainPodName = script.sh(
                    script: "kubectl get pods -o go-template --template \'{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}\' -n ${domainNamespace} | grep ${domainName}-create",
                    returnStdout: true
            ).trim()
            Log.info(script, "begin fetch create domain job pod logs.")
            Logging.getPodLogs(script, this.createDomainPodName, domainNamespace)
            Log.info(script, "fetch create domain job pod logs success.")
        }
    }

    static isDomainReady() {
        try {
            Log.info(script, "begin domain readiness check.")

            script.sh "kubectl get all,domains -n ${domainNamespace}"
            Log.info(script, "begin Admin server status check")
            adminServerPodName = "${domainName}-${yamlUtility.domainInputsMap.get("adminServerName")}"
            Log.info(script, adminServerPodName)
            K8sUtility.checkPodStatus(script, adminServerPodName, domainNamespace, 20)
            Log.info(script, "admin server status check completed.")
            Log.info(script, "begin Managed server status check.")
            script.sh "kubectl get domain -n ${domainNamespace} -o yaml > ${domainName}-domain.yaml && \
                       ls"
            replicaCount = script.sh(
                    script: "cat ${domainName}-domain.yaml | grep replicas:|tail -1|awk -F':' '{print \$2}'",
                    returnStdout: true
            ).trim()
            Log.info(script, replicaCount)
            for (int i = 1; i <= Integer.parseInt(replicaCount); i++) {
                managedServerPodName = "${domainName}-${yamlUtility.domainInputsMap.get("managedServerNameBase")}${i}"
                K8sUtility.checkPodStatus(script, managedServerPodName, domainNamespace, 20)
            }
            Log.info(script, "managed server status check completed.")
            Log.info(script, "domain readiness check success.")
        }
        catch (exc) {
            Log.error(script, "domain readiness check failed.")
        }
    }

    static configureDomainLoadBalancer() {
        try {
            Log.info(script, "begin configure domain loadbalancer.")
            script.git branch: 'master',
                    credentialsId: 'sandeep.zachariah.ssh',
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh "ls -ltr"
            script.sh "ls -ltr kubernetes/framework/charts/ingress-per-domain"
            script.sh "helm install kubernetes/framework/charts/ingress-per-domain --name ${domainNamespace}-ingress --namespace ${domainNamespace} \
                    --set wlsDomain.domainUID=${domainName} \
                    --set wlsDomain.domainType=${domainType} \
                    --set wlsDomain.adminServerName=${yamlUtility.domainInputsMap.get("adminServerName")} \
                    --set wlsDomain.clusterName=${yamlUtility.domainInputsMap.get("clusterName")} \
                    --set wlsDomain.adminServerPort=${yamlUtility.domainInputsMap.get("adminPort")} \
                    --set wlsDomain.managedServerPort=${yamlUtility.domainInputsMap.get("managedServerPort")} \
                    --set traefik.hostname=fmwk8s.us.oracle.com"
            Log.info(script, "configure domain loadbalancer success.")
        }
        catch (exc) {
            Log.error(script, "configure domain loadbalancer failed.")
            throw exc
        }
    }

    static createNamespace() {
        try {
            Log.info(script, "begin create domain namespace.")

            script.sh "kubectl create ns ${domainNamespace}"

            Log.info(script, "create domain namespace success.")
        }
        catch (exc) {
            Log.error(script, "create domain namespace failed.")
            throw exc
        }
        finally {
            Log.info(script, "initialize helm.")

            script.sh "helm init --client-only --skip-refresh --wait"
        }
    }

    static cleanDomain() {
        try {
            script.sh "helm delete --purge ${domainNamespace}-ingress"
        }
        catch (exc) {
            Log.error(script, "cleanup domain ingress failed.")
        }
        finally {
            sleep 30
        }

        try {
            script.sh "kubectl delete jobs --all -n ${domainNamespace} && \
                       kubectl delete services --all -n ${domainNamespace} && \
                       kubectl delete pods --all -n ${domainNamespace}"
        }
        catch (exc) {
            Log.error(script, "cleanup domain pods and services failed.")
        }
        finally {
            sleep 30
        }

        try {
            script.sh "kubectl delete configmaps --all -n ${domainNamespace}"
            script.sh "kubectl delete statefulsets --all -n ${domainNamespace}"
        }
        catch (exc) {
            Log.error(script, "cleanup domain configmap and stateful sets failed.")
        }
        finally {
            sleep 30
        }

        try {
            script.sh "kubectl delete domain ${domainName} -n ${domainNamespace}"
        }
        catch (exc) {
            Log.error(script, "cleanup domain resource failed.")
        }
        finally {
            sleep 30
        }

        try {
            script.sh "kubectl delete pvc ${domainName}-${domainNamespace}-pvc -n ${domainNamespace}"
            sleep 180
            script.sh "kubectl delete pv ${domainName}-${domainNamespace}-pv -n ${domainNamespace}"
        }
        catch (exc) {
            Log.error(script, "cleanup domain persistent volume failed.")
        }
        finally {
            sleep 60
        }
    }

    static cleanDomainNamespace() {
        try {
            script.sh "kubectl delete ns ${domainNamespace}"
            sleep 30
        }
        catch (exc) {
            Log.error(script, "cleanup domain namespace failed.")
        }
        finally {
            try {
                script.sh "kubectl get ns ${domainNamespace} -o json | jq '.spec.finalizers=[]' > ns-without-finalizers.json && \
                       curl -k -X PUT ${k8sMasterUrl}/api/v1/namespaces/${domainNamespace}/finalize \
                               -H \"Content-Type: application/json\" --data-binary @ns-without-finalizers.json"
            }
            catch (exc) {
            }
        }
    }
}
