package com.oracle.fmwk8s.common

/**
 * Validation class handles the common validation operations that are required
 * in E2E execution of FMW in Docker/K8S environments
 */
class Validation {
    
    /**
     * validates the inputs given to the E2E execution job
     *
     * @param script the workflow script of jenkins
     * @param operatorVersion the version of operator
     * @param productName the name of the product
     * @param domainType the domain type of the product
     * @param databaseVersion the version of the database
     * @param testImageTag the tag for the test image
     * @param testType the type of the test selected in E2E
     * @param hoursAfter the number of hours to hold the environment
     */
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
    
    /**
     * validates the operator version whether is is applicable for the product or not
     *
     * @param script the workflow script of jenkins
     * @param operatorVersion the version of operator
     * @param productName the name of the product
     */
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
    
    /**
     * validates whether the domain type is applicable for the product or not
     *
     * @param script the workflow script of jenkins
     * @param domainType the domain type of the product
     * @param productName the name of the product
     */
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
    
    /**
     * validates for the mandatory databaseVersion
     *
     * @param script the workflow script of jenkins
     * @param databaseVersion the version of the database
     */
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
    
    /**
     * validates for the mandatory testImageTag
     *
     * @param testImageTag the tag for the test image
     * @param testType the type of the test selected in E2E
     */
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
    
    /**
     * validates the hoursafter flag if there is no testType selected
     *
     * @param hoursAfter the number of hours to hold the environment
     * @param testType the type of the test selected in E2E
     */
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
