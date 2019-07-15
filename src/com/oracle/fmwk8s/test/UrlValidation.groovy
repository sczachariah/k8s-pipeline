package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Log

class UrlValidation {
    static runTests(script, testNamespace) {
        createTestProps(script, testNamespace)
    }

    static createTestProps(script, testNamespace) {
        try {
            Log.info(script, "begin create test props.")

            script.sh "cat <<EOF > ${script.env.WORKSPACE}/test.props \
SELENIUM_HUB_HOST=selenium-standalone-firefox.${testNamespace} \
SELENIUM_HUB_PORT=4444 \
EOF && \
            cat ${WORKSPACE}/test.props"

            Log.info(script, "create test props success.")
        }
        catch (exc) {
            Log.error(script, "create test props failed.")
        }
        finally {
        }
    }

    static waitForTests(script) {}

    static publishResults(script) {}

    static cleanTests(script) {}
}
