package com.oracle.fmwk8s.utility


import com.oracle.fmwk8s.common.Log

class K8sUtility {

    static checkPodStatus(script, podName, namespace, timeout) {
        try {
            Log.info("begin ${podName} status check.")
            script.sh "podstat='podstat' && \
                        i=0 && \
                        until `echo \$podstat | grep -q 1/1` > /dev/null\n \
                        do \n \
                            if [ \$i == ${timeout} ]; then\n \
                                echo \"timeout waiting for pod ${podName}. exiting!!.\"\n \
                                exit 1\n \
                            fi\n \
                        i=\$((i+1))\n \
                        echo \"${podName} is not running. iteration \$i of ${timeout}. sleeping\"\n \
                        sleep 30\n \
                        podstat=`echo \\`kubectl get pods -n ${namespace} 2>&1 | grep ${podName}\\``\n \
                        done"
            Log.info("${podName} is up and running")
        }
        catch (exc) {
            Log.error("server status check failed.")
//            throw exc
        }
    }
}
