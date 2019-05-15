package com.oracle.fmwk8s.common

import java.text.SimpleDateFormat

class Common {
    static def getUniqueId(def script) {
        def date = new Date()
        def sdf = new SimpleDateFormat("MMddHHmm")

        def buildNumber = "${script.env.BUILD_NUMBER}"
        def uniqueId = buildNumber + "-" + sdf.format(date)

        return uniqueId
    }
}
