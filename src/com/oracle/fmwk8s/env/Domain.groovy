package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.test.Test
import com.oracle.fmwk8s.utility.K8sUtility
import com.oracle.fmwk8s.utility.ReportUtility
import com.oracle.fmwk8s.utility.YamlUtility

/**
 * Domain class handles the common domain operations that are required
 * in E2E execution of FMW in Docker/K8S environments
 */
class Domain extends Common {
    static def weblogicCredentialsSecretName
    static def createDomainJobName
    static def adminServerPodName
    static def managedServerPodName
    static def replicaCount

    static pullSampleScripts() {
        script.git branch: "${samplesBranch}",
                credentialsId: "${sshCredentialId}",
                url: "${samplesRepo}"
    }

    static configureRcuSecret() {
        try {
            if (productId != "weblogic") {
                Log.info("begin configure rcu secrets.")

                script.sh label: "create rcu credentials",
                        script: "retVal=`echo \\`kubectl get secret ${domainName}-rcu-credentials -n ${domainNamespace} 2>&1\\`` &&\
                       if echo \"\$retVal\" | grep -q \"not found\"; then \n \
                          kubernetes/samples/scripts/create-rcu-credentials/create-rcu-credentials.sh -u ${domainName} -p ${Database.dbSchemaPassword} -a sys -q ${Database.dbPassword} -d ${domainName} -n ${domainNamespace} \n \
                       fi"

                Log.info("configure rcu secrets success.")
            }
        }
        catch (exc) {
            Log.error("configure rcu secrets failed.")
            throw exc
        }
    }

    static preparRcu() {
        try {
            if (productId != "weblogic" && operatorVersion != "2.1") {
                Log.info("begin prepare rcu.")

                script.git branch: 'master',
                        credentialsId: "${sshCredentialId}",
                        url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

                script.sh label: "create rcu silent script configmap",
                        script: "cd kubernetes/framework/db/rcu && \
                           sed -i \"s|%CONNECTION_STRING%|${Database.dbName}.${domainNamespace}:${Database.dbPort}/${Database.dbName}pdb.us.oracle.com|g\" fmwk8s-rcu-configmap.yaml && \
                           sed -i \"s|%RCUPREFIX%|${domainName}|g\" fmwk8s-rcu-configmap.yaml && \
                           sed -i \"s|%SYS_PASSWORD%|${Database.dbPassword}|g\" fmwk8s-rcu-configmap.yaml && \
                           sed -i \"s|%PASSWORD%|${Database.dbSchemaPassword}|g\" fmwk8s-rcu-configmap.yaml && \
                           sed -i \"s|%PRODUCT_ID%|${productId}|g\" fmwk8s-rcu-configmap.yaml && \
                           cat fmwk8s-rcu-configmap.yaml"

                script.sh label: "prepare pod for running rcu",
                        script: "cd kubernetes/framework/db/rcu && \
                           sed -i \"s|%DB_SECRET%|${registrySecret}|g\" fmwk8s-rcu-pod.yaml && \
                           sed -i \"s|%PRODUCT_ID%|${productId}|g\" fmwk8s-rcu-pod.yaml && \
                           sed -i \"s|%PRODUCT_IMAGE%|${productImage}|g\" fmwk8s-rcu-pod.yaml && \
                           cat fmwk8s-rcu-pod.yaml"

                script.sh label: "create pod to run rcu",
                        script: "kubectl apply -f kubernetes/framework/db/rcu/fmwk8s-rcu-configmap.yaml -n ${domainNamespace} && \
                           kubectl apply -f kubernetes/framework/db/rcu/fmwk8s-rcu-pod.yaml -n ${domainNamespace}"

                K8sUtility.checkPodStatus(script, 'fmwk8s-rcu', domainNamespace, 25, 'Completed')

                Log.info("prepare rcu success.")
            }
        }
        catch (exc) {
            Log.error("prepare rcu failed.")
            throw exc
        }
        finally {
            Log.info("begin fetch rcu pod logs.")
            Logging.getPodLogs('fmwk8s-rcu', domainNamespace)
            Log.info("fetch rcu pod logs success.")
        }
    }

    static configureDomainSecret() {
        try {
            Log.info("begin configure domain secrets.")

            weblogicCredentialsSecretName = "${domainName}-weblogic-credentials"

            script.sh label: "create domain credentials",
                    script: "retVal=`echo \\`kubectl get secret ${weblogicCredentialsSecretName} -n ${domainNamespace} 2>&1\\`` &&\
                       if echo \"\$retVal\" | grep -q \"not found\"; then \n \
                          kubernetes/samples/scripts/create-weblogic-domain-credentials/create-weblogic-credentials.sh -u ${weblogicUsername} -p ${weblogicPassword} -n ${domainNamespace} -d ${domainName} \n \
                       fi"

            Log.info("configure domain secrets success.")

        }
        catch (exc) {
            Log.error("configure domain secrets failed.")
            throw exc
        }
    }

