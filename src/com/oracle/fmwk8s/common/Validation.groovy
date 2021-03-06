package com.oracle.fmwk8s.common

/**
 * Validation class handles the common validation operations that are required
 * in E2E execution of FMW in Docker/K8S environments
 */
class Validation extends Base {

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
    static def validateInputs() {
        try {
            Log.info("begin validate inputs.")
            validateProduct(productName)
            validateOperatorVersion(operatorVersion, productName)
            validateProductImageTag(productImage)
            validateDomainType(domainType, productName)
            validateDatabaseVersion(databaseVersion)
            validateTestImageTag(testImage, testType)
            // disabling hoursAfter validation so that env can be retained even if no tests are run
//            validateHoursAfter(hoursAfter, testType)
        }
        catch (exc) {
            Log.error("input validation failed.")
            throw exc
        }

        Log.info("validate inputs success.")
    }

    static validateProduct(productName) {
        try {
            Log.info("begin product name validation.")
            if (!Mapping.productIdMap.containsKey(productName)) {
                Log.error("product ${productName} is not supported by validation framework.")
                throw new Exception("[validation error] product ${productName} is not supported by validation framework.")
            }
            Log.info("product name validation success.")
        } catch (exc) {
            Log.error("product name validation failed.")
            throw exc
        }
    }

    /**
     * validates the operator version whether is is applicable for the product or not
     *
     * @param script the workflow script of jenkins
     * @param operatorVersion the version of operator
     * @param productName the name of the product
     */
    static validateOperatorVersion(operatorVersion, productName) {
        try {
            Log.info("begin operator version validation.")
            if ("${operatorVersion}".equalsIgnoreCase("2.1") && "${productName}".equalsIgnoreCase("WLS-INFRA")) {
                Log.error("Operator version ${operatorVersion} is not applicable for the product ${productName} .")
                throw new Exception("[validation error] operator version ${operatorVersion} is not applicable for the product ${productName} .")
            }
            Log.info("operator version validation success.")
        }
        catch (exc) {
            Log.error("operator version validation failed.")
            throw exc
        }
    }

    static validateProductImageTag(productImage) {
        try {
            Log.info("begin product image tag validation.")
            if (productImage == null || productImage.toString().isEmpty()) {
                Log.error("Product Image Tag is mandatory.")
                throw new Exception("[validation error] productImage tag is mandatory.")
            }
            Log.info("product image tag validation success.")
        }
        catch (exc) {
            Log.error("product image tag validation failed.")
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
    static validateDomainType(domainType, productName) {
        try {
            Log.info("begin domain type validation.")
            if ("${domainType}".equalsIgnoreCase("N/A") && "${productName}".equalsIgnoreCase("SOA")) {
                Log.error("Domain type ${domainType} is not applicable for product ${productName}. So defaulting to domainType=soa")
                Base.domainType = "soa"
            } else if (!("${domainType}".equalsIgnoreCase("N/A")) && !("${productName}".equalsIgnoreCase("SOA"))) {
                Log.error("Domain type ${domainType} is not applicable for product ${productName}. So defaulting to domainType=N/A")
                Base.domainType = "N/A"
            }
            Log.info("domain type validation success.")
        }
        catch (exc) {
            Log.error("domain type validation failed.")
            throw exc
        }
    }

    /**
     * validates for the mandatory databaseVersion
     *
     * @param script the workflow script of jenkins
     * @param databaseVersion the version of the database
     */
    static validateDatabaseVersion(databaseVersion) {
        try {
            Log.info("begin mandatory database version validation.")
            if ("${databaseVersion}" == "" || "${databaseVersion}" == " ") {
                Log.error("Database version is mandatory.")
                throw new Exception("[validation error] database version is mandatory.")
            }
            Log.info("mandatory database version validation success.")
        }
        catch (exc) {
            Log.error("mandatory database version validation failed.")
            throw exc
        }
    }

    /**
     * validates for the mandatory testImageTag
     *
     * @param testImageTag the tag for the test image
     * @param testType the type of the test selected in E2E
     */
    static validateTestImageTag(testImageTag, testType) {
        try {
            Log.info("begin testImageTag validation.")
            if ("${testType}" != "N/A" && "${testImageTag}" == "") {
                Log.error("Test Image Tag is mandatory for the ${testType}.")
                throw new Exception("[validation error] testImage tag is mandatory for the ${testType}.")
            }
            Log.info("mandatory test image tag validation success.")
        }
        catch (exc) {
            Log.error("mandatory test image tag validation failed.")
            throw exc
        }
    }

    /**
     * validates the hoursafter flag if there is no testType selected
     *
     * @param hoursAfter the number of hours to hold the environment
     * @param testType the type of the test selected in E2E
     */
    static validateHoursAfter(hoursAfter, testType) {
        try {
            Log.info("begin hours after validation.")
            if ("${testType}" == "N/A" && "${hoursAfter}" == "true") {
                Log.error("Hours after is not applicable for the test type ${testType}.")
                throw new Exception("[validation error] hoursAfter is not applicable for the test type ${testType}.")
            }
            Log.info("hours after validation success.")
        }
        catch (exc) {
            Log.error("hours after validation failed.")
            throw exc
        }
    }
}
