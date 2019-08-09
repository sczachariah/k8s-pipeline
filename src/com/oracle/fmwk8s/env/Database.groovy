package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log

class Database {
    static dbName = "oracledb"
    static dbPassword = "Oradoc_db1"
    static dbPort = "1521"
    static dbPodName

    static deployDatabase(script, databaseVersion, dbNamespace) {
        try {
            if (Common.productId != "weblogic") {
                Log.info(script, "begin deploy database.")

                script.git branch: 'master',
                        credentialsId: 'sandeep.zachariah.ssh',
                        url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

                script.sh "cd kubernetes/framework/db && \
                        sed -i \"s#%DB_NAME%#${dbName}#g\" oracle-db.yaml && \
                        sed -i \"s#%DB_PASSWORD%#${dbPassword}#g\" oracle-db.yaml && \
                        sed -i \"s#%DB_NAMESPACE%#${dbNamespace}#g\" oracle-db.yaml && \
                        sed -i \"s#%DB_IMAGE%#container-registry.oracle.com/database/${databaseVersion}#g\" oracle-db.yaml && \
                        sed -i \"s#%DB_SECRET%#${Common.registrySecret}#g\" oracle-db.yaml && \
                        cat oracle-db.yaml && \
                        kubectl apply -f oracle-db.yaml -n ${dbNamespace}"

                script.sh "dbstat='dbstat' && \
                        i=0 && \
                        until `echo \$dbstat | grep -q 1/1` > /dev/null\n \
                        do \n \
                            if [ \$i == 25 ]; then\n \
                                echo \"Timeout waiting for DB. Exiting!!.\"\n \
                                exit 1\n \
                            fi\n \
                        i=\$((i+1))\n \
                        echo \"DB is not Running. Iteration \$i of 25. Sleeping\"\n \
                        sleep 60\n \
                        dbstat=`echo \\`kubectl get pods -n ${dbNamespace} 2>&1 | grep ${dbName}\\``\n \
                        done"

                Log.info(script, "DB container is Running.")
                script.sh "kubectl get pods,svc -n ${dbNamespace} | grep ${dbName}"

                Log.info(script, "begin xaview setup for database.")
                this.dbPodName = script.sh(
                        script: "kubectl get pods -o go-template --template \'{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}\' -n ${dbNamespace} | grep ${dbName}",
                        returnStdout: true
                ).trim()
                script.sh "kubectl exec -it ${this.dbPodName} bash -c \"source /home/oracle/.bashrc; sqlplus sys/Oradoc_db1@ORCLCDB as sysdba <<EOF @\\\$ORACLE_HOME/rdbms/admin/xaview.sql / exit EOF\""
                Log.info(script, "xaview setup for database success.")

                Log.info(script, "deploy database success.")
            }
        }
        catch (exc) {
            Log.error(script, "deploy database failed.")
            throw exc
        }
    }

    static cleanDatabase(script) {}
}
