package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log

class Domain {
    static def weblogicUser = "weblogic"
    static def weblogicPass = "Welcome1"
    static def domainName
    static def domainNamespace
    static def weblogicCredentialsSecretName

    static pullSampleScripts(script) {
        script.git branch: "${Common.samplesBranch}",
                url: "${Common.samplesRepo}"
    }

    static configureRcuSecret(script, domainName, domainNamespace) {
        try {
            if (Common.productId != "weblogic") {
                Log.info(script, "begin configure rcu secrets.")

                script.sh "export KUBECONFIG=${script.env.KUBECONFIG}"

                script.sh "retVal=`echo \\`kubectl get secret ${domainName}-rcu-credentials -n ${domainNamespace} 2>&1\\`` &&\
                       if echo \"\$retVal\" | grep -q \"not found\"; then \n \
                          kubernetes/samples/scripts/create-rcu-credentials/create-rcu-credentials.sh -u ${domainName} -p Welcome1 -a sys -q Oradoc_db1 -d ${domainName} -n ${domainNamespace} \n \
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
            if (!productImage?.trim()) {
                productImage = Common.defaultProductImage
            }

            if (Common.productId != "weblogic" && Common.operatorVersion != "2.1") {
                Log.info(script, "begin prepare rcu.")

                script.git branch: 'master',
                        credentialsId: 'sandeep.zachariah.ssh',
                        url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

                script.sh "cd kubernetes/framework/db/rcu && \
                           sed -i \"s|%CONNECTION_STRING%|${Database.dbName}.${domainNamespace}:1521/${Database.dbName}pdb.us.oracle.com|g\" ${Common.productId}-rcu-configmap.yaml && \
                           sed -i \"s|%RCUPREFIX%|${domainName}|g\" ${Common.productId}-rcu-configmap.yaml && \
                           sed -i \"s|%SYS_PASSWORD%|Oradoc_db1|g\" ${Common.productId}-rcu-configmap.yaml && \
                           sed -i \"s|%PASSWORD%|Welcome1|g\" ${Common.productId}-rcu-configmap.yaml && \
                           cat ${Common.productId}-rcu-configmap.yaml"

                script.sh "cd kubernetes/framework/db/rcu && \
                           sed -i \"s|%DB_SECRET%|${Common.registrySecret}|g\" fmwk8s-rcu-pod.yaml && \
                           sed -i \"s|%PRODUCT_ID%|${Common.productId}|g\" fmwk8s-rcu-pod.yaml && \
                           sed -i \"s|%PRODUCT_IMAGE%|${productImage}|g\" fmwk8s-rcu-pod.yaml && \
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

            script.sh "cd kubernetes/samples/scripts/create-weblogic-domain-pv-pvc &&\
                       sed -i \"s|baseName: weblogic-sample|baseName: ${domainNamespace}|g\" create-pv-pvc-inputs.yaml && \
                       sed -i \"s|domainUID:|domainUID: ${domainName}|g\" create-pv-pvc-inputs.yaml && \
                       sed -i \"s|namespace: default|namespace: ${domainNamespace}|g\" create-pv-pvc-inputs.yaml && \
                       sed -i \"s|#weblogicDomainStoragePath: /scratch/k8s_dir|weblogicDomainStoragePath: ${nfsDomainPath}|g\" create-pv-pvc-inputs.yaml && \
                       sed -i \"s|weblogicDomainStorageReclaimPolicy: Retain|weblogicDomainStorageReclaimPolicy: Recycle|g\" create-pv-pvc-inputs.yaml && \
                       cat create-pv-pvc-inputs.yaml && \
                       ./create-pv-pvc.sh -i create-pv-pvc-inputs.yaml -o ${script.env.WORKSPACE}/script-output-directory"

            script.sh "cp ${script.env.WORKSPACE}/script-output-directory/pv-pvcs/${domainName}-${domainNamespace}-pv.yaml ${script.env.WORKSPACE} && \
                       cp ${script.env.WORKSPACE}/script-output-directory/pv-pvcs/${domainName}-${domainNamespace}-pvc.yaml ${script.env.WORKSPACE} && \
                       cat ${script.env.WORKSPACE}/${domainName}-${domainNamespace}-pv.yaml && \
                       cat ${script.env.WORKSPACE}/${domainName}-${domainNamespace}-pvc.yaml"

            script.sh "kubectl apply -f ${script.env.WORKSPACE}/${domainName}-${domainNamespace}-pv.yaml -n ${domainNamespace} && \
                       kubectl apply -f ${script.env.WORKSPACE}/${domainName}-${domainNamespace}-pvc.yaml -n ${domainNamespace} && \
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

            if (!productImage?.trim()) {
                productImage = Common.defaultProductImage
            }

            script.sh "cd kubernetes/samples/scripts/create-${Common.productId}-domain/${Common.samplesDirectory} && \
                        cp create-domain-inputs.yaml create-domain-inputs.yaml.orig && \
                        cp create-domain-job-template.yaml create-domain-job-template.yaml.orig && \
                        sed -i \"s|domainUID: domain1|domainUID: ${domainName}|g\" create-domain-inputs.yaml && \
                        sed -i \"s|domainHome: /shared/domains/domain1|domainHome: /shared/domains/${domainName}|g\" create-domain-inputs.yaml && \
                        sed -i \"s|initialManagedServerReplicas: 2|initialManagedServerReplicas: 2|g\" create-domain-inputs.yaml && \
                        sed -i \"s|image: ${Common.defaultProductImage}|image: ${productImage}|g\" create-domain-inputs.yaml && \
                        sed -i \"s|image: store/oracle/weblogic:12.2.1.3|image: ${productImage}|g\" create-domain-inputs.yaml && \
                        sed -i \"s|#imagePullSecretName:|imagePullSecretName: ${Common.registrySecret}|g\" create-domain-inputs.yaml && \
                        sed -i \"s|weblogicCredentialsSecretName: domain1-weblogic-credentials|weblogicCredentialsSecretName: ${domainName}-weblogic-credentials|g\" create-domain-inputs.yaml && \
                        sed -i \"s|logHome: /shared/logs/domain1|logHome: /shared/logs/${domainName}|g\" create-domain-inputs.yaml && \
                        sed -i \"s|namespace: default|namespace: ${domainNamespace}|g\" create-domain-inputs.yaml && \
                        sed -i \"s|persistentVolumeClaimName: domain1-weblogic-sample-pvc|persistentVolumeClaimName: ${domainName}-${domainNamespace}-pvc|g\" create-domain-inputs.yaml && \
                        sed -i \"s|rcuSchemaPrefix: domain1|rcuSchemaPrefix: ${domainName}|g\" create-domain-inputs.yaml && \
                        sed -i \"s|rcuDatabaseURL: database:1521/service|rcuDatabaseURL: ${Database.dbName}.${domainNamespace}:1521/${Database.dbName}pdb.us.oracle.com|g\" create-domain-inputs.yaml && \
                        sed -i \"s|rcuCredentialsSecret: domain1-rcu-credentials|rcuCredentialsSecret: ${domainName}-rcu-credentials|g\" create-domain-inputs.yaml && \
                        cat create-domain-inputs.yaml"

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
            script.sh "cd kubernetes/samples/scripts/create-${Common.productId}-domain/${Common.samplesDirectory} && \
                      ./create-domain.sh -i create-domain-inputs.yaml -o ${script.env.WORKSPACE}/script-output-directory"
            script.sh "cp ${script.env.WORKSPACE}/script-output-directory/weblogic-domains/${domainName}/domain.yaml ${script.env.WORKSPACE}/${domainName}-${domainNamespace}-domain.yaml && \
                       cat ${script.env.WORKSPACE}/${domainName}-${domainNamespace}-domain.yaml"
            Log.info(script, "create " + Common.productId + " domain success.")

            Log.info(script, "begin start " + Common.productId + " domain")
            script.sh "kubectl apply -f ${script.env.WORKSPACE}/${domainName}-${domainNamespace}-domain.yaml -n ${domainNamespace} && \
                       sleep 360"
            Log.info(script, "start " + Common.productId + " domain success.")

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
            script.sh "curl -o /dev/null -s -w \"%{http_code}\\n\" \"http://${domainName}-admin-server.${domainNamespace}.svc.cluster.local:7001/weblogic/ready\" | grep 200"

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
            script.sh "helm init"
        }
    }

    static cleanDomain(script, domainName, namespace) {
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
            sleep 60
            script.sh "kubectl delete pv ${domainName}-${namespace}-pv -n ${namespace}"
        }
        catch (exc) {
            Log.error(script, "cleanup domain persistent volume failed.")
        }
        finally {
            sleep 60
        }
    }

    static cleanDomainNamespace(script, namespace) {
        try {
            script.sh "kubectl delete ns ${namespace}"
            sleep 30
            script.sh "kubectl get ns ${namespace} -o json | jq '.spec.finalizers=[]' > ns-without-finalizers.json && \
                       curl -k -X PUT https://fmwk8s.us.oracle.com:6443/api/v1/namespaces/${namespace}/finalize \
                               -H \"Content-Type: application/json\" --data-binary @ns-without-finalizers.json"
        }
        catch (exc) {
            Log.error(script, "cleanup domain namespace failed.")
        }
    }
}
