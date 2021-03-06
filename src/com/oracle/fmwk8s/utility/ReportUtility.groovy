package com.oracle.fmwk8s.utility

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.common.Log
import com.oracle.fmwk8s.env.Domain
import com.oracle.fmwk8s.env.IngressController
import com.oracle.fmwk8s.test.Test

class ReportUtility {
    /** Variable Declaration */
    static def domainURLs = ""
    /** Variable for storing count of total suc, dif, skip files generated after test execution */
    static Integer totalSucDifSkipCasesCount = 0
    /** Variable for storing count of succeeded test cases */
    static Integer sucCountValue = 0
    /** Variable for storing count of failed test cases */
    static Integer difCountValue = 0
    /** Variable for storing count of skipped test cases */
    static Integer skipCountValue = 0
    /** List Variable for storing list of suc file names generated after test execution */
    static def sucFileNameList = ""
    /** List Variable for storing list of dif file names generated after test execution */
    static def difFileNameList = ""
    /** List Variable for storing list of skip file names generated after test execution */
    static def skipFileNameList = ""

    /** List Variable for storing list of all suc,dif,skip file names generated after test execution */
    static def overallExecutedTestCaseList = ""

    static printDomainUrls(script) {
        domainURLs = """
-----------Domain URL's------------
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/weblogic/ready
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/console
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/consolehelp
"""
        if(!Common.productId.toString().equalsIgnoreCase("weblogic")){
            domainURLs = domainURLs + """
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/em
"""
        }
        if (Common.productId.toString().equalsIgnoreCase("soa")) {
            if (Domain.domainType.toString().toLowerCase().contains("soa") ||
                    Domain.domainType.toString().toLowerCase().contains("soaosb") ||
                    Domain.domainType.toString().toLowerCase().contains("soaessosb")) {
                domainURLs = domainURLs + """
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/soa-infra/
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/soa/composer
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/integration/worklistapp
"""
            }
            if (Domain.domainType.toString().toLowerCase().contains("osb")) {
                domainURLs = domainURLs + """
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/servicebus
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/lwpfconsole
"""
            }
            if (Domain.domainType.toString().toLowerCase().contains("ess")) {
                domainURLs = domainURLs + """
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/ess
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/EssHealthCheck
"""
            }
        }
        if (Common.productId.toString().equalsIgnoreCase("wcp")) {
            domainURLs = domainURLs + """
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/webcenter
"""
        }
        if (Common.productId.toString().equalsIgnoreCase("wcsites")) {
            domainURLs = domainURLs + """
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/sites/sitesconfigsetup
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/sites
"""
        }
        domainURLs = domainURLs + """
-----------------------------------
"""
        script.sh label: "print domain url's",
                script: "printf \"%s\\n\" \"${domainURLs.toString()}\""
    }