    static preparePersistentVolume() {
        try {
            Log.info("begin prepare persistent volume.")

            script.sh label: "backup domain pv/pvc files",
                    script: "cp -r kubernetes/samples/scripts/create-weblogic-domain-pv-pvc/create-pv-pvc-inputs.yaml create-pv-pvc-inputs && \
                       ls -ltr . && cat create-pv-pvc-inputs"

            yamlUtility.generatePeristentVolumeInputsYaml(script, domainName, domainNamespace, nfsDomainPath, "create-pv-pvc-inputs")

            script.sh label: "prepare domain pv/pvc yaml",
                    script: "cat create-pv-pvc-inputs.yaml && \
                       ./kubernetes/samples/scripts/create-weblogic-domain-pv-pvc/create-pv-pvc.sh -i create-pv-pvc-inputs.yaml -o script-output-directory"

            script.sh label: "verify domain pv/pvc yaml",
                    script: "cp script-output-directory/pv-pvcs/${domainName}-${domainNamespace}-pv.yaml . && \
                       cp script-output-directory/pv-pvcs/${domainName}-${domainNamespace}-pvc.yaml . && \
                       cat ${domainName}-${domainNamespace}-pv.yaml && \
                       cat ${domainName}-${domainNamespace}-pvc.yaml"

            script.sh label: "create domain pv/pvc",
                    script: "kubectl apply -f ${domainName}-${domainNamespace}-pv.yaml -n ${domainNamespace} && \
                       kubectl apply -f ${domainName}-${domainNamespace}-pvc.yaml -n ${domainNamespace} && \
                       kubectl describe pv ${domainName}-${domainNamespace}-pv -n ${domainNamespace} && \
                       kubectl describe pvc ${domainName}-${domainNamespace}-pvc -n ${domainNamespace}"

            Log.info("prepare persistent volume success.")

        }
        catch (exc) {
            Log.error("prepare persistent volume failed.")
            throw exc
        }
    }

    static prepareDomain() {
        try {
            Log.info("begin prepare domain.")

            if (domainType.toString().equalsIgnoreCase("N/A")) {
                domainType = "weblogic"
            }

            script.sh label: "backup domain input files",
                    script: "cp -r kubernetes/samples/scripts/create-${productId}-domain/${samplesDirectory}/create-domain-inputs.yaml create-domain-inputs && \
                       cp -r kubernetes/samples/scripts/create-${productId}-domain/${samplesDirectory}/create-domain-job-template.yaml create-domain-job-template && \
                       ls -ltr . && cat create-domain-inputs"

            yamlUtility.generateDomainInputsYaml(script, domainType, domainName, domainNamespace, "create-domain-inputs")

            script.sh label: "verify domain inputs yaml",
                    script: "cat create-domain-inputs.yaml"

            Log.info("prepare domain success.")

        }
        catch (exc) {
            Log.error("prepare domain failed.")
            throw exc
        }
    }

    static createDomain() {
        try {
            this.domainName = domainName
            this.domainNamespace = domainNamespace

            // create
            Log.info("begin create " + productId + " domain.")
            script.sh label: "create domain",
                    script: "./kubernetes/samples/scripts/create-${productId}-domain/${samplesDirectory}/create-domain.sh -i create-domain-inputs.yaml -o script-output-directory"
            script.sh label: "prepare domain yaml file",
                    script: "mkdir -p ${domainName}-${domainNamespace} && \
                       ls -ltr script-output-directory/weblogic-domains/ && \
                       cp -r script-output-directory/weblogic-domains/${domainName}/domain.yaml domain && \
                       cp -r script-output-directory/weblogic-domains/${domainName}/domain.yaml domain" + productId + ""
            Log.info("create " + productId + " domain success.")

            yamlUtility.generateDomainYaml(script, productId, "domain")
        }
        catch (exc) {
            Log.error("create " + productId + " domain failed.")
            throw exc
        }
        finally {
            createDomainJobName = script.sh(
                    script: "kubectl get jobs -o go-template --template \'{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}\' -n ${domainNamespace} | grep ${domainName}-create",
                    returnStdout: true
            ).trim()
            Log.info("begin fetch create domain job pod logs.")
            Logging.getPodLogs("job/${createDomainJobName}", domainNamespace)
            Log.info("fetch create domain job pod logs success.")
        }
    }

