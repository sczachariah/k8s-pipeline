package com.oracle.fmwk8s.utility


import com.oracle.fmwk8s.common.Log

class DocUtility {

    static generateGroovyDoc(script) {
        Log.info(script, "begin generate groovy doc.")

        script.git branch: 'master',
                credentialsId: 'sandeep.zachariah.ssh',
                url: 'git@orahub.oraclecorp.com:fmw-platform-qa/fmw-k8s-pipeline.git'

        script.sh "groovy doc.groovy"
        script.sh "rm -rf /fmwk8s/groovydoc"
        script.sh "cp -r groovydoc /fmwk8s/"
        script.sh "chmod -R 777 /fmwk8s/groovydoc"

        Log.info(script, "generate groovy doc success.")
    }
}
