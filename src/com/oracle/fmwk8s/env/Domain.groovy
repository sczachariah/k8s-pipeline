package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Base
import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.utility.K8sUtility
import com.oracle.fmwk8s.utility.YamlUtility

/**
 * Domain class handles the common domain operations that are required
 * in E2E execution of FMW in Docker/K8S environments
 */
class Domain extends Base{
    static def yamlUtility = new YamlUtility()
    static def K8sUtility = new K8sUtility()
    static def weblogicUser = "weblogic"
    static def weblogicPass = "Welcome1"
    static def domainName
    static def domainNamespace
    static def weblogicCredentialsSecretName
    static def createdomainPodName
    static def adminServerPodName
    static def replicaCount
    static def managedServerPodName

    static pullSampleScripts(script) {
        script.git branch: "${Common.samplesBranch}",
                credentialsId: 'sandeep.zachariah.ssh',
                url: "${Common.samplesRepo}"
    }

    static configureRcuSecret(script) {
        try {
            if (Common.productId != "weblogic") {
                Log.info(script, "begin configure rcu secrets.")

                script.sh "retVal=`echo \\`kubectl get secret ${Base.DOMAIN_NAME}-rcu-credentials -n ${Base.DOMAIN_NS} 2>&1\\`` &&\
                       if echo \"\$retVal\" | grep -q \"not found\"; then \n \
                          kubernetes/samples/scripts/create-rcu-credentials/create-rcu-credentials.sh -u ${Base.DOMAIN_NAME} -p Welcome1 -a sys -q ${Database.dbPassword} -d ${Base.DOMAIN_NAME} -n ${Base.DOMAIN_NS} \n \
                       fi"

                Log.info(script, "configure rcu secrets success.")
            }
        }
        catch (exc) {
            Log.error(script, "configure rcu secrets failed.")
            throw exc
        }
    }

