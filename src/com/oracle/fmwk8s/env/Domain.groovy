package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log

class Domain {
    static pullSampleScripts(script) {
        script.git branch: 'master',
                url: '' + Common.samplesRepo
    }

    static configureDomainSecret(script, domainName, domainNamespace) {
        try {
            Log.info(script, "begin configure domain secrets.")

            script.sh "export KUBECONFIG=${script.env.KUBECONFIG}"

            script.sh "retVal=`echo \\`kubectl get secret ${domainName}-weblogic-credentials -n ${domainNamespace} 2>&1\\`` &&\
                       if echo \"\$retVal\" | grep -q \"not found\"; then \n \
                          kubernetes/samples/scripts/create-" + Common.productId + "-domain-credentials/create-domain-credentials.sh -u weblogic -p Welcome1 -n ${domainNamespace} -d ${domainName} \n \
                       fi"

            Log.info(script, "configure domain secrets success.")

        }
        catch (exc) {
            Log.error(script, "configure domain secrets failed.")
        }
    }

    static preparePersistentVolume(script, domainName, domainNamespace, nfsDomainPath) {
        try {
            Log.info(script, "begin prepare persistent volume.")

            script.sh "cd kubernetes/samples/scripts/create-" + Common.productId + "-domain-pv-pvc &&\
                       sed -i \"s#baseName: domain#baseName: ${domainNamespace}#g\" create-pv-pvc-inputs.yaml && \
                       sed -i \"s#domainUID: soainfra#domainUID: ${domainName}#g\" create-pv-pvc-inputs.yaml && \
                       sed -i \"s#namespace: soans#namespace: ${domainNamespace}#g\" create-pv-pvc-inputs.yaml && \
                       sed -i \"s#weblogicDomainStoragePath: /scratch/DockerVolume/SOA#weblogicDomainStoragePath: ${nfsDomainPath}#g\" create-pv-pvc-inputs.yaml && \
                       sed -i \"s#weblogicDomainStorageReclaimPolicy: Retain#weblogicDomainStorageReclaimPolicy: Recycle#g\" create-pv-pvc-inputs.yaml && \
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
        }
    }

    static prepareDomain(script, domainName, domainNamespace, productImage) {
        try {
            Log.info(script, "begin prepare domain.")

            script.sh "cd kubernetes/samples/scripts/create-" + Common.productId + "-domain/domain-home-on-pv/multiple-Managed-servers && \
                       cp create-domain-inputs.yaml create-domain-inputs.yaml.orig && \
                       cp create-domain-job-template.yaml create-domain-job-template.yaml.orig && \
                       sed -i \"s#domainUID: soainfra#domainUID: ${domainName}#g\" create-domain-inputs.yaml && \
                       sed -i \"s#domainHome: /u01/oracle/user_projects/domains/soainfra#domainHome: /u01/oracle/user_projects/domains/${domainName}#g\" create-domain-inputs.yaml && \
                       sed -i \"s#weblogicCredentialsSecretName: soainfra-domain-credentials#weblogicCredentialsSecretName: ${domainName}-weblogic-credentials#g\" create-domain-inputs.yaml && \
                       sed -i \"s#image: oracle/soa:12.2.1.3#image: ${productImage}#g\" create-domain-inputs.yaml && \
                       sed -i \"s/#imagePullSecretName:/imagePullSecretName: ${Database.dbSecret}/g\" create-domain-inputs.yaml && \
                       sed -i \"s#logHome: /u01/oracle/user_projects/domains/logs/soainfra#logHome: /u01/oracle/user_projects/domains/logs/${domainName}#g\" create-domain-inputs.yaml && \
                       sed -i \"s#namespace: soans#namespace: ${domainNamespace}#g\" create-domain-inputs.yaml && \
                       sed -i \"s#persistentVolumeClaimName: soainfra-domain-pvc#persistentVolumeClaimName: ${domainName}-${domainNamespace}-pvc#g\" create-domain-inputs.yaml && \
                       sed -i \"s#initialManagedServerReplicas: 2#initialManagedServerReplicas: 1#g\" create-domain-inputs.yaml && \
                       cat create-domain-inputs.yaml && \
                       sed -i \"s#soadb:1521#${Database.dbName}.${domainNamespace}:1521#g\" create-domain-job-template.yaml && \
                       cat create-domain-job-template.yaml"

            Log.info(script, "prepare domain success.")

        }
        catch (exc) {
            Log.error(script, "prepare domain failed.")
        }
    }

    static createDomain(script, domainName, domainNamespace) {
        try {
            Log.info(script, "begin create " + Common.productId + " domain.")
            script.sh "cd kubernetes/samples/scripts/create-" + Common.productId + "-domain/domain-home-on-pv/multiple-Managed-servers && \
                      ./create-domain.sh -i create-domain-inputs.yaml -o ${script.env.WORKSPACE}/script-output-directory"
            script.sh "cp ${script.env.WORKSPACE}/script-output-directory/" + Common.productId + "-domains/${domainName}/domain.yaml ${script.env.WORKSPACE} && \
                       cat ${script.env.WORKSPACE}/domain.yaml"
            Log.info(script, "create " + Common.productId + " domain success.")

            Log.info(script, "begin start " + Common.productId + " domain")
            script.sh "kubectl apply -f ${script.env.WORKSPACE}/domain.yaml -n ${domainNamespace} && \
                       sleep 360"
            Log.info(script, "start " + Common.productId + " domain success.")

        }
        catch (exc) {
            Log.error(script, "create/start " + Common.productId + " domain failed.")
        }
    }

    static isDomainReady(script, domainName, domainNamespace) {
        try {
            Log.info(script, "begin domain readiness check.")

            script.sh "kubectl get all,domains -n ${domainNamespace}"
            script.sh "kubectl get domain -n ${domainNamespace} | grep ${domainName}"
            script.sh "curl -v \"http://${domainName}-adminserver.${domainNamespace}.svc.cluster.local:7001/weblogic/ready\" | grep 200"

            Log.info(script, "domain readiness check success.")

        }
        catch (exc) {
            Log.error(script, "domain readiness check failed.")
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
            sleep 10
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
            sleep 10
            script.sh "kubectl delete pv ${domainName}-${namespace}-pv -n ${namespace}"
        }
        catch (exc) {
            Log.error(script, "cleanup domain persistent volume failed.")
        }
        finally {
            sleep 10
        }
    }

    static cleanDomainNamespace(script, namespace) {
        try {
            script.sh "kubectl delete ns ${namespace}"
        }
        catch (exc) {
            Log.error(script, "cleanup domain namespace failed.")
        }
    }
}