    static customizeDomain() {
        try {
            // customize
            Log.info("begin customize " + productId + " domain.")
            Operator.setDomainNamespace()
            script.sh label: "create domain yaml configmap",
                    script: "kubectl create configmap fmwk8s-domain-yaml --from-file=create-domain-inputs.yaml --from-file=domain.yaml -n ${domainNamespace}"
            script.sh label: "customize domain",
                    script: "cd ../fmwk8s/kubernetes/framework/ && \
                        sed -i \"s|%FMWK8S_NFS_HOME%|${fmwk8sNfsHome}|g\" fmwk8s-customize-domain-pod.yaml && \
                        sed -i \"s|%LB_HOST%|${k8sMasterIP}|g\" fmwk8s-customize-domain-pod.yaml && \
                        sed -i \"s|%LB_PORT%|${IngressController.httplbPort}|g\" fmwk8s-customize-domain-pod.yaml && \
                        sed -i \"s|%DOMAIN_PVC%|${domainName}-${domainNamespace}-pvc|g\" fmwk8s-customize-domain-pod.yaml && \
                        cat fmwk8s-customize-domain-pod.yaml && \
                        kubectl apply -f fmwk8s-customize-domain-pod.yaml -n ${domainNamespace}"
            K8sUtility.checkPodStatus(script, 'fmwk8s-customize-domain', domainNamespace, 60, 'Completed')
            Log.info("customize " + productId + " domain success.")
        }
        catch (exc) {
            Log.error("customize " + productId + " domain failed.")
            throw exc
        }
        finally {
            Log.info("begin fetch customize domain pod logs.")
            Logging.getPodLogs("fmwk8s-customize-domain", domainNamespace)
            Log.info("fetch customize domain pod logs success.")
        }
    }

    static startDomain() {
        try {
            // start
            Log.info("begin start " + productId + " domain")
            yamlUtility.generateDomainYaml(script, productId, "domain")
            script.sh label: "verify domain yaml",
                    script: "ls -ltr && cat domain*"
            script.sh label: "apply domain yaml",
                    script: "kubectl apply -f domain.yaml -n ${domainNamespace}"
            if ("${productId}" == "oim") {
                script.sh label: "apply domain yaml",
                        script: "kubectl apply -f domain" + productId + ".yaml -n ${domainNamespace} && \
                           sleep 480000"
            }
            Log.info("start " + productId + " domain success.")

            // fix operator not managing the domain intermittently
            Operator.setDomainNamespace()
            sleep 150
            isDomainReady()
        }
        catch (exc) {
            Log.error("start " + productId + " domain failed.")
            throw exc
        }
        finally {
            ReportUtility.printDomainUrls(script)
            ReportUtility.sendNotificationMailPostDomainCreation(script)
        }
    }

    static isDomainReady() {
        try {
            Log.info("begin domain readiness check.")

            script.sh "kubectl get all,domains -n ${domainNamespace}"
            Log.info("begin admin server status check")
            adminServerPodName = "${domainName}-${yamlUtility.domainInputsMap.get("adminServerName").toString().replaceAll("_", "-")}"
            Log.info(adminServerPodName)
            K8sUtility.checkPodStatus(script, adminServerPodName, domainNamespace, 40, '1/1')
            Log.info("admin server status check completed.")

            Log.info("begin managed server status check.")
            script.sh "kubectl get domain -n ${domainNamespace} -o yaml > ${domainName}-domain.yaml && \
                       ls"
            Map<Object, Object> map = YamlUtility.readYaml(script, "${domainName}-domain.yaml")

            for (Object key : map.keySet()) {
                if (key.equals("items")) {
                    LinkedHashMap items = map.get("items")
                    for (Object item : items.keySet()) {
                        if (item.equals("spec")) {
                            LinkedHashMap specs = items.get("spec")
                            for (Object spec : specs.keySet()) {
                                if (spec.equals("clusters")) {
                                    List clusters = specs.get("clusters")
                                    for (LinkedHashMap cluster : clusters) {
                                        replicaCount = cluster.get("replicas")
                                        Log.info("replicaCount :: ${replicaCount}")
                                        for (int i = 1; i <= replicaCount; i++) {
                                            managedServerPodName = "${domainName}-${yamlUtility.domainInputsMap.get("managedServerNameBase").toString().replaceAll("_", "-")}${i}"
                                            K8sUtility.checkPodStatus(script, managedServerPodName, domainNamespace, 40, '1/1')
                                        }
                                        Log.info("managed server status check completed.")
                                    }
                                }
                            }
                        }
                    }
                }

            }
            Log.info("domain readiness check success.")
        }
        catch (exc) {
            Log.error("domain readiness check failed.")
            throw exc
        }
    }

