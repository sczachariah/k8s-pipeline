package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.env.Domain
import com.oracle.fmwk8s.env.Operator

class OperatorIntegration {
    static invokeTest(script, testImage) {
        fetchSource(script)
        createTestProps(script)
    }

    static fetchSource(script) {
        script.git branch: 'master',
                credentialsId: 'sandeep.zachariah.ssh',
                url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-operator-intg-tests.git'
    }

    static createTestProps(script) {
        script.sh "sed -i \"s|SOA_OPERATOR_NS|${Operator.operatorNamespace}|g\" config/operatorTest.properties && \
                   sed - i \"s#SOA_DOMAIN_NS#${Domain.domainNamespace}#g\" config / operatorTest.properties && \
                   sed - i \"s#SOA_DOMAIN_NAME#${Domain.domainName}#g\" config / operatorTest.properties && \
                   sed - i \"s#SOA_OPERATOR_SA#${Operator.operatorServiceAccount}#g\" config / operatorTest.properties && \
                   sed - i \"s#PRODUCT_NAME#${Common.productName}#g\" config / operatorTest.properties"
    }

    static runTests(script, testNamespace) {}

    static waitForTests(script) {}

    static publishResults(script) {}

    static cleanTests(script) {}
}
