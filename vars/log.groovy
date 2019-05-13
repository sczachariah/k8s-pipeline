def call(String level = 'INFO', String message = '') {
    if (level.equals('INFO'))
        info(message)
    else if (level.equals('WARNING'))
        warning(message)
}

def info(message) {
    echo "INFO: ${message}"
}

def warning(message) {
    echo "WARNING: ${message}"
}
