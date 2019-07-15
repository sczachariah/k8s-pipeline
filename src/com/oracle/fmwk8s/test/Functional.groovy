package com.oracle.fmwk8s.test

class Functional {
    static invokeTest(script, testType, testNamespace) {
        switch ("${testType}") {
            case "URL_VALIDATION":
                UrlValidation.runTests(script, testNamespace)
                break
            case "OPERATOR_INTEGRATION":
                OperatorIntegration.runTests(script, testNamespace)
                break
        }
    }
}
