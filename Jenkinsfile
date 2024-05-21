pipeline {
    agent any

    parameters {
        string(name: 'TOPIC', defaultValue: 'default-topic', description: 'Kafka topic to publish message to')
        string(name: 'MESSAGE', defaultValue: 'default-message', description: 'Message to publish to Kafka topic')
    }

    stages {
        stage('Clone Repository') {
            steps {
                git url: 'https://github.com/ongkyoktafian1/kafka-automate.git', branch: 'main'
            }
        }

        stage('Install Dependencies') {
            steps {
                sh 'pip install kafka-python'
            }
        }

        stage('Publish Message to Kafka') {
            steps {
                sh "python kafka_producer.py ${params.TOPIC} '${params.MESSAGE}'"
            }
        }
    }

    post {
        success {
            echo 'Message published successfully!'
        }
        failure {
            echo 'Failed to publish message.'
        }
    }
}
