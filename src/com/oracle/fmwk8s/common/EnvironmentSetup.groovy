package com.oracle.fmwk8s.common

import com.oracle.fmwk8s.test.Test

class EnvironmentSetup {

    static def isWaiting = false
    static def kubeconfig

    static createNfsFolder(script, namespace, nfsHomeDir, nfsDomainDir) {
        try {
            Log.info(script, "begin create nfs folder.")

            script.git branch: 'master',
                    credentialsId: 'sandeep.zachariah.ssh',
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh "cd kubernetes/framework && \
                        sed -i \"s#%FMWK8S_NFS_HOME%#${nfsHomeDir}#g\" fmwk8s-mkdir-pod.yaml && \
                        sed -i \"s#%NFS_DOMAIN_DIR%#${nfsDomainDir}#g\" fmwk8s-mkdir-pod.yaml && \
                        cat fmwk8s-mkdir-pod.yaml && \
                        kubectl apply -f fmwk8s-mkdir-pod.yaml -n ${namespace}"

            Log.info(script, "create nfs folder success.")
        }
        catch (exc) {
            Log.error(script, "create nfs folder failed.")
            throw exc
        }
        finally {
        }
    }

    static mountKubeconfig(script, namespace) {
        try {
            Log.info(script, "begin mount kubeconfig.")

            script.sh "export KUBECONFIG=${script.env.KUBECONFIG} && \
                       kubectl create configmap fmwk8s-kubeconfig-configmap --from-file=kubeconfig=${script.env.KUBECONFIG} -n ${namespace}"

            Log.info(script, "mount kubeconfig success.")
        }
        catch (exc) {
            Log.error(script, "mount kubeconfig failed.")
            throw exc
        }
    }

    static deleteNfsFolder(script, namespace, nfsHomeDir, nfsDomainDir) {
        try {
            Log.info(script, "begin delete nfs folder.")

            script.git branch: 'master',
                    credentialsId: 'sandeep.zachariah.ssh',
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh "cd kubernetes/framework && \
                        sed -i \"s#%FMWK8S_NFS_HOME%#${nfsHomeDir}#g\" fmwk8s-rmdir-pod.yaml && \
                        sed -i \"s#%NFS_DOMAIN_DIR%#${nfsDomainDir}#g\" fmwk8s-rmdir-pod.yaml && \
                        cat fmwk8s-rmdir-pod.yaml && \
                        kubectl apply -f fmwk8s-rmdir-pod.yaml -n ${namespace} && \
                        sleep 60"

            Log.info(script, "delete nfs folder success.")
        }
        catch (exc) {
            Log.error(script, "delete nfs folder failed.")
        }
        finally {
        }
    }

    static waitHoursAfter(script, hoursAfter) {
        if ("${hoursAfter}" == "true") {
            if (isWaiting)
                Log.warning(script, "already in wait loop.")
            else {
                Log.info(script, "begin wait loop.")
                isWaiting = true
                script.sh "sleep 14400"
                Log.info(script, "end wait loop.")
                isWaiting = false
                Test.cleanup(script)
            }
        } else {
            Log.info(script, "skipping wait loop.")
        }
    }
}
