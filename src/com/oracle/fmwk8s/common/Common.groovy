package com.oracle.fmwk8s.common

import java.text.SimpleDateFormat

class Common {
    static String getUniqueId() {
        def date = new Date()
        def sdf = new SimpleDateFormat("MMddHHmmss")

        def buildNumber = System.getenv("BUILD_NUMBER")
        String uniqueId = sdf.format(date) + "." + buildNumber

        return uniqueId
    }
}
