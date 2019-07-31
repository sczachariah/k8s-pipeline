package com.oracle.fmwk8s.common

import java.text.SimpleDateFormat

class Common {
    static def operatorVersion
    static def operatorBranch
    static def operatorImageVersion

    static def productId
    static def productName
    static def productImage
    static def registrySecret = "regcred"
    static def denRegistrySecret = "denregcred"


    static def domainName

    static def samplesRepo
    static def samplesBranch
    static def samplesDirectory

    static def elasticSearchHost = "elasticsearch.logging.svc.cluster.local"
    static def elasticSearchPort = "9200"

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
            case "OIG":
                domainName = "oim"
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
                productImage = "container-registry.oracle.com/middleware/weblogic:12.2.1.3"
                break
            case "WLS-INFRA":
                productId = "fmw-infrastructure"
                productImage = "container-registry.oracle.com/middleware/fmw-infrastructure:12.2.1.3"
                break
            case "SOA":
                productId = "soa"
                productImage = "container-registry.oracle.com/middleware/soasuite:12.2.1.3"
                break
            case "OIG":
                productId = "oim"
                productImage = " fmw-paas-sandbox-cert-docker/oracle/oig:12.2.1.4.0-190725.1317.317"
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
            case "OIG":
                samplesRepo = "git@orahub.oraclecorp.com:idm/oim-kubernetes-operator.git"
                samplesDirectory = "domain-home-on-pv/single-Managed-server"
            default:
                samplesRepo = "unknown"
                samplesDirectory = "unknown"
                break
        }
    }

    static configureRegistrySecret(script, namespace, registryUsername, registryPass) {
        try {
            Log.info(script, "begin configure registry secret.")


            script.sh "export KUBECONFIG=${script.env.KUBECONFIG} && \
                       retVal=`echo \\`kubectl get secret ${registrySecret} -n ${namespace} 2>&1\\`` &&\
                       if echo \"\$retVal\" \\| grep -q 'not found'; then\n \
                          kubectl create secret docker-registry ${registrySecret} -n ${namespace} --docker-server=http://container-registry.oracle.com --docker-username='${registryUsername}' --docker-password='${registryPass}' --docker-email='${registryUsername}'\n \
                       fi"
            script.sh "denRetVal=`echo \\`kubectl get secret ${denRegistrySecret} -n ${namespace} 2>&1\\`` &&\
                       if echo \"\$denRetVal\" \\| grep -q 'not found'; then\n \
                          kubectl create secret docker-registry ${denRegistrySecret} -n ${namespace} --docker-server=http://fmwk8s-dev.dockerhub-den.oraclecorp.com --docker-username='${registryUsername}' --docker-password='${registryPass}' --docker-email='${registryUsername}'\n \
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
