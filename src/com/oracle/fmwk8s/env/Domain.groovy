package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Log

class Domain {
    static configureDomainSecret(script, domainName, namespace) {
        try {
            Log.info(script, "configure domain secrets !!!")
            script.sh "export KUBECONFIG=${script.env.KUBECONFIG} && \
                       sleep 120"
            script.sh "retVal=`echo \\`kubectl get secret ${domainName}-weblogic-credentials -n ${namespace} 2>&1\\`` &&\
                       if echo \"\$retVal\" | grep -q \"not found\"; then \n \
                          kubernetes/samples/scripts/create-soa-domain-credentials/create-domain-credentials.sh -u weblogic -p Welcome1 -n ${namespace} -d ${domainName} \n \
                       fi"

            Log.info(script, "Configure Domain secrets Completed!!!")

        }
        catch (exc) {
            Log.error(script, "Configure Domain secrets failed!!.")
        }
    }

    static preparePersistentVolume(script, domainName, namespace) {
        try {
            Log.info(script, "Prepare persisitent volume !!!")
            script.sh "cd kubernetes/samples/scripts/create-soa-domain-pv-pvc &&\
                       cp create-pv-pvc-inputs.yaml create-pv-pvc-inputs.yaml.orig &&\
                       sed -i \"s#baseName: domain#baseName: ${namespace}#g\" create-pv-pvc-inputs.yaml && \
                       sed -i \"s#domainUID: soainfra#domainUID: ${domainName}#g\" create-pv-pvc-inputs.yaml && \
                       sed -i \"s#namespace: soans#namespace: ${namespace}#g\" create-pv-pvc-inputs.yaml && \
                       sed -i \"s#weblogicDomainStoragePath: /scratch/DockerVolume/SOA#weblogicDomainStoragePath: ${script.env.NFS_DOMAIN_PATH}#g\" create-pv-pvc-inputs.yaml && \
                       sed -i \"s#weblogicDomainStorageReclaimPolicy: Retain#weblogicDomainStorageReclaimPolicy: Recycle#g\" create-pv-pvc-inputs.yaml && \
                       cat create-pv-pvc-inputs.yaml && \
                       ./create-pv-pvc.sh -i create-pv-pvc-inputs.yaml -o ${script.env.WORKSPACE}/soa-operator-output-directory && \
                       cp ${script.env.WORKSPACE}/soa-operator-output-directory/pv-pvcs/${domainName}-${namespace}-pv.yaml ${script.env.WORKSPACE} && \
                       cp ${script.env.WORKSPACE}/soa-operator-output-directory/pv-pvcs/${domainName}-${namespace}-pvc.yaml ${script.env.WORKSPACE} && \
                       cat ${script.env.WORKSPACE}/${domainName}-${namespace}-pv.yaml && \
                       cat ${script.env.WORKSPACE}/${domainName}-${namespace}-pvc.yaml && \
                       kubectl apply -f ${script.env.WORKSPACE}/${domainName}-${namespace}-pv.yaml -n ${namespace} && \
                       kubectl apply -f ${script.env.WORKSPACE}/${domainName}-${namespace}-pvc.yaml -n ${namespace} && \
                       kubectl describe pv ${domainName}-${namespace}-pv -n ${namespace} && \
                       kubectl describe pvc ${domainName}-${namespace}-pvc -n ${namespace}"

            Log.info(script, "Prepare persistent volume Completed!!!")

        }
        catch (exc) {
            Log.error(script, "Prepare persistent volume failed!!.")
        }
    }

