package com.oracle.fmwk8s.common

import java.text.SimpleDateFormat

class Common {
    static def operatorVersion
    static def operatorBranch
    static def operatorImageVersion

    static def productId
    static def productName
    static def defaultProductImage
    static def registrySecret

    static def domainName

    static def samplesRepo
    static def samplesBranch
    static def samplesDirectory

    static def getUniqueId(def script, productName) {
        def date = new Date()
        def sdf = new SimpleDateFormat("MMddHHmm")

        def buildNumber = "${script.env.BUILD_NUMBER}"
        def uniqueId = buildNumber + "-" + sdf.format(date)

        this.productName = productName
        getDomainName(productName)
        getProductIdentifier(productName)
        getSamplesRepo(productName)

        return uniqueId
    }

    static def getOperatorVersions(operatorVersion) {
        switch ("${operatorVersion}") {
            case "2.1":
                this.operatorVersion = "2.1"
                operatorBranch = "release/2.1"
                operatorImageVersion = "2.1"
                samplesBranch = "release/2.1"
                break
            case "2.2":
                this.operatorVersion = "2.2"
                operatorBranch = "release/2.2"
                operatorImageVersion = "2.2.0"
                samplesBranch = "release/2.2"
                break
            case "2.2.1":
                this.operatorVersion = "2.2.1"
                operatorBranch = "release/2.2.1"
                operatorImageVersion = "2.2.1"
                samplesBranch = "release/2.2.1"
                break
            default:
                this.operatorVersion = "develop"
                operatorBranch = "develop"
                operatorImageVersion = "develop"
                samplesBranch = "develop"
                break
        }
    }

    static def getDomainName(productName) {
        switch ("${productName}") {
            case "WLS":
                domainName = "weblogic"
                break
            case "WLS-INFRA":
                domainName = "wlsinfra"
                break
            case "SOA":
                domainName = "soainfra"
                break
            default:
                domainName = "unknown"
                break
        }

        return domainName
    }

    static def getProductIdentifier(productName) {
        switch ("${productName}") {
            case "WLS":
                productId = "weblogic"
                defaultProductImage = "container-registry.oracle.com/middleware/weblogic:12.2.1.3"
                break
            case "WLS-INFRA":
                productId = "fmw-infrastructure"
                defaultProductImage = "container-registry.oracle.com/middleware/fmw-infrastructure:12.2.1.3"
                break
            case "SOA":
                productId = "soa"
                defaultProductImage = "container-registry.oracle.com/middleware/soasuite:12.2.1.3"
                break
            default:
                productId = "unknown"
                break
        }
    }

    static def getSamplesRepo(productName) {
        switch ("${productName}") {
            case "WLS":
                samplesRepo = "https://github.com/oracle/weblogic-kubernetes-operator"
                samplesDirectory = "domain-home-on-pv"
                break
            case "WLS-INFRA":
                samplesRepo = "https://github.com/oracle/weblogic-kubernetes-operator"
                samplesDirectory = ""
                break
            case "SOA":
                samplesRepo = "git@orahub.oraclecorp.com:tooling/soa-kubernetes-operator.git"
                samplesDirectory = "domain-home-on-pv/multiple-Managed-servers"
                break
            default:
                samplesRepo = "unknown"
                samplesDirectory = "unknown"
                break
        }
    }

    static configureRegistrySecret(script, namespace, registryUsername, registryPass) {
        try {
            Log.info(script, "begin configure registry secret.")

            registrySecret = "regcred"
            script.sh "export KUBECONFIG=${script.env.KUBECONFIG} && \
                       retVal=`echo \\`kubectl get secret ${registrySecret} -n ${namespace} 2>&1\\`` &&\
                       if echo \"\$retVal\" \\| grep -q 'not found'; then\n \
                          kubectl create secret docker-registry ${registrySecret} -n ${namespace} --docker-server=http://container-registry.oracle.com --docker-username='${registryUsername}' --docker-password='${registryPass}' --docker-email='${registryUsername}'\n \
                       fi"

            Log.info(script, "configure registry secret success.")
        }
        catch (exc) {
            Log.error(script, "configure registry secret failed.")
            throw exc
        }
    }

    static configureDenverRegistrySecret(script, namespace, registryUsername, registryPass) {
        try {
            Log.info(script, "begin configure registry secret.")

            registrySecret = "denregcred"
            script.sh "export KUBECONFIG=${script.env.KUBECONFIG} && \
                       retVal=`echo \\`kubectl get secret ${registrySecret} -n ${namespace} 2>&1\\`` &&\
                       if echo \"\$retVal\" \\| grep -q 'not found'; then\n \
                          kubectl create secret docker-registry ${registrySecret} -n ${namespace} --docker-server=http://fmwk8s-dev.dockerhub-den.oraclecorp.com --docker-username='${registryUsername}' --docker-password='${registryPass}' --docker-email='${registryUsername}'\n \
                       fi"

            Log.info(script, "configure registry secret success.")
        }
        catch (exc) {
            Log.error(script, "configure registry secret failed.")
            throw exc
        }
    }

    static publishLogs(script) {
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
                                "target": "cisystem-dev-local/com/oracle/fmwk8s/e2e/${this.productId}/test-reports/"
                             },
                             {
                                "pattern": "buildlog*.txt",
                                "target": "cisystem-dev-local/com/oracle/fmwk8s/e2e/${this.productId}/logs/"
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
}
