package com.oracle.fmwk8s.env


import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log

/**
 * Database class handles the common database operations that are required
 * in E2E execution of FMW in Docker/K8S environments
 */
class Database extends Common {
    /** the name of the database service */
    static dbName = "oracledb"
    /** the password to connect to db */
    static dbPassword = "Oradoc_db1"
    /** the port on which database service is up */
    static dbPort = "1521"
    /** the kubernetes pod name of database pod */
    static dbPodName

    /**
     * Deploys a specific version of database container in a kubernetes cluster.
     *
     * @param script the workflow script of jenkins
     * @param databaseVersion the version of database that needs to be deployed
     * @param dbNamespace the kubernetes namespace in which database is to be deployed
     */
    static deployDatabase() {
        try {
            if (productId != "weblogic") {
                Log.info("begin deploy database.")

                script.git branch: 'master',
                        credentialsId: 'sandeep.zachariah.ssh',
                        url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

                script.sh "cd kubernetes/framework/db && \
                        sed -i \"s#%DB_NAME%#${dbName}#g\" oracle-db.yaml && \
                        sed -i \"s#%DB_PASSWORD%#${dbPassword}#g\" oracle-db.yaml && \
                        sed -i \"s#%DB_NAMESPACE%#${domainNamespace}#g\" oracle-db.yaml && \
                        sed -i \"s#%DB_IMAGE%#container-registry.oracle.com/database/${databaseVersion}#g\" oracle-db.yaml && \
                        sed -i \"s#%DB_SECRET%#${registrySecret}#g\" oracle-db.yaml && \
                        cat oracle-db.yaml && \
                        kubectl apply -f oracle-db.yaml -n ${domainNamespace}"

                script.sh "dbstat='dbstat' && \
                        i=0 && \
                        until `echo \$dbstat | grep -q 1/1` > /dev/null\n \
                        do \n \
                            if [ \$i == 25 ]; then\n \
                                echo \"timeout waiting for db. exiting!!.\"\n \
                                exit 1\n \
                            fi\n \
                        i=\$((i+1))\n \
                        echo \"db is not running. iteration \$i of 25. sleeping\"\n \
                        sleep 60\n \
                        dbstat=`echo \\`kubectl get pods -n ${domainNamespace} 2>&1 | grep ${dbName}\\``\n \
                        done"

                Log.info("db container is running.")
                script.sh "kubectl get pods,svc -n ${domainNamespace} | grep ${dbName}"

                Log.info("begin xaview setup for database.")
                dbPodName = script.sh(
                        script: "kubectl get pods -o go-template --template \'{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}\' -n ${domainNamespace} | grep ${dbName}",
                        returnStdout: true
                ).trim()
                script.sh "kubectl exec -it ${dbPodName} -n ${domainNamespace} -- bash -c \"source /home/oracle/.bashrc; sqlplus sys/${dbPassword}@${dbName}pdb as sysdba <<EOF @\\\$ORACLE_HOME/rdbms/admin/xaview.sql / exit EOF\""
                Log.info("xaview setup for database success.")

                Log.info("deploy database success.")
            }
        }
        catch (exc) {
            Log.error("deploy database failed.")
            throw exc
        }
        finally {
            if (productId != "weblogic") {
                Log.info("begin fetch database pod logs.")
                Logging.getPodLogs(dbPodName, domainNamespace)
                Log.info("fetch database pod logs success.")
            }

        }
    }

    static cleanDatabase(script) {}
}