    static prepareDomain(script, domainName, namespace) {
        try {
            Log.info(script, "Prepare Domain !!!")
            script.sh "cd kubernetes/samples/scripts/create-soa-domain/domain-home-on-pv/multiple-Managed-servers && \
                       cp create-domain-inputs.yaml create-domain-inputs.yaml.orig && \
                       cp create-domain-job-template.yaml create-domain-job-template.yaml.orig && \
                       sed -i \"s#domainUID: soainfra#domainUID: ${domainName}#g\" create-domain-inputs.yaml && \
                       sed -i \"s#domainHome: /u01/oracle/user_projects/domains/soainfra#domainHome: /u01/oracle/user_projects/domains/${domainName}#g\" create-domain-inputs.yaml && \
                       sed -i \"s#weblogicCredentialsSecretName: soainfra-domain-credentials#weblogicCredentialsSecretName: ${domainName}-weblogic-credentials#g\" create-domain-inputs.yaml && \
                       sed -i \"s#image: oracle/soa:12.2.1.3#image: container-registry.oracle.com/middleware/soasuite:12.2.1.3#g\" create-domain-inputs.yaml && \
                       sed -i \"s/#imagePullSecretName:/imagePullSecretName: regcred/g\" create-domain-inputs.yaml && \
                       sed -i \"s#logHome: /u01/oracle/user_projects/domains/logs/soainfra#logHome: /u01/oracle/user_projects/domains/logs/${domainName}#g\" create-domain-inputs.yaml && \
                       sed -i \"s#namespace: soans#namespace: ${namespace}#g\" create-domain-inputs.yaml && \
                       sed -i \"s#persistentVolumeClaimName: soainfra-domain-pvc#persistentVolumeClaimName: ${domainName}-${namespace}-pvc#g\" create-domain-inputs.yaml && \
                       sed -i \"s#initialManagedServerReplicas: 2#initialManagedServerReplicas: 1#g\" create-domain-inputs.yaml && \
                       cat create-domain-inputs.yaml && \
                       sed -i \"s#soadb:1521#soadb.${namespace}:1521#g\" create-domain-job-template.yaml && \
                       cat create-domain-job-template.yaml"
            Log.info(script, "Prepare Domain Completed!!!")

        }
        catch (exc) {
            Log.error(script, "Prepare Domain failed!!.")
        }
    }

    static createDomain(script, domainName, namespace) {
        try {
            Log.info(script, "Create Domain !!!")
            script.sh "cd kubernetes/samples/scripts/create-soa-domain/domain-home-on-pv/multiple-Managed-servers && \
                      ./create-domain.sh -i create-domain-inputs.yaml -o ${script.env.WORKSPACE}/soa-operator-output-directory && \
                      cp ${script.env.WORKSPACE}/soa-operator-output-directory/soa-domains/${domainName}/domain.yaml ${script.env.WORKSPACE} && \
                      cat ${script.env.WORKSPACE}/domain.yaml"
            Log.info(script, "Start Domain !!!")
            script.sh "kubectl apply -f ${script.env.WORKSPACE}/domain.yaml && \
                       sleep 360"
            Log.info(script, "Create Domain Completed!!!")

        }
        catch (exc) {
            Log.error(script, "Create Domain failed!!.")
        }
    }

    static isDomainReady(script, domainName, namespace) {
        try {
            Log.info(script, "begin domain readiness check.")

            script.sh "kubectl get all,domains -n ${namespace}"

            Log.info(script, "domain readiness check success.")

        }
        catch (exc) {
            Log.error(script, "domain readiness check failed.")
        }
    }

    static createNamespace(script, namespace) {
        try {
            Log.info(script, "create domain namespace!!")
            script.sh "export KUBECONFIG=${script.env.KUBECONFIG}"
            script.sh "kubectl create ns ${namespace}"
        }
        catch (exc) {
            Log.error(script, "Create Domain namespace failed!!.")
        }
        finally {
            Log.info(script, "initialize helm!!")
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
            Log.error(script, "Cleanup domain pods and services failed!!")
        }
        finally {
            sleep 10
        }

        try {
            script.sh "kubectl delete configmaps --all -n ${namespace}"
            script.sh "kubectl delete statefulsets --all -n ${namespace}"
        }
        catch (exc) {
            Log.error(script, "Cleanup domain configmap and stateful sets failed!!")
        }
        finally {
            sleep 30
        }

        try {
            script.sh "kubectl delete domain ${domainName} -n ${namespace}"
        }
        catch (exc) {
            Log.error(script, "Cleanup domain resource failed!!")
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
            Log.error(script, "Cleanup domain persistent volume failed!!")
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
            Log.error(script, "Cleanup domain namespace failed!!")
        }
    }
}
