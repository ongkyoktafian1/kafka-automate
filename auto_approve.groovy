// auto_approve.groovy
pipeline {
    agent any

    stages {
        stage('Auto Approve') {
            steps {
                script {
                    def userInput = input(
                        id: 'AutoApprove', message: 'Do you want to proceed?', parameters: [
                            booleanParam(defaultValue: true, description: 'Auto-approve the process', name: 'Proceed')
                        ]
                    )
                    if (!userInput) {
                        error "Pipeline aborted by user"
                    }
                }
            }
        }
    }
}
