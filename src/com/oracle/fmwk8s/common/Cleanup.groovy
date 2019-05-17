package com.oracle.fmwk8s.common

class Cleanup {
    static cleanOperator(String release) {
        try {
            sh label: 'clean soa operator', script: '''
                        helm delete --purge ${release}
                        '''
        }
        catch (exc) {
            echo "Cleanup operator failed!!."
        }
    }

    static def cleanDomain(String namespace) {}

    static def cleanDatabase(String namespace) {}
}
