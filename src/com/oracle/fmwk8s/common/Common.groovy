package com.oracle.fmwk8s.common

import java.text.SimpleDateFormat

class Common extends Base {

    static def getUniqueId() {
        def date = new Date()
        def sdf = new SimpleDateFormat("MMddHHmm")

        def buildNumber = "${script.env.BUILD_NUMBER}"
        runId = buildNumber + "-" + sdf.format(date)

        getKubernetesMasterUrl()
    }

    static configureRegistrySecret() {
        try {
            Log.info(script, "begin configure registry secret.")


            script.sh "retVal=`echo \\`kubectl get secret ${registrySecret} -n ${domainNamespace} 2>&1\\`` &&\
                       if echo \"\$retVal\" \\| grep -q 'not found'; then\n \
                          kubectl create secret docker-registry ${registrySecret} -n ${domainNamespace} --docker-server=http://container-registry.oracle.com --docker-username='${registryAuthUsr}' --docker-password='${registryAuthPsw}' --docker-email='${registryAuthUsr}'\n \
                       fi"
            script.sh "denRetVal=`echo \\`kubectl get secret ${denRegistrySecret} -n ${domainNamespace} 2>&1\\`` &&\
                       if echo \"\$denRetVal\" \\| grep -q 'not found'; then\n \
                          kubectl create secret docker-registry ${denRegistrySecret} -n ${domainNamespace} --docker-server=http://fmwk8s-dev.dockerhub-den.oraclecorp.com --docker-username='${registryAuthUsr}' --docker-password='${registryAuthPsw}' --docker-email='${registryAuthUsr}'\n \
                       fi"

            Log.info(script, "configure registry secret success.")
        }
        catch (exc) {
            Log.error(script, "configure registry secret failed.")
            throw exc
        }
    }

    static publishLogs() {
        try {
            Log.info(script, "begin publish logs.")

            script.echo "Reports directory: ${script.env.WORKSPACE}/test-output"
            script.env.DEPLOY_BUILD_DATE = script.sh(returnStdout: true, script: "date -u +'%Y-%m-%d-%H%M'").trim()

            def logContent = jenkins.model.Jenkins.getInstance()
                    .getItemByFullName(script.env.JOB_NAME)
                    .getBuildByNumber(Integer.parseInt(script.env.BUILD_NUMBER))
                    .logFile.text
            script.writeFile file: "buildlog-${script.env.BUILD_NUMBER}-${script.env.DEPLOY_BUILD_DATE}.txt", text: logContent

//        script.zip zipFile: "test-output-${script.env.BUILD_NUMBER}-${script.env.DEPLOY_BUILD_DATE}.zip", archive: true, dir: "${script.env.WORKSPACE}/test-output"
            script.rtUpload(
                    serverId: "artifactory.oraclecorp.com",
                    spec:
                            """{
                           "files": [
                             {
                                "pattern": "test-output*.zip",
                                "target": "cisystem-dev-local/com/oracle/fmwk8s/e2e/${productId}/test-reports/"
                             },
                             {
                                "pattern": "buildlog*.txt",
                                "target": "cisystem-dev-local/com/oracle/fmwk8s/e2e/${productId}/logs/"
                             }
                           ]
                        }""",
                    failNoOp: true
            )

            Log.info(script, "publish logs success.")
        }
        catch (exc) {
            Log.error(script, "publish logs failed.")
            throw exc
        }
    }

    static getKubernetesMasterUrl() {
        Log.info(script, "begin get k8s master url.")
        this.k8sMasterUrl = script.sh(
                script: "kubectl cluster-info | grep \"master is running at\" | sed \"s|.*\\ ||\"",
                returnStdout: true
        ).trim()

        Log.info(script, "k8s master url : ${k8sMasterUrl}")
        Log.info(script, "get k8s master url success.")
    }
}
