package com.oracle.fmwk8s.utility

import com.cloudbees.groovy.cps.NonCPS
@GrabResolver(name = 'fmw-virtual', root = 'http://artifactory-slc-prod1.oraclecorp.com/artifactory/fmw-virtual/')
@Grab('org.apache.commons:commons-lang3:3.9')
import org.apache.commons.lang3.RandomStringUtils

class CommonUtility implements Serializable {

    @NonCPS
    static String generatePassword() {
        String upperCaseLetters = RandomStringUtils.random(1, 65, 90, true, true)
        String lowerCaseLetters = RandomStringUtils.random(6, 97, 122, true, true)
        String numbers = RandomStringUtils.randomNumeric(1)
        String password = upperCaseLetters.concat(lowerCaseLetters).concat(numbers)
        return password
    }
}