    /**
     * countOfSucDifFilesAfterTestRunsAndGenerateTestSummaryReport : Method to generate the number of *.suc, *.dif and *.skip files from test_logs folder
     * after the successful test execution. Once these files are generated they are counted and the file names are listed out and these
     * variables are passed to the HTML report generation for the test case summary.
     * @param script
     * @return
     */
    static countOfSucDifFilesAfterTestRunsAndGenerateTestSummaryReport(script) {
        Log.info("Begin to count the *.suc & *.dif files from test logs folder after tests are run.")
        try {
            /**Display contents of the test_logs directory */
            script.sh "ls -ltr ${Test.logDirectory}"

            /** Logic to check if the fmwk8s.completed file exists and is created after test execution */
            def fileExists = script.sh(
                    label: "check if the fmwk8s.completed file exists and is created after test execution",
                    script: "test -f ${Test.logDirectory}/fmwk8s.completed && echo 'exists'",
                    returnStdout: true).trim()
            Log.info("file Exists........... :: ${fileExists}")
            /** if the fmwk8s.completed file exists, then we calculate suc dif for tests executed */
            if (fileExists == 'exists') {
                /** Logic to evaluate no. of *.suc files in test_logs directory given above */
                def sucCount = script.sh(
                        label: "evaluate no. of *.suc files in test_logs directory",
                        script: "cd ${Test.logDirectory} && find . -name *.suc  | wc | uniq | awk '{print \$1}'",
                        returnStdout: true)
                Log.info("suc........... ::  ${sucCount.toInteger()}")

                /** Logic to evaluate no. of *.dif files in test_logs directory given above */
                def difCount = script.sh(
                        label: "evaluate no. of *.dif files in test_logs directory",
                        script: "cd ${Test.logDirectory} && find . -name *.dif  | wc | uniq | awk '{print \$1}'",
                        returnStdout: true).trim()
                Log.info("dif........... :: ${difCount.toInteger()}")

                /** Logic to evaluate no. of *.skip files in test_logs directory given above */
                def skipCount = script.sh(
                        label: "evaluate no. of *.skip files in test_logs directory",
                        script: "cd ${Test.logDirectory} && find . -name *.skip  | wc | uniq | awk '{print \$1}'",
                        returnStdout: true).trim()
                Log.info("skip........... :: ${skipCount.toInteger()}")

                sucCountValue = (sucCount == null) ? 0 : sucCount.trim().toInteger()
                difCountValue = (difCount == null) ? 0 : difCount.trim().toInteger()
                skipCountValue = (skipCount == null) ? 0 : skipCount.trim().toInteger()

                /** Summarize the total no of *.suc, *.dif & *.skip files in test_logs directory given above */
                totalSucDifSkipCasesCount = sucCountValue + difCountValue + skipCountValue
                Log.info("total........... :: :: $totalSucDifSkipCasesCount")

                /** Fetch the test case names that generated *.suc files in test_logs directory */
                sucFileNameList = script.sh(
                        label: "Fetch the test case names that generated *.suc files in test_logs directory",
                        script: "cd ${Test.logDirectory} && find . -name *.suc| uniq | xargs -r -n 1 basename",
                        returnStdout: true
                )
                Log.info("sucFileNameList :: \n${sucFileNameList.toString()}")

                /** Fetch the test case names that generated *.dif files in test_logs directory */
                difFileNameList = script.sh(
                        label: "Fetch the test case names that generated *.dif files in test_logs directory",
                        script: "cd ${Test.logDirectory} && find . -name *.dif| uniq | xargs -r -n 1 basename",
                        returnStdout: true
                )
                Log.info("difFileNameList :: \n${difFileNameList.toString()}")

                /** Fetch the test case names that generated *.skip files in test_logs directory */
                skipFileNameList = script.sh(
                        label: "Fetch the test case names that generated *.skip files in test_logs directory",
                        script: "cd ${Test.logDirectory} && find . -name *.skip| uniq | xargs -r -n 1 basename",
                        returnStdout: true
                )
                Log.info("skipFileNameList :: \n${skipFileNameList.toString()}")

                /** Summarize the test cases totally executed and is dumped in test_logs folder.
                 * Fetch the total test case executed *.suc, *.dif and *.skip files in test_logs directory */
                overallExecutedTestCaseList = sucFileNameList + difFileNameList + skipFileNameList
                Log.info("Overall Executed Tests :: \n${overallExecutedTestCaseList.toString()}")

                Log.info("count of *.suc & *.dif files from test logs folder is evaluated successfully")

                /** if hourAfter is > 0 then issue mail notification soon after wait for test is completed */
                if ("${Common.testType}" != "N/A") {
                    ReportUtility.sendNotificationMailPostTestExecution(script)
                }
            }

        } catch (exc) {
            Log.error("count of *.suc & *.dif files from test logs folder has failed!!!.")
            exc.printStackTrace()
            throw exc
        }
    }

