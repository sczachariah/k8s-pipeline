package com.oracle.fmwk8s.test

class Functional {
    static invokeTest(script, testType, testImage) {
        switch ("${testType}") {
            case "URL_VALIDATION":
                UrlValidation.invokeTest(script, testImage)
                break
            case "OPERATOR_INTEGRATION_BASIC":
                OperatorIntegration.invokeTest(script, testImage, "basic-operator-tests")
                break
            case "OPERATOR_INTEGRATION_ADVANCED":
                OperatorIntegration.invokeTest(script, testImage, "advanced-operator-tests")
                break
            case "MATS":
                Mats.invokeTest(script, testImage)
                break
        }
    }
}
