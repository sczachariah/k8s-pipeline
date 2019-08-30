package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Common

class Functional {
    static logDirectory

    static invokeTest(script, testType, testImage) {
        logDirectory = "/logs/${Common.runId}"

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
