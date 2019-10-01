package com.oracle.fmwk8s.common

class Validation {

    static def validateInputs(script, operatorVersion, productName, domainType, databaseVersion, testImageTag, testType, hoursAfter) {
        try {
            Log.info(script, "begin validate inputs.")
            validateOperatorVersion(script, operatorVersion, productName)
            validateDomainType(script, domainType, productName)
            manadatoryDatabaseVersion(script, databaseVersion)
            validateTestimageTag(script, testImageTag, testType)
            validateHoursAfter(script, hoursAfter, testType)
        }
        catch (exc) {
            Log.error(script, "input validation failed.")
            throw exc
        }

        Log.info(script, "validate inputs success.")
    }

    static validateOperatorVersion(script, operatorVersion, productName) {
        try {
            Log.info(script, "begin operator version validation.")
            if ("${operatorVersion}".equalsIgnoreCase("2.1") && "${productName}".equalsIgnoreCase("WLS-INFRA")) {
                Log.error(script, "Operator version ${operatorVersion} is not applicable for the product ${productName} .")
                throw new Exception("input validation for operator failed")
            }
            Log.info(script, "operator version validation success.")
        }
        catch (exc) {
            Log.error(script, "operator version validation failed.")
            throw exc
        }
    }

    static validateDomainType(script, domainType, productName) {
        try {
            Log.info(script, "begin domain type validation.")
            if ("${domainType}".equalsIgnoreCase("N/A") && "${productName}".equalsIgnoreCase("SOA")) {
                Log.error(script, "Domain type ${domainType} is not applicable for product ${productName} .")
                throw new Exception("domain type validation failed")
            }
            if (!("${domainType}".equalsIgnoreCase("N/A")) && !("${productName}".equalsIgnoreCase("SOA"))) {
                Log.error(script, "Domain type ${domainType} is not applicable for product ${productName} .")
                throw new Exception("domain type validation failed")
            }
            Log.info(script, "domain type validation success.")
        }
        catch (exc) {
            Log.error(script, "domain type validation failed.")
            throw exc
        }
    }

    static manadatoryDatabaseVersion(script, databaseVersion) {
        try {
            Log.info(script, "begin mandatory database version validation.")
            if ("${databaseVersion}" == "" || "${databaseVersion}" == " ") {
                Log.error(script, "Database version is mandatory.")
                throw new Exception(" mandatory database version validation failed")
            }
            Log.info(script, "mandatory database version validation success.")
        }
        catch (exc) {
            Log.error(script, "mandatory database version validation failed.")
            throw exc
        }
    }

    static validateTestimageTag(script, testImageTag, testType) {
        try {
            Log.info(script, "begin testImageTag validation.")
            if ("${testType}" != "N/A" && "${testImageTag}" == "") {
                Log.error(script, "Test Image Tag is mandatory for the ${testType}.")
                throw new Exception("test image tag validation failed")
            }
            Log.info(script, "mandatory test image tag validation success.")
        }
        catch (exc) {
            Log.error(script, "mandatory test image tag validation failed.")
            throw exc
        }
    }

    static validateHoursAfter(script, hoursAfter, testType) {
        try {
            Log.info(script, "begin hours after validation.")
            if ("${testType}" == "N/A" && "${hoursAfter}" == "true") {
                Log.error(script, "Hours after is not applicable for the test type ${testType}.")
                throw new Exception("hours after validation failed")
            }
            Log.info(script, "hours after validation success.")
        }
        catch (exc) {
            Log.error(script, "hours after validation failed.")
            throw exc
        }
    }
}