    static sendNotificationMailPostDomainCreation(script) {
        def subject = "Environment details for '[${script.env.BUILD_NUMBER}]'"
        def body = """<p>Hi,</p>
"""

        body = body + """
<p>As requested, a ${Common.productName} environment with ${Common.domainType} domain has been provisioned with below parameters:</p>

<table border="1">
<tr><td>CLOUD</td><td>${Common.cloud}</td></tr>
<tr><td>OPERATOR_VERSION</td><td>${Common.operatorVersion}</td></tr>
<tr><td>PRODUCT_NAME</td><td>${Common.productName}</td></tr>
<tr><td>DOMAIN_TYPE</td><td>${Common.domainType}</td></tr>
<tr><td>PRODUCT_IMAGE_TAG</td><td>${Common.productImage}</td></tr>
<tr><td>DATABASE_VERSION</td><td>${Common.databaseVersion}</td></tr>
<tr><td>K8S_LOADBALANCER</td><td>${Common.lbType}</td></tr>
<tr><td>TEST_IMAGE_TAG</td><td>${Common.testImage}</td></tr>
<tr><td>TEST_TYPE</td><td>${Common.testType}</td></tr>
<tr><td>HOURS_AFTER</td><td>${Common.hoursAfter}</td></tr>
</table>

"""

        if (Common.hoursAfter > 0) {
            Calendar calendar = Calendar.getInstance()
            System.out.println("Original = " + calendar.getTime())
            calendar.add(Calendar.HOUR, Integer.parseInt("${Common.hoursAfter}").intValue())
            def hourAfterTime
            hourAfterTime = calendar.getTime()
            body = body + """
<p>The environment is available for use for ${Common.hoursAfter} hours. The environment will be de-commissioned at ${
                hourAfterTime
            } </p>
"""
        }

        body = body + """
<p>Please find below the access URLs to the environment. The credentials for the same are ${Common.weblogicUsername}/${Common.weblogicPassword}.</p>

"""

        String[] str
        str = domainURLs.split('\n')

        body = body + """
<table border="1">
<tr><td>Domain Url's</td></tr>
"""

        for (String values : str) {
            Log.info("value is $values")
            if ("$values".contains("http")) {
                body = body + """<tr><td>${values}</td></tr> """
            }
        }

        body = body + """</table>"""

        def domainURL
        domainURL = "${Common.k8sMasterUrl}/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/#!/overview?namespace=${Common.domainNamespace}"

        if ("${Common.testType}" != "N/A") {
            body = body + """
<p>QA tests are being executed against the provisioned environment. Domain and Test pods are accessible here : <a href="${
                domainURL
            }">${domainURL}</a> </p>

<p>A detailed status on test execution will be sent over email once the test execution is completed. </p>
"""
        } else {
            body = body + """
<p>Domain pods are accessible here : <a href="${domainURL}">${domainURL}</a> </p>
"""
        }
        if ("${Common.elkEnable}" == "true") {
            body = body + """
<p>Operator and Domain logs are available in Kibana : <a href="${Common.kibanaUrl}">${Common.kibanaUrl}</a> </p>
"""
        }

        body = body + """
<p>Regards,</p>
<p>FMW K8S Team</p>
"""
        sendMail(script, subject, body)
    }

    /**
     * sendMail - Method to send mail regarding status of overall test for users
     * @param script
     * @param subject
     * @param body
     * @return
     */
    static sendMail(script, subject, body) {
        /** plugin used to send user email on status of overall test results after test execution*/
        script.emailext(
                to: Common.emailRecipients,
                subject: subject,
                body: body,
                recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']],
                mimeType: "text/html"
        )
    }

