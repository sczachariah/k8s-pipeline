package com.oracle.fmwk8s.common

class Initializer {

    static def initialize(def script) {
        script.sh "echo Initializing Validation Framework"
    }
}