package com.oracle.fmwk8s.test

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.env.Database
import com.oracle.fmwk8s.env.Domain
import com.oracle.fmwk8s.env.Operator
import com.oracle.fmwk8s.utility.YamlUtility

class OperatorIntegration {
    static def yamlUtility = new YamlUtility()
    static def testId = "op-intg"
    static def mavenProfile

    static createTestProps(script) {
        try {
            Log.info(script, "begin create test properties.")

            script.sh "cd config && \
                       sed -i \"s|\\\${PRODUCT_NAME}|${Common.productName}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${OPERATOR_NS}|${Operator.operatorNamespace}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${OPERATOR_SA}|${Operator.operatorServiceAccount}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${DOMAIN_NAME}|${Domain.domainName}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${DOMAIN_NS}|${Domain.domainNamespace}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${CLUSTER_NAME}|${yamlUtility.domainInputsMap.get("clusterName")}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${MANAGED_SERVER_NAME_BASE}|${yamlUtility.domainInputsMap.get("managedServerNameBase")}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${ADMIN_SERVER_NAME}|${yamlUtility.domainInputsMap.get("adminServerName")}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${WEBLOGIC_CREDENTIALS_SECRET_NAME}|${Domain.weblogicCredentialsSecretName}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${OPERATOR_HELM_RELEASE}|${Operator.operatorHelmRelease}|g\" operatorTest.properties && \
                       sed -i \"s|\\\${IMAGE_NAME}|${Common.productImage}|g\" && \
                       cat operatorTest.properties"

            Log.info(script, "create test properties success.")
        }
        catch (exc) {
            Log.error(script, "create test properties failed.")
        }
        finally {
        }
    }


    static invokeTest(script, testImage, mavenProfile) {
        this.mavenProfile = mavenProfile
        createEnvConfigMap(script)
        runTests(script, testImage)
        publishResults(script)
    }

    static createEnvConfigMap(script) {
        try {
            Log.info(script, "begin create env configmap.")

            script.git branch: 'master',
                    credentialsId: 'sandeep.zachariah.ssh',
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh "cd kubernetes/framework/test/operator-integration && \
                        sed -i \"s|%ADMIN_SERVER_NAME_SVC%|${Domain.domainName}-adminserver.${Domain.domainNamespace}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%MANAGED_SERVER_NAME_SVC%|${Domain.domainName}-cluster-${Common.productId}-cluster.${Domain.domainNamespace}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%WEBLOGIC_USER%|${Domain.weblogicUser}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%ADMIN_PASSWORD%|${Domain.weblogicPass}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%ADMIN_PORT%|7001|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%ADMIN_SERVER_NAME%|admin-server|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%ADMIN_SSL_PORT%||g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%CONNECTION_STRING%|${Database.dbName}.${Domain.domainNamespace}:${Database.dbPort}/${Database.dbName}pdb.us.oracle.com|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DB_HOST%|${Database.dbName}.${Domain.domainNamespace}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DB_PORT%|${Database.dbPort}|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DB_SCHEMA_PASSWORD%|Welcome1|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%DB_SID%|${Database.dbName}pdb.us.oracle.com|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%JDBC_URL%|jdbc:oracle:thin:@${Database.dbName}.${Domain.domainNamespace}:${Database.dbPort}/${Database.dbName}pdb.us.oracle.com|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%MANAGED_SERVER_NAME_BASE%|${Common.productId}_server|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%MANAGED_SERVER_PORT%|8001|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        sed -i \"s|%MDS_USER%|" + "${Common.productId}".toUpperCase() + "1_MDS|g\" fmwk8s-${testId}-env-configmap.yaml && \
                        cat fmwk8s-${testId}-env-configmap.yaml"

            script.sh "kubectl apply -f kubernetes/framework/${testId}/fmwk8s-${testId}-env-configmap.yaml -n ${Domain.domainNamespace}"

            Log.info(script, "create env configmap success.")
        }
        catch (exc) {
            Log.error(script, "create env configmap failed.")
        }
        finally {
        }
    }

    static runTests(script, testImage) {
        try {
            Log.info(script, "begin run test.")

            script.git branch: 'master',
                    credentialsId: 'sandeep.zachariah.ssh',
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh "cd kubernetes/framework/${testId} && \
                        sed -i \"s#%TEST_IMAGE%#${testImage}#g\" fmwk8s-${testId}-mats-pod.yaml && \
                        cat fmwk8s-${testId}-mats-pod.yaml"

            script.sh "kubectl apply -f kubernetes/framework/${testId}/fmwk8s-${testId}-mats-pod.yaml -n ${Domain.domainNamespace} && \
                       kubectl get all -n ${Domain.domainNamespace}"

            waitForTests(script)

            Log.info(script, "run test success.")
        }
        catch (exc) {
            Log.error(script, "run test failed.")
        }
        finally {
        }
    }

    static waitForTests(script) {
        try {
            Log.info(script, "begin wait for test completion.")

            script.sh "testStat='testStat' && \
                        i=0 && \
                        until `echo \$testStat | grep -q Completed` > /dev/null\n \
                        do \n \
                            if [ \$i == 50 ]; then\n \
                                echo \"Timeout waiting for Test Completion. Exiting!!.\"\n \
                                exit 1\n \
                            fi\n \
                        i=\$((i+1))\n \
                        echo \"Test is Running. Iteration \$i of 50. Sleeping\"\n \
                        sleep 300\n \
                        testStat=`echo \\`kubectl get pods -n ${Domain.domainNamespace} 2>&1 | grep fmwk8s-${testId}-mats\\``\n \
                        done"

            Log.info(script, "wait for test completion success.")
        }
        catch (exc) {
            Log.error(script, "wait for test completion failed.")
            throw exc
        }
    }

    static publishResults(script) {
    }

    static cleanTests(script) {}
}
