package com.oracle.fmwk8s.common

class Log {
    static info(String message) {
        echo "INFO: ${message}"
    }
    static warning(String message) {
        echo "WARNING: ${message}"
    }
    static error(String message) {
        echo "ERROR: ${message}"
    }
}
