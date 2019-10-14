package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Common

class Functional {
    static logDirectory

    static invokeTest(script, testType, testImage) {
        logDirectory = "/logs/${Common.runId}"

        if (testType.matches("url-validation")) {
            UrlValidation.invokeTest(script, testImage)
        } else if (testType.matches("operator-integration-.*")) {
            OperatorIntegration.invokeTest(script, testImage, testType)
        } else if (testType.matches("mats.*")) {
            Mats.invokeTest(script, testImage)
        }
    }
}