    static preparRcu(script) {
        try {
            if (Base.productImage?.trim()) {
                Common.productImage = Base.productImage
            }

            if (Common.productId != "weblogic" && Common.operatorVersion != "2.1") {
                Log.info(script, "begin prepare rcu.")

                script.git branch: 'master',
                        credentialsId: 'sandeep.zachariah.ssh',
                        url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

                script.sh "cd kubernetes/framework/db/rcu && \
                           sed -i \"s|%CONNECTION_STRING%|${Database.dbName}.${Base.DOMAIN_NS}:${Database.dbPort}/${Database.dbName}pdb.us.oracle.com|g\" ${Common.productId}-rcu-configmap.yaml && \
                           sed -i \"s|%RCUPREFIX%|${Base.DOMAIN_NAME}|g\" ${Common.productId}-rcu-configmap.yaml && \
                           sed -i \"s|%SYS_PASSWORD%|${Database.dbPassword}|g\" ${Common.productId}-rcu-configmap.yaml && \
                           sed -i \"s|%PASSWORD%|Welcome1|g\" ${Common.productId}-rcu-configmap.yaml && \
                           cat ${Common.productId}-rcu-configmap.yaml"

                script.sh "cd kubernetes/framework/db/rcu && \
                           sed -i \"s|%DB_SECRET%|${Common.registrySecret}|g\" fmwk8s-rcu-pod.yaml && \
                           sed -i \"s|%PRODUCT_ID%|${Common.productId}|g\" fmwk8s-rcu-pod.yaml && \
                           sed -i \"s|%PRODUCT_IMAGE%|${Common.productImage}|g\" fmwk8s-rcu-pod.yaml && \
                           cat fmwk8s-rcu-pod.yaml"

                script.sh "kubectl apply -f kubernetes/framework/db/rcu/${Common.productId}-rcu-configmap.yaml -n ${Base.DOMAIN_NS} && \
                           kubectl apply -f kubernetes/framework/db/rcu/fmwk8s-rcu-pod.yaml -n ${Base.DOMAIN_NS}"

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
                           rcustat=`echo \\`kubectl get pods -n ${Base.DOMAIN_NS} 2>&1 | grep fmwk8s-rcu\\``\n \
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
            Logging.getPodLogs(script, 'fmwk8s-rcu', Base.DOMAIN_NAME)
            Log.info(script, "fetch rcu pod logs success.")
        }
    }

    static configureDomainSecret(script) {
        try {
            Log.info(script, "begin configure domain secrets.")

            this.weblogicCredentialsSecretName = "${Domain.DOMAIN_NAME}-weblogic-credentials"

            script.sh "retVal=`echo \\`kubectl get secret ${this.weblogicCredentialsSecretName} -n ${Domain.DOMAIN_NS} 2>&1\\`` &&\
                       if echo \"\$retVal\" | grep -q \"not found\"; then \n \
                          kubernetes/samples/scripts/create-weblogic-domain-credentials/create-weblogic-credentials.sh -u ${this.weblogicUser} -p ${this.weblogicPass} -n ${Base.DOMAIN_NS} -d ${Base.DOMAIN_NAME} \n \
                       fi"

            Log.info(script, "configure domain secrets success.")

        }
        catch (exc) {
            Log.error(script, "configure domain secrets failed.")
            throw exc
        }
    }

    static preparePersistentVolume(script) {
        try {
            Log.info(script, "begin prepare persistent volume.")

            script.sh "cp -r kubernetes/samples/scripts/create-weblogic-domain-pv-pvc/create-pv-pvc-inputs.yaml create-pv-pvc-inputs && \
                       ls -ltr . && cat create-pv-pvc-inputs"

            yamlUtility.generatePeristentVolumeInputsYaml(script, Base.DOMAIN_NAME, Base.DOMAIN_NS, Base.NFS_DOMAIN_PATH, "create-pv-pvc-inputs")

            script.sh "cat create-pv-pvc-inputs.yaml && \
                       ./kubernetes/samples/scripts/create-weblogic-domain-pv-pvc/create-pv-pvc.sh -i create-pv-pvc-inputs.yaml -o script-output-directory"

            script.sh "cp script-output-directory/pv-pvcs/${Base.DOMAIN_NAME}-${Base.DOMAIN_NS}-pv.yaml . && \
                       cp script-output-directory/pv-pvcs/${Base.DOMAIN_NAME}-${Base.DOMAIN_NS}-pvc.yaml . && \
                       cat ${Base.DOMAIN_NAME}-${Base.DOMAIN_NS}-pv.yaml && \
                       cat ${Base.DOMAIN_NAME}-${Base.DOMAIN_NS}-pvc.yaml"

            script.sh "kubectl apply -f ${Base.DOMAIN_NAME}-${Base.DOMAIN_NS}-pv.yaml -n ${Base.DOMAIN_NS} && \
                       kubectl apply -f ${Base.DOMAIN_NAME}-${Base.DOMAIN_NS}-pvc.yaml -n ${Base.DOMAIN_NS} && \
                       kubectl describe pv ${Base.DOMAIN_NAME}-${Base.DOMAIN_NS}-pv -n ${Base.DOMAIN_NS} && \
                       kubectl describe pvc ${Base.DOMAIN_NAME}-${Base.DOMAIN_NS}-pvc -n ${Base.DOMAIN_NS}"

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

            if (Base.productImage?.trim()) {
                Common.productImage = Base.productImage
            }

            script.sh "cp -r kubernetes/samples/scripts/create-${Common.productId}-domain/${Common.samplesDirectory}/create-domain-inputs.yaml create-domain-inputs && \
                       cp -r kubernetes/samples/scripts/create-${Common.productId}-domain/${Common.samplesDirectory}/create-domain-job-template.yaml create-domain-job-template && \
                       ls -ltr . && cat create-domain-inputs"

            yamlUtility.generateDomainInputsYaml(script, script.env.DOMAIN_TYPE, Base.DOMAIN_NAME, Base.DOMAIN_NS, "create-domain-inputs")

            script.sh "cat create-domain-inputs.yaml"

            Log.info(script, "prepare domain success.")

        }
        catch (exc) {
            Log.error(script, "prepare domain failed.")
            throw exc
        }
    }

    static createDomain(script) {
        try {
            this.domainName = Base.DOMAIN_NAME
            this.domainNamespace = Base.DOMAIN_NS

            Log.info(script, "begin create " + Common.productId + " domain.")
            script.sh "./kubernetes/samples/scripts/create-${Common.productId}-domain/${Common.samplesDirectory}/create-domain.sh -i create-domain-inputs.yaml -o script-output-directory"
            script.sh "mkdir -p ${Base.DOMAIN_NAME}-${Base.DOMAIN_NS} && \
                       ls -ltr script-output-directory/weblogic-domains/ && \
                       cp -r script-output-directory/weblogic-domains/${Base.DOMAIN_NAME}/domain.yaml domain && \
                       cp -r script-output-directory/weblogic-domains/${Base.DOMAIN_NAME}/domain.yaml domain" + Common.productId + ""
            Log.info(script, "create " + Common.productId + " domain success.")

            Log.info(script, "begin start " + Common.productId + " domain")
            yamlUtility.generateDomainYaml(script, Common.productId, "domain")
            script.sh "ls -ltr && cat domain*"
            script.sh "kubectl apply -f domain.yaml -n ${Base.DOMAIN_NS}"
            if ("${Common.productId}" == "oim") {
                script.sh "kubectl apply -f domain" + Common.productId + ".yaml -n ${Base.DOMAIN_NS} && \
                           sleep 480000"
            }
            Log.info(script, "start " + Common.productId + " domain success.")

            isDomainReady(script)

        }
        catch (exc) {
            Log.error(script, "create/start " + Common.productId + " domain failed.")
            throw exc
        }
        finally {
            this.createdomainPodName = script.sh(
                    script: "kubectl get pods -o go-template --template \'{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}\' -n ${Base.DOMAIN_NS} | grep ${Base.DOMAIN_NAME}-create",
                    returnStdout: true
            ).trim()
            Log.info(script, "begin fetch create domain job pod logs.")
            Logging.getPodLogs(script, this.createdomainPodName, Base.DOMAIN_NS)
            Log.info(script, "fetch create domain job pod logs success.")
        }
    }

    static isDomainReady(script) {
        try {
            Log.info(script, "begin domain readiness check.")

            script.sh "kubectl get all,domains -n ${Base.DOMAIN_NS}"
            Log.info(script, "begin Admin server status check")
            this.adminServerPodName = "${Base.DOMAIN_NAME}-${yamlUtility.domainInputsMap.get("adminServerName")}"
            Log.info(script, this.adminServerPodName)
            K8sUtility.checkPodStatus(script, this.adminServerPodName, Base.DOMAIN_NS, 20)
            Log.info(script, "admin server status check completed.")
            Log.info(script, "begin Managed server status check.")
            script.sh "kubectl get domain -n ${Base.DOMAIN_NS} -o yaml > ${Base.DOMAIN_NAME}-domain.yaml && \
                       ls"
            this.replicaCount = script.sh(
                    script: "cat ${Base.DOMAIN_NAME}-domain.yaml | grep replicas:|tail -1|awk -F':' '{print \$2}'",
                    returnStdout: true
            ).trim()
            Log.info(script, this.replicaCount)
            for (int i = 1; i <= Integer.parseInt(this.replicaCount); i++) {
                this.managedServerPodName = "${Base.DOMAIN_NAME}-${yamlUtility.domainInputsMap.get("managedServerNameBase")}${i}"
                K8sUtility.checkPodStatus(script, this.managedServerPodName, Base.DOMAIN_NS, 20)
            }
            Log.info(script, "managed server status check completed.")
            Log.info(script, "domain readiness check success.")
        }
        catch (exc) {
            Log.error(script, "domain readiness check failed.")
        }
    }

    static configureDomainLoadBalancer(script) {
        try {
            Log.info(script, "begin configure domain loadbalancer.")
            script.sh "helm install kubernetes/samples/charts/ingress-per-domain --name ${Base.DOMAIN_NS}-ingress --namespace ${Base.DOMAIN_NS} \
                    --set wlsDomain.domainUID=${Base.DOMAIN_NAME} --set traefik.hostname=fmwk8s.us.oracle.com"
            Log.info(script, "configure domain loadbalancer success.")
        }
        catch (exc) {
            Log.error(script, "configure domain loadbalancer failed.")
            throw exc
        }
    }

    static createNamespace(script) {
        try {
            Log.info(script, "begin create domain namespace.")

            script.sh "kubectl create ns ${Base.DOMAIN_NS}"

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

    static cleanDomain(script) {
        try {
            script.sh "helm delete --purge ${Base.DOMAIN_NS}-ingress"
        }
        catch (exc) {
            Log.error(script, "cleanup domain ingress failed.")
        }
        finally {
            sleep 30
        }

        try {
            script.sh "kubectl delete jobs --all -n ${Base.DOMAIN_NS} && \
                       kubectl delete services --all -n ${Base.DOMAIN_NS} && \
                       kubectl delete pods --all -n ${Base.DOMAIN_NS}"
        }
        catch (exc) {
            Log.error(script, "cleanup domain pods and services failed.")
        }
        finally {
            sleep 30
        }

        try {
            script.sh "kubectl delete configmaps --all -n ${Base.DOMAIN_NS}"
            script.sh "kubectl delete statefulsets --all -n ${Base.DOMAIN_NS}"
        }
        catch (exc) {
            Log.error(script, "cleanup domain configmap and stateful sets failed.")
        }
        finally {
            sleep 30
        }

        try {
            script.sh "kubectl delete domain ${Base.DOMAIN_NAME} -n ${Base.DOMAIN_NS}"
        }
        catch (exc) {
            Log.error(script, "cleanup domain resource failed.")
        }
        finally {
            sleep 30
        }

        try {
            script.sh "kubectl delete pvc ${Base.DOMAIN_NAME}-${Base.DOMAIN_NS}-pvc -n ${Base.DOMAIN_NS}"
            sleep 180
            script.sh "kubectl delete pv ${Base.DOMAIN_NAME}-${Base.DOMAIN_NS}-pv -n ${Base.DOMAIN_NS}"
        }
        catch (exc) {
            Log.error(script, "cleanup domain persistent volume failed.")
        }
        finally {
            sleep 60
        }
    }

    static cleanDomainNamespace(script) {
        try {
            script.sh "kubectl delete ns ${Base.DOMAIN_NS}"
            sleep 30
        }
        catch (exc) {
            Log.error(script, "cleanup domain namespace failed.")
        }
        finally {
            try {
                script.sh "kubectl get ns ${Base.DOMAIN_NS} -o json | jq '.spec.finalizers=[]' > ns-without-finalizers.json && \
                       curl -k -X PUT ${Common.k8sMasterUrl}/api/v1/namespaces/${Base.DOMAIN_NS}/finalize \
                               -H \"Content-Type: application/json\" --data-binary @ns-without-finalizers.json"
            }
            catch (exc) {
            }
        }
    }
}
