package com.oracle.fmwk8s.common

class Log {
    static info(script, message) {
        script.echo "INFO: ${message}"
    }

    static warning(script, message) {
        script.echo "WARNING: ${message}"
    }

    static error(script, message) {
        script.echo "ERROR: ${message}"
    }
}
