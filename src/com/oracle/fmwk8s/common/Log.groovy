package com.oracle.fmwk8s.common

class Log extends Base {
    static info(message) {
        script.echo "INFO: ${message}"
    }

    static warning(message) {
        script.echo "WARNING: ${message}"
    }

    static error(message) {
        script.echo "ERROR: ${message}"
    }
}
