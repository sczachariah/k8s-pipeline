package com.oracle.fmwk8s.env

class Logging {

    static configureLogstashConfigmap(script, domainName){
    }

    static configureLogstash(script, domainName, domainNamespace){
    }

    static deployLogstash(script, domainName, domainNamespace){
        configureLogstashConfigmap()
        configureLogstash()


    }
}
