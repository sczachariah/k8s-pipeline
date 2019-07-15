package com.oracle.fmwk8s.test

class Functional {
    static invokeTest(script, testType, testImage) {
        switch ("${testType}") {
            case "URL_VALIDATION":
                UrlValidation.invokeTest(script, testImage)
                break
            case "OPERATOR_INTEGRATION":
                OperatorIntegration.invokeTest(script, testImage)
                break
            case "MATS":
                Mats.invokeTest(script, testImage)
                break
        }
    }
}
