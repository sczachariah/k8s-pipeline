package com.oracle.fmwk8s.utility

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.env.Domain
import com.oracle.fmwk8s.env.IngressController
import com.oracle.fmwk8s.test.Test
import java.util.Calendar;
import groovy.time.TimeCategory
import com.oracle.fmwk8s.env.Logging

class ReportUtility {
    
    static def domainURLs

    static printDomainUrls(script) {
        domainURLs = """
-----------Domain URL's------------
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/weblogic/ready
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/console
"""
        if (Common.productId.toString().equalsIgnoreCase("soa")) {
            if (Domain.domainType.toString().toLowerCase().contains("soa") ||
                    Domain.domainType.toString().toLowerCase().contains("osb")) {
                domainURLs = domainURLs + """
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/em
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/soa-infra/
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/soa/composer
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/integration/worklistapp
"""
            }
            if (Domain.domainType.toString().toLowerCase().contains("osb")) {
                domainURLs = domainURLs + """
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/servicebus
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/sbconsole
"""
            }
            if (Domain.domainType.toString().toLowerCase().contains("ess")) {
                domainURLs = domainURLs + """
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/ess
http://${Common.k8sMasterIP}:${IngressController.httplbPort}/EssHealthCheck
"""
            }
        }
        domainURLs = domainURLs + """
-----------------------------------
"""
        script.sh label: "print domain url's",
                script: "printf \"%s\\n\" \"${domainURLs.toString()}\""
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

        if(Common.hoursAfter > 0){
            Calendar calendar = Calendar.getInstance()
            System.out.println("Original = " + calendar.getTime())
            calendar.add(Calendar.HOUR, Integer.parseInt("${Common.hoursAfter}").intValue())
            def hourAfterTime
            hourAfterTime = calendar.getTime()
            body = body + """
<p>The environment is available for use for ${Common.hoursAfter} hours. The environment will be de-commissioned at ${hourAfterTime} </p>
"""
        }

        body = body + """
<p>Please find below the access URLs to the environment:</p>
<p>${domainURLs} </p>
"""
        def domainURL
        if ("${Common.cloud}".equalsIgnoreCase("oci-v1.12.9")){
            domainURL = "https://100.111.150.162:6443/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy/#!/overview?namespace=${Common.domainNamespace}"
        }
        if("${Common.cloud}".equalsIgnoreCase("kubernetes-v1.15.2")){
            domainURL = "https://10.248.90.49:6443/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy/#!/overview?namespace=${Common.domainNamespace}"
        }

        if ("${Common.testType}" != "N/A") {
            body = body + """
<p>QA tests are being executed against the provisioned environment. Domain and Test pods are accessible here : ${domainURL} </p>

<p>A detailed status on test execution will be sent over email once the test execution is completed. </p>
"""
        }else{
            body = body + """
<p>Domain pods are accessible here : ${domainURL} </p>
"""
        }
        if ("${Common.elkEnable}" == "true") {
            body = body + """
<p>Operator and Domain logs are available in Kibana : ${Common.kibanaUrl} </p>
"""
        }

        body = body + """
<p>Regards,</p>
<p>FMW K8S Team</p>
"""
        sendMail(script,subject,body)
    }

    static sendMail(script,subject, body) {
        script.emailext (
            subject: subject,
            body: body,
            recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']],
            mimeType: "text/html"
        )
    }



static sendNotificationMailPostTestExecution(script) {
    def subject = "${Test.testStatus} - '[${script.env.BUILD_NUMBER}]'"
    def body = """<p>Hi,</p>
"""

    body = body + """
<p>QA tests has finished execution and please find the detailed status below:</p>
${Common.testResults}
"""

    body = body + """
<p>The logs and results for this run is available at Artifactory Location : https://artifacthub.oraclecorp.com/fmwk8s-dev-local/com/oracle/fmwk8sval/logs/${Common.productName}/${Logging.productImageVersion}/${Common.runId}/</p>
"""
    body = body + """
<p>Regards,</p>
<p>FMW K8S Team</p>
"""
    sendMail(script,subject,body)
 }
 
}
