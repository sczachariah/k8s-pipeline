package com.oracle.fmwk8s.common

class EnvironmentSetup {

    static createNfsFolder(script, namespace) {
        try {

            script.git branch: 'master',
                    credentialsId: 'sandeep.zachariah.ssh',
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh "export KUBECONFIG=${script.env.KUBECONFIG} \
                        cd kubernetes/framework \
                        sed -i \"s#%FMWK8S_NFS_HOME%#${script.env.FMWK8S_NFS_HOME}#g\" fmwk8s-mkdir-pod.yaml \
                        sed -i \"s#%NFS_DOMAIN_DIR%#${script.env.NFS_DOMAIN_DIR}#g\" fmwk8s-mkdir-pod.yaml \
                        cat fmwk8s-mkdir-pod.yaml \
                        kubectl apply -f fmwk8s-mkdir-pod.yaml -n ${namespace}"  \

        }
        catch (exc) {
            Log.error(script, "Create NFS folder failed!!")
        }
        finally {
        }
    }

    static deleteNfsFolder(script) {}
}
