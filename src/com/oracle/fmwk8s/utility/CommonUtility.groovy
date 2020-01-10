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

    static httpPOST() {
        HttpURLConnection post = new URL("https://httpbin.org/post").openConnection()
        def message = '{"message":"this is a message"}'
        post.setRequestMethod("POST")
        post.setDoOutput(true)
        post.setRequestProperty("Content-Type", "application/json")
        post.getOutputStream().write(message.getBytes("UTF-8"))
        def postRC = post.getResponseCode();
        println(postRC)
        if (postRC == 200) {
            println(post.getInputStream().getText())
        }

    }

    static httpPOST1() {
        def baseUrl = new URL('http://api.duckduckgo.com')
        def queryString = 'q=groovy&format=json&pretty=1'
        def connection = baseUrl.openConnection()
        connection.with {
            doOutput = true
            outputStream.withWriter { writer ->
                writer << queryString
            }
            println content.toString()
        }
    }

    static httpPOST2() {
        final HttpURLConnection connection = new URL('').openConnection()
        connection.setDoOutput(true)
        connection.outputStream.withWriter { Writer writer ->
            writer << 'arg=foo'
        }

        String response = connection.inputStream.withReader { Reader reader -> reader.text }
        println response
        println connection.responseCode
    }
}
