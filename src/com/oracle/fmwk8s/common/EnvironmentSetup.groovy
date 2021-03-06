package com.oracle.fmwk8s.common

class EnvironmentSetup extends Base {

    static def isWaiting = false
    static def kubeconfig

    static createNfsFolder() {
        try {
            Log.info("begin create nfs folder.")

            script.git branch: 'master',
                    credentialsId: "${sshCredentialId}",
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh label: "create mkdir pod",
                    script: "cd kubernetes/framework && \
                        sed -i \"s#%FMWK8S_NFS_HOME%#${fmwk8sNfsHome}#g\" fmwk8s-mkdir-pod.yaml && \
                        sed -i \"s#%NFS_DOMAIN_DIR%#${nfsDomainDir}#g\" fmwk8s-mkdir-pod.yaml && \
                        cat fmwk8s-mkdir-pod.yaml && \
                        kubectl apply -f fmwk8s-mkdir-pod.yaml -n ${domainNamespace}"

            Log.info("create nfs folder success.")
        }
        catch (exc) {
            Log.error("create nfs folder failed.")
            throw exc
        }
        finally {
        }
    }

    static mountKubeconfig(script, namespace) {
        try {
            Log.info("begin mount kubeconfig.")

            script.sh label: "create kubeconfig configmap",
                    script: "export KUBECONFIG=${script.env.KUBECONFIG} && \
                       kubectl create configmap fmwk8s-kubeconfig-configmap --from-file=kubeconfig=${script.env.KUBECONFIG} -n ${namespace}"

            Log.info("mount kubeconfig success.")
        }
        catch (exc) {
            Log.error("mount kubeconfig failed.")
            throw exc
        }
    }

    static deleteNfsFolder() {
        try {
            Log.info("begin delete nfs folder.")

            script.git branch: 'master',
                    credentialsId: "${sshCredentialId}",
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh label: "create rmdir pod",
                    script: "cd kubernetes/framework && \
                        sed -i \"s#%FMWK8S_NFS_HOME%#${fmwk8sNfsHome}#g\" fmwk8s-rmdir-pod.yaml && \
                        sed -i \"s#%NFS_DOMAIN_DIR%#${nfsDomainDir}#g\" fmwk8s-rmdir-pod.yaml && \
                        cat fmwk8s-rmdir-pod.yaml && \
                        kubectl apply -f fmwk8s-rmdir-pod.yaml -n ${domainNamespace} && \
                        sleep 60"

            Log.info("delete nfs folder success.")
        }
        catch (exc) {
            Log.error("delete nfs folder failed.")
        }
        finally {
        }
    }

    static waitHoursAfter() {
        try {
            Log.info("start wait hoursAfter.")

            if (hoursAfter > 0) {
                if (isWaiting)
                    Log.warning("already in wait loop.")
                else {
                    Log.info("begin wait loop.")
                    isWaiting = true
                    script.sh label: "sleep for ${hoursAfter}hrs",
                            script: "sleep ${hoursAfterSeconds}"
                    Log.info("end wait loop.")
                    isWaiting = false
                }
            } else {
                Log.info("skipping wait loop.")
            }

            Log.info("wait hoursAfter success.")
        }
        catch (exc) {
            Log.error("wait hoursAfter failed.")
        }
    }
}
