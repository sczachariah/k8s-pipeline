package com.oracle.fmwk8s.common

import java.text.SimpleDateFormat

class Common {
    static def getUniqueId() {
        def date = new Date()
        def sdf = new SimpleDateFormat("MMddHHmmss")

        def buildNumber = System.getenv("BUILD_NUMBER")
        def uniqueId = sdf.format(date) + "." + buildNumber

        return uniqueId
    }
}
