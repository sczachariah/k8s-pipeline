package com.oracle.fmwk8s.env

import com.oracle.fmwk8s.common.Log

class Database {
    static deployDatabase(script,domainns,REGISTRY_AUTH_USR,REGISTRY_AUTH_PSW) {
        try {
            Log.info(script, "setup env !!!")
            script.sh "export KUBECONFIG=${script.env.KUBECONFIG}"
            Log.info(script, "setup db !!!")
            script.sh "retVal=`echo \\`kubectl get secret regcred -n ${domainns} 2>&1\\``"
            script.sh "if echo "$retVal" | grep -q ""not found""; then\n \
                           kubectl create secret docker-registry regcred -n ${domainns} --docker-server=http://container-registry.oracle.com \
                          --docker-username='${REGISTRY_AUTH_USR}' --docker-password='${REGISTRY_AUTH_PSW}' --docker-email='${REGISTRY_AUTH_USR}'\n \
                       fi"
            script.sh "cd kubernetes/samples/scripts/create-soa-domain/domain-home-on-pv/multiple-Managed-servers"
            script.sh "cp soadb.yaml soadb.yaml.orig"

            script.sh "sed -i ""s#image: oracle/database:12.2.0.1#image: container-registry.oracle.com/database/enterprise:12.2.0.1-slim#g"" soadb.yaml"
            script.sh "sed -i \"s#namespace: soans#namespace: ${domainns}#g\" soadb.yaml"
            script.sh "sed -i \"s#terminationGracePeriodSeconds: 30#terminationGracePeriodSeconds: 30\\n      imagePullSecrets:\\n      - name: regcred#g\" soadb.yaml"
            script.sh "cat soadb.yaml"

            script.sh "kubectl apply -f soadb.yaml"

            script.sh "dbstat='dbstat'"
            script.sh "i=0"
            script.sh "until `echo \$dbstat | grep -q Running` > /dev/null\n \
                       do \n \
                       if [ \$i == 25 ]; then\n \
                       echo \"Timeout waiting for DB. Exiting!!.\"\n \
                       exit 1\n \
                       fi\n \
                       i=\$((i+1))\n \
                       echo \"DB is not Running. Iteration \$i of 25. Sleeping\"\n \
                       sleep 60\n \
                       dbstat=`echo \\`kubectl get pods -n ${domainns} 2>&1 | grep soadb\\``\n \
                       done"
            Log.info(script, "DB container is Running.")
            script.sh "sleep 300"

            script.sh "kubectl get pods,svc -n ${domainns} | grep soadb"

            Log.info(script, "Deploy DB Completed!!!")

        }
        catch (exc) {
            Log.error(script, "Deploy Database failed!!.")
        }
    }

    static cleanDatabase(script) {}
}
