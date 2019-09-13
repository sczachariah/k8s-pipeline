def call(String buildResult) {
    if (buildResult == "FAILURE") {
        echo "INFO: Notifying Slack about FAILURE"
        slackSend color: "danger", message: "Job: ${env.JOB_NAME} with buildnumber ${env.BUILD_NUMBER} was failed"
    } else if (buildResult == "UNSTABLE") {
        echo "INFO: Notifying Slack about UNSTABLE"
        slackSend color: "warning", message: "Job: ${env.JOB_NAME} with buildnumber ${env.BUILD_NUMBER} was unstable"
    }
}
