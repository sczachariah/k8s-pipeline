package com.oracle.fmwk8s.common

class Log extends Base {
    static info(message) {
        script.echo script: "INFO: ${message}", label: "INFO"
    }

    static warning(message) {
        script.echo script: "WARNING: ${message}", label: "WARNING"
    }

    static error(message) {
        script.echo script: "ERROR: ${message}", label: "ERROR"
    }
}