    static configureDomainLoadBalancer() {
        try {
            Log.info("begin configure domain loadbalancer.")
            script.git branch: 'master',
                    credentialsId: "${sshCredentialId}",
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            if (!("${lbType}".equalsIgnoreCase("APACHE"))) {
                script.sh label: "create domain ingress rules",
                        script: "helm install kubernetes/framework/charts/ingress-per-domain --name ${domainNamespace}-ingress --namespace ${domainNamespace} \
                    --set type=${lbType} \
                    --set wlsDomain.domainUID=${domainName} \
                    --set wlsDomain.productID=${productId} \
                    --set wlsDomain.domainType=${domainType} \
                    --set wlsDomain.adminServerName=${yamlUtility.domainInputsMap.get("adminServerName")} \
                    --set wlsDomain.clusterName=${yamlUtility.domainInputsMap.get("clusterName")} \
                    --set wlsDomain.adminServerPort=${yamlUtility.domainInputsMap.get("adminPort")} \
                    --set wlsDomain.managedServerPort=${yamlUtility.domainInputsMap.get("managedServerPort")} \
                    --set traefik.hostname=fmwk8s.us.oracle.com"
            } else {
                Log.info("There is no ingress for APACHE LB type")
            }
            Log.info("configure domain loadbalancer success.")
        }
        catch (exc) {
            Log.error("configure domain loadbalancer failed.")
            throw exc
        }
    }

    static createNamespace() {
        try {
            Log.info("begin create domain namespace.")

            script.sh label: "create domain namespace",
                    script: "kubectl create ns ${domainNamespace}"

            Log.info("create domain namespace success.")
        }
        catch (exc) {
            Log.error("create domain namespace failed.")
            throw exc
        }
        finally {
            Log.info("initialize helm.")

            script.sh label: "initialize helm",
                    script: "helm init --client-only --skip-refresh --wait"
        }
    }

    static cleanDomain() {
        try {
            script.sh label: "clean domain ingress rules",
                    script: "helm delete --purge ${domainNamespace}-ingress"
            script.sh label: "delete helm configmap",
                    script: "kubectl delete cm -n kube-system --selector=NAME=${domainNamespace}-ingress"
            script.sh label: "delete lb cluster roles",
                    script: "kubectl delete clusterroles --selector=release=${lbHelmRelease}"
        }
        catch (exc) {
            Log.error("cleanup domain ingress failed.")
        }
        finally {
            sleep 30
        }

        try {
            script.sh label: "cleanup jobs/services/pods",
                    script: "kubectl delete jobs --all -n ${domainNamespace} --cascade && \
                       kubectl delete services --all -n ${domainNamespace} --cascade && \
                       kubectl delete deployment --all -n ${domainNamespace} --cascade && \
                       kubectl delete pods --all -n ${domainNamespace} --grace-period=0 --force --cascade"
        }
        catch (exc) {
            Log.error("cleanup domain pods and services failed.")
        }
        finally {
            sleep 30
        }

        try {
            script.sh label: "cleanup configmap",
                    script: "kubectl delete configmaps --all -n ${domainNamespace} --cascade"
            script.sh label: "cleanup statefulsets",
                    script: "kubectl delete statefulsets --all -n ${domainNamespace} --grace-period=0 --force --cascade"
        }
        catch (exc) {
            Log.error("cleanup domain configmap and stateful sets failed.")
        }
        finally {
            sleep 30
        }

        try {
            script.sh label: "cleanup domain",
                    script: "kubectl delete domain ${domainName} -n ${domainNamespace} --cascade"
        }
        catch (exc) {
            Log.error("cleanup domain resource failed.")
        }
        finally {
            sleep 30
        }

        try {
            script.sh label: "cleanup domain pvc",
                    script: "kubectl delete pvc ${domainName}-${domainNamespace}-pvc -n ${domainNamespace} --grace-period=0 --force --cascade && \
                             kubectl delete pvc --all -n ${domainNamespace} --grace-period=0 --force --cascade"
            sleep 180
            script.sh label: "cleanup domain pv",
                    script: "kubectl delete pv ${domainName}-${domainNamespace}-pv --grace-period=0 --force --cascade"
        }
        catch (exc) {
            Log.error("cleanup domain persistent volume failed.")
        }
        finally {
            sleep 60
        }
    }

    static cleanDomainNamespace() {
        try {
            script.sh label: "cleanup domain namespace",
                    script: "kubectl delete ns ${domainNamespace} --grace-period=0 --force --cascade"
            sleep 30
        }
        catch (exc) {
            Log.error("cleanup domain namespace failed.")
        }
        finally {
            try {
                script.sh label: "finalize domain namespace",
                        script: "kubectl get ns ${domainNamespace} -o json | jq '.spec.finalizers=[]' > ns-without-finalizers.json && \
                       curl -k -X PUT ${k8sMasterUrl}/api/v1/namespaces/${domainNamespace}/finalize \
                               -H \"Content-Type: application/json\" --data-binary @ns-without-finalizers.json"
            }
            catch (exc) {
            }
        }
    }
}
