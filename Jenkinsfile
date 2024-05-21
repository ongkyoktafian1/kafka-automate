pipeline {
    agent {
        kubernetes {
            yaml """
            apiVersion: v1
            kind: Pod
            spec:
              containers:
              - name: python
                image: python:3.9-slim
                command:
                - cat
                tty: true
            """
        }
    }

    parameters {
        choice(name: 'TEAM', choices: ['money', 'payment', 'core'], description: 'Select the team')
    }

    stages {
        stage('Clone Repository') {
            steps {
                git url: 'https://github.com/ongkyoktafian1/kafka-automate.git', branch: 'main'
            }
        }

        stage('Install Dependencies') {
            steps {
                container('python') {
                    sh 'pip install kafka-python'
                }
            }
        }

        stage('Publish Message to Kafka') {
            steps {
                container('python') {
                    script {
                        def team = params.TEAM
                        def configPath = "${team}/kafka_config.json"
                        def config = readFile(file: configPath)
                        def configData = readJSON text: config
                        def topic = configData.topic
                        def message = configData.message

                        writeFile file: 'kafka_producer.py', text: """
from kafka import KafkaProducer
import sys

topic = sys.argv[1]
message = sys.argv[2]

producer = KafkaProducer(bootstrap_servers='kafka-1.platform.stg.ajaib.int:9092')
producer.send(topic, value=message.encode('utf-8'))
producer.flush()
"""
                        sh "python kafka_producer.py ${topic} '${message}'"
                    }
                }
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
