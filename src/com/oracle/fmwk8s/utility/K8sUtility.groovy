package com.oracle.fmwk8s.utility


import com.oracle.fmwk8s.common.Log

class K8sUtility {

    static checkPodStatus(script, podname, namespace, timeout) {
        try {
            Log.info(script, "begin ${podname} status check.")
            script.sh "adminstat='adminstat' && \
                        i=0 && \
                        until `echo \$adminstat | grep -q 1/1` > /dev/null\n \
                        do \n \
                            if [ \$i == ${timeout} ]; then\n \
                                echo \"Timeout waiting for Admin server. Exiting!!.\"\n \
                                exit 1\n \
                            fi\n \
                        i=\$((i+1))\n \
                        echo \"${podname} is not Running. Iteration \$i of 20. Sleeping\"\n \
                        sleep 60\n \
                        adminstat=`echo \\`kubectl get pods -n ${namespace} 2>&1 | grep ${podname}\\``\n \
                        done"
            Log.info(script, "${podname} is up and running")
        }
        catch (exc) {
            Log.error(script, "server status check failed.")
        }
    }
}
