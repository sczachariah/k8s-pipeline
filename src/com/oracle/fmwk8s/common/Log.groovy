package com.oracle.fmwk8s.common

class Log {
    static info(String message) {
        String msg = (String) "INFO: ${message}"
        echo msg
    }

    static warning(String message) {
        String msg = (String) "WARNING: ${message}"
        echo msg
    }

    static error(String message) {
        String msg = (String) "ERROR: ${message}"
        echo msg
    }
}
