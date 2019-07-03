package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Log

class Database {
    static dbName
    static dbSecret

    static deployDatabase(script, databaseVersion, dbNamespace, registryUsername, registryPass) {
        try {
            Log.info(script, "begin deploy database.")

            //dbname can only be 8 character in length
            dbName = "oracledb"
            dbSecret = "regcred"

            script.git branch: 'master',
                    credentialsId: 'sandeep.zachariah.ssh',
                    url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

            script.sh "export KUBECONFIG=${script.env.KUBECONFIG} && \
                       retVal=`echo \\`kubectl get secret ${dbSecret} -n ${dbNamespace} 2>&1\\`` &&\
                       if echo \"\$retVal\" \\| grep -q 'not found'; then\n \
                          kubectl create secret docker-registry ${dbSecret} -n ${dbNamespace} --docker-server=http://container-registry.oracle.com --docker-username='${registryUsername}' --docker-password='${registryPass}' --docker-email='${registryUsername}'\n \
                       fi"

            script.sh "cd kubernetes/framework/db && \
                        sed -i \"s#%DB_NAME%#${dbName}#g\" oracle-db.yaml && \
                        sed -i \"s#%DB_NAMESPACE%#${dbNamespace}#g\" oracle-db.yaml && \
                        sed -i \"s#%DB_IMAGE%#container-registry.oracle.com/database/enterprise:${databaseVersion}-slim#g\" oracle-db.yaml && \
                        sed -i \"s#%DB_SECRET%#${dbSecret}#g\" oracle-db.yaml && \
                        cat oracle-db.yaml && \
                        kubectl apply -f oracle-db.yaml -n ${dbNamespace}"

            script.sh "dbstat='dbstat' && \
                        i=0 && \
                        until `echo \$dbstat | grep -q Running | grep 1/1` > /dev/null\n \
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

            Log.info(script, "deploy database success.")
        }
        catch (exc) {
            Log.error(script, "deploy database failed.")
        }
    }

    static cleanDatabase(script) {}
}
