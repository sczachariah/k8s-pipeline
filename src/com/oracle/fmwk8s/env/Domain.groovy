package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.utility.YamlUtility

class Domain {
    static def yamlUtility = new YamlUtility()
    static def weblogicUser = "weblogic"
    static def weblogicPass = "Welcome1"
    static def domainName
    static def domainNamespace
    static def weblogicCredentialsSecretName
    static def createdomainPodName

    static pullSampleScripts(script) {
        script.git branch: "${Common.samplesBranch}",
                credentialsId: 'sandeep.zachariah.ssh',
                url: "${Common.samplesRepo}"
    }

    static configureRcuSecret(script, domainName, domainNamespace) {
        try {
            if (Common.productId != "weblogic") {
                Log.info(script, "begin configure rcu secrets.")

                script.sh "export KUBECONFIG=${script.env.KUBECONFIG}"

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

    static preparRcu(script, productImage, domainName, domainNamespace) {
        try {
            if (productImage?.trim()) {
                Common.productImage = productImage
            }

            if (Common.productId != "weblogic" && Common.operatorVersion != "2.1") {
                Log.info(script, "begin prepare rcu.")

                script.git branch: 'master',
                        credentialsId: 'sandeep.zachariah.ssh',
                        url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

                script.sh "cd kubernetes/framework/db/rcu && \
                           sed -i \"s|%CONNECTION_STRING%|${Database.dbName}.${domainNamespace}:${Database.dbPort}/${Database.dbName}pdb.us.oracle.com|g\" ${Common.productId}-rcu-configmap.yaml && \
                           sed -i \"s|%RCUPREFIX%|${domainName}|g\" ${Common.productId}-rcu-configmap.yaml && \
                           sed -i \"s|%SYS_PASSWORD%|${Database.dbPassword}|g\" ${Common.productId}-rcu-configmap.yaml && \
                           sed -i \"s|%PASSWORD%|Welcome1|g\" ${Common.productId}-rcu-configmap.yaml && \
                           cat ${Common.productId}-rcu-configmap.yaml"

                script.sh "cd kubernetes/framework/db/rcu && \
                           sed -i \"s|%DB_SECRET%|${Common.registrySecret}|g\" fmwk8s-rcu-pod.yaml && \
                           sed -i \"s|%PRODUCT_ID%|${Common.productId}|g\" fmwk8s-rcu-pod.yaml && \
                           sed -i \"s|%PRODUCT_IMAGE%|${Common.productImage}|g\" fmwk8s-rcu-pod.yaml && \
                           cat fmwk8s-rcu-pod.yaml"

                script.sh "export KUBECONFIG=${script.env.KUBECONFIG} && \
                           kubectl apply -f kubernetes/framework/db/rcu/${Common.productId}-rcu-configmap.yaml -n ${domainNamespace} && \
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
                           
                Log.info(script, "begin fetch rcu pod logs.")
                Logging.getPodLogs(script, 'fmwk8s-rcu', domainNamespace)
                Log.info(script, "fetch rcu pod logs success.")
                
                Log.info(script, "prepare rcu success.")
            }
        }
        catch (exc) {
            Log.error(script, "prepare rcu failed.")
            throw exc
        }
    }

    static configureDomainSecret(script, domainName, domainNamespace) {
        try {
            Log.info(script, "begin configure domain secrets.")

            this.weblogicCredentialsSecretName = "${domainName}-weblogic-credentials"

            script.sh "export KUBECONFIG=${script.env.KUBECONFIG}"

            script.sh "retVal=`echo \\`kubectl get secret ${this.weblogicCredentialsSecretName} -n ${domainNamespace} 2>&1\\`` &&\
                       if echo \"\$retVal\" | grep -q \"not found\"; then \n \
                          kubernetes/samples/scripts/create-weblogic-domain-credentials/create-weblogic-credentials.sh -u ${this.weblogicUser} -p ${this.weblogicPass} -n ${domainNamespace} -d ${domainName} \n \
                       fi"

            Log.info(script, "configure domain secrets success.")

        }
        catch (exc) {
            Log.error(script, "configure domain secrets failed.")
            throw exc
        }
    }

    static preparePersistentVolume(script, domainName, domainNamespace, nfsDomainPath) {
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

    static prepareDomain(script, productImage, domainName, domainNamespace) {
        try {
            Log.info(script, "begin prepare domain.")

            if (productImage?.trim()) {
                Common.productImage = productImage
            }

            script.sh "cp -r kubernetes/samples/scripts/create-${Common.productId}-domain/${Common.samplesDirectory}/create-domain-inputs.yaml create-domain-inputs && \
                       cp -r kubernetes/samples/scripts/create-${Common.productId}-domain/${Common.samplesDirectory}/create-domain-job-template.yaml create-domain-job-template && \
                       ls -ltr . && cat create-domain-inputs"

            yamlUtility.generateDomainInputsYaml(script, domainName, domainNamespace, "create-domain-inputs")

            script.sh "cat create-domain-inputs.yaml"

            Log.info(script, "prepare domain success.")

        }
        catch (exc) {
            Log.error(script, "prepare domain failed.")
            throw exc
        }
    }

    static createDomain(script, domainName, domainNamespace) {
        try {
            this.domainName = domainName
            this.domainNamespace = domainNamespace

            Log.info(script, "begin create " + Common.productId + " domain.")
            script.sh "./kubernetes/samples/scripts/create-${Common.productId}-domain/${Common.samplesDirectory}/create-domain.sh -i create-domain-inputs.yaml -o script-output-directory"
            script.sh "mkdir -p ${domainName}-${domainNamespace} && \
                       ls -ltr script-output-directory/weblogic-domains/ && \
                       cp -r script-output-directory/weblogic-domains/${domainName}/domain.yaml domain && \
                       cp -r script-output-directory/weblogic-domains/${domainName}/domain.yaml domain" + Common.productId + ""
            Log.info(script, "create " + Common.productId + " domain success.")
            
            this.createdomainPodName = script.sh(
                    script: "kubectl get pods -o go-template --template \'{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}\' -n ${domainNamespace} | grep ${domainName}-create",
                    returnStdout: true
            ).trim()
            Log.info(script, "begin fetch create domain job pod logs.")
            Logging.getPodLogs(script, this.createdomainPodName, domainNamespace)
            Log.info(script, "fetch create domain job pod logs success.")

            Log.info(script, "begin start " + Common.productId + " domain")
            yamlUtility.generateDomainYaml(script, Common.productId, "domain")
            script.sh "ls -ltr && cat domain*"
            script.sh "kubectl apply -f domain.yaml -n ${domainNamespace} && \
                       sleep 480"
            if ("${Common.productId}" == "oim") {
                script.sh "kubectl apply -f domain" + Common.productId + ".yaml -n ${domainNamespace} && \
                           sleep 480000"
            }
            Log.info(script, "start " + Common.productId + " domain success.")

            isDomainReady(script, domainName, domainNamespace)

        }
        catch (exc) {
            Log.error(script, "create/start " + Common.productId + " domain failed.")
            throw exc
        }
    }

    static isDomainReady(script, domainName, domainNamespace) {
        try {
            Log.info(script, "begin domain readiness check.")

            script.sh "kubectl get all,domains -n ${domainNamespace}"
            script.sh "kubectl get domain -n ${domainNamespace} | grep ${domainName}"
            script.sh "curl -o /dev/null -s -w \"%{http_code}\\n\" \"http://${domainName}-${yamlUtility.domainInputsMap.get("adminServerName")}.${domainNamespace}.svc.cluster.local:${yamlUtility.domainInputsMap.get("adminPort")}/weblogic/ready\" | grep 200"

            Log.info(script, "domain readiness check success.")

        }
        catch (exc) {
            Log.error(script, "domain readiness check failed.")
        }
    }

    static configureDomainLoadBalancer(script, domainName, domainNamespace) {
        try {
            Log.info(script, "begin configure domain loadbalancer.")
            script.sh "helm install kubernetes/samples/charts/ingress-per-domain --name ${domainNamespace}-ingress --namespace ${domainNamespace} \
                    --set wlsDomain.domainUID=${domainName} --set traefik.hostname=fmwk8s.us.oracle.com"
            Log.info(script, "configure domain loadbalancer success.")
        }
        catch (exc) {
            Log.error(script, "configure domain loadbalancer failed.")
            throw exc
        }
    }

    static createNamespace(script, namespace) {
        try {
            Log.info(script, "begin create domain namespace.")

            script.sh "export KUBECONFIG=${script.env.KUBECONFIG}"
            script.sh "kubectl create ns ${namespace}"

            Log.info(script, "create domain namespace success.")
        }
        catch (exc) {
            Log.error(script, "create domain namespace failed.")
            throw exc
        }
        finally {
            Log.info(script, "initialize helm.")

            script.sh "export KUBECONFIG=${script.env.KUBECONFIG}"
            script.sh "helm init --client-only --skip-refresh --wait"
        }
    }

    static cleanDomain(script, domainName, namespace) {
        try {
            script.sh "helm delete --purge ${namespace}-ingress"
        }
        catch (exc) {
            Log.error(script, "cleanup domain ingress failed.")
        }
        finally {
            sleep 30
        }

        try {
            script.sh "kubectl delete jobs --all -n ${namespace} && \
                       kubectl delete services --all -n ${namespace} && \
                       kubectl delete pods --all -n ${namespace}"
        }
        catch (exc) {
            Log.error(script, "cleanup domain pods and services failed.")
        }
        finally {
            sleep 30
        }

        try {
            script.sh "kubectl delete configmaps --all -n ${namespace}"
            script.sh "kubectl delete statefulsets --all -n ${namespace}"
        }
        catch (exc) {
            Log.error(script, "cleanup domain configmap and stateful sets failed.")
        }
        finally {
            sleep 30
        }

        try {
            script.sh "kubectl delete domain ${domainName} -n ${namespace}"
        }
        catch (exc) {
            Log.error(script, "cleanup domain resource failed.")
        }
        finally {
            sleep 30
        }

        try {
            script.sh "kubectl delete pvc ${domainName}-${namespace}-pvc -n ${namespace}"
            sleep 120
            script.sh "kubectl delete pv ${domainName}-${namespace}-pv -n ${namespace}"
        }
        catch (exc) {
            Log.error(script, "cleanup domain persistent volume failed.")
        }
        finally {
            sleep 120
        }
    }

    static cleanDomainNamespace(script, namespace) {
        try {
            script.sh "kubectl delete ns ${namespace}"
            sleep 30
        }
        catch (exc) {
            Log.error(script, "cleanup domain namespace failed.")
        }
        finally {
            script.sh "kubectl get ns ${namespace} -o json | jq '.spec.finalizers=[]' > ns-without-finalizers.json && \
                       curl -k -X PUT https://fmwk8s.us.oracle.com:6443/api/v1/namespaces/${namespace}/finalize \
                               -H \"Content-Type: application/json\" --data-binary @ns-without-finalizers.json"
        }
    }
}
