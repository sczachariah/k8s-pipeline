package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.env.Domain
import com.oracle.fmwk8s.utility.Tools

class UrlValidation extends Test {
    static fireTest() {
        deployTestTools(Domain.domainNamespace)
        createTestProps(Domain.domainNamespace)
    }

    static deployTestTools(testNamespace) {
        Tools.deploySelenium(script, testNamespace)
    }


    static createTestProps(testNamespace) {
        try {
            Log.info("begin create test props.")

            script.sh "cat <<EOF > ${script.env.WORKSPACE}/test.props \
SELENIUM_HUB_HOST=selenium-standalone-firefox.${testNamespace} \
SELENIUM_HUB_PORT=4444 \
EOF && \
            cat ${script.env.WORKSPACE}/test.props"

            Log.info("create test props success.")
        }
        catch (exc) {
            Log.error("create test props failed.")
        }
        finally {
        }
    }

    static waitForTests(script) {}

    static publishResults(script) {}

    static cleanTests(script) {}

}