    /**
     * sendNotificationMailPostTestExecution : Method to send notification mail to users
     * regarding the overall status of the test summary after the successful test execution.
     * @param script
     * @return
     */
    static sendNotificationMailPostTestExecution(script) {
        def productImageVersion = script.sh(
                script: "echo ${Common.productImage}| awk -F':' '{print \$2}'",
                returnStdout: true
        ).trim()
        Log.info("product Image version :: ${productImageVersion}")

        /** Local Variable declaration for this method */
        /** overallTestList - variable to convert string to list elements with whitespace as delimiter for split function */
        List overallTestList = (overallExecutedTestCaseList == null) ? [] : overallExecutedTestCaseList.split()

        /** Generating the subject and the mail body for mail notification */
        def subject = "Test Summary for build '[${script.env.BUILD_NUMBER}]' is in '[${Test.testStatus}]' status."
        def body = """<p font-family:verdana,courier,arial,helvetica;>Hi,</p>
"""

        body = body + """
<p font-family:verdana,courier,arial,helvetica;>QA tests has finished execution and please find the detailed status below:</p>
<html lang="en">
  <head>
    <title>Test Summary</title>
    <base target="_blank"></base>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"></meta>
    <meta name="viewport" content="width=320"></meta>
    <style type="text/css"> 
    p {
      font-family: verdana,courier,arial,helvetica,sans-serif;
     }
    </style>
  </head>
  <body style="font-family:verdana,courier,arial,helvetica;padding:0; margin:0; -webkit-text-size-adjust:none; width:100%;">
    <div>
      <table border="1" style="cellpadding:2; cellspacing: 2; width:100%;">
        <tr>
          <th colspan="1" valign="center" align="left"><h3>&nbsp;&nbsp;&nbsp;JOB ID&nbsp;:&nbsp;${
            script.env.BUILD_NUMBER
        }</h3></th>
          <th colspan="2" valign="center" align="left"><h3>&nbsp;&nbsp;&nbsp;TEST STATUS&nbsp;:&nbsp;${
            Test.testStatus.toUpperCase()
        }</h3></th>
        </tr>
        <tr>
          <th colspan="3">&nbsp;</th>
        </tr>
        <tr>
          <th colspan="1" align="left">&nbsp;&nbsp;&nbsp;Sl. No.</th>
          <th colspan="1" align="left">&nbsp;&nbsp;&nbsp;Test Cases Executed</th>
          <th colspan="1" align="left">&nbsp;&nbsp;&nbsp;Results</th>
        </tr>
        <tr>
          <td align="left">
"""
        for (int i = 1; i <= totalSucDifSkipCasesCount; i++) {
            body = body + """
              <p>&nbsp;&nbsp;&nbsp;$i.</p>
"""
        }
        body = body + """
          </td>
          <td align="left">
"""
        for (String overallTestCase : overallTestList) {
            body = body + """
                    <p>&nbsp;&nbsp;&nbsp;${overallTestCase.trim()}</p>
"""
        }
        body = body + """
          </td>
          <td align="left">
"""
        for (String overallTestCase : overallTestList) {
            if (overallTestCase.endsWith(".suc")) {
                body = body + """
                            <p>&nbsp;&nbsp;&nbsp;<font color="green">PASSED</font></p>
"""
            } else if (overallTestCase.endsWith(".dif")) {
                body = body + """
                            <p>&nbsp;&nbsp;&nbsp;<font color="red">FAILED</font></p>
"""
            } else if (overallTestCase.endsWith(".skip")) {
                body = body + """
                            <p>&nbsp;&nbsp;&nbsp;<font color="blue">SKIPPED</font></p>
"""
            }
        }
        body = body + """
           </td>          
         </tr>
"""
        body = body + """
    </table>
    <br/>
    <table border="1" style="cellpadding:10; cellspacing: 4; width:100%;">
      <tr align="center">
       <th colspan="1"># of Suc</th>
       <th colspan="1"># of Dif</th>
       <th colspan="1"># of Skip</th>
       <th colspan="1"># of Overall Test Cases Run</th>
      </tr>
      <tr>
       <td align="right" valign="center">${sucCountValue}&nbsp;&nbsp;&nbsp;</td>
       <td align="right" valign="center">${difCountValue}&nbsp;&nbsp;&nbsp;</td>
       <td align="right" valign="center">${skipCountValue}&nbsp;&nbsp;&nbsp;</td>
       <td align="right" valign="center">${totalSucDifSkipCasesCount}&nbsp;&nbsp;&nbsp;</td>
      </tr>
  </table>
</div>
</body>
</html>
<p font-family:verdana,courier,arial,helvetica;>The logs and results for this run is available at Artifactory Location : </p>
<p font-family:verdana,courier,arial,helvetica;>https://artifacthub.oraclecorp.com/fmwk8s-dev-local/com/oracle/fmwk8sval/logs/${
            Common.productName
        }/${productImageVersion}/${Common.runId}/</p>
<p font-family:verdana,courier,arial,helvetica;>Thanks & Regards,</p>
<p font-family:verdana,courier,arial,helvetica;>FMW K8S Team</p>
"""
        sendMail(script, subject, body)
    }
}
