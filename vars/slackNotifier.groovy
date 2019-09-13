def call(String buildResult) {
    if (buildResult == "FAILURE") {
        echo "INFO: Notifying Slack about FAILURE"
        slackSend color: "danger", message: "Job: ${env.JOB_NAME} - build #${env.BUILD_NUMBER} - ${currentBuild.displayName} has failed.\nRefer Logs at ${env.BUILD_URL}."
    } else if (buildResult == "UNSTABLE") {
        echo "INFO: Notifying Slack about UNSTABLE"
        slackSend color: "warning", message: "Job: ${env.JOB_NAME} - build #${env.BUILD_NUMBER} - ${currentBuild.displayName} is unstable.\nRefer Logs at ${env.BUILD_URL}."
    }
}
