package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.env.Domain
import com.oracle.fmwk8s.env.Operator
import com.oracle.fmwk8s.utility.YamlUtility

class OperatorIntegration {
    static def yamlUtility = new YamlUtility()

    static invokeTest(script, mavenProfile) {
        fetchSource(script)
        createTestProps(script)
        runTests(script, mavenProfile)
        publishResults(script)
    }

    static fetchSource(script) {
        script.git branch: 'master',
                credentialsId: 'sandeep.zachariah.ssh',
                url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-operator-intg-tests.git'
    }

    static createTestProps(script) {
        try {
            Log.info(script, "begin create test properties.")

            script.sh "cd config && \
                       sed -i \"s|\\\${PRODUCT_NAME}|${Common.productName}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${OPERATOR_NS}|${Operator.operatorNamespace}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${OPERATOR_SA}|${Operator.operatorServiceAccount}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${DOMAIN_NAME}|${Domain.domainName}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${DOMAIN_NS}|${Domain.domainNamespace}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${CLUSTER_NAME}|${yamlUtility.domainInputsMap.get("clusterName")}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${MANAGED_SERVER_NAME_BASE}|${yamlUtility.domainInputsMap.get("managedServerNameBase")}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${ADMIN_SERVER_NAME}|${yamlUtility.domainInputsMap.get("adminServerName")}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${WEBLOGIC_CREDENTIALS_SECRET_NAME}|${Domain.weblogicCredentialsSecretName}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${OPERATOR_HELM_RELEASE}|${Operator.operatorHelmRelease}|g\" operatorTest.properties && \
                       cat operatorTest.properties"

            Log.info(script, "create test properties success.")
        }
        catch (exc) {
            Log.error(script, "create test properties failed.")
        }
        finally {
        }
    }


    static runTests(script, mavenProfile) {
        try {
            Log.info(script, "begin run operator integration tests.")

            script.sh "echo Maven command:  && \
                       echo \"mvn clean verify -P ${mavenProfile} -DoperatorTest.properties=config/operatorTest.properties -DproxySet=true -DproxyHost=www-proxy.us.oracle.com -DproxyPort=80\" && \
                       mvn clean verify -P ${mavenProfile} -DoperatorTest.properties=config/operatorTest.properties -DproxySet=true -DproxyHost=www-proxy.us.oracle.com -DproxyPort=80"

            Log.info(script, "run operator integration tests success.")
        }
        catch (exc) {
            Log.error(script, "run operator integration tests failed.")
        }
        finally {
        }
    }

    static waitForTests(script) {}

    static publishResults(script) {}

    static cleanTests(script) {}
}
