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

        stage('Publish Messages to Kafka') {
            steps {
                container('python') {
                    script {
                        def team = params.TEAM
                        def configPath = "${team}/kafka_config.json"
                        def config = readFile(file: configPath)
                        def configData = readJSON text: config
                        def topic = configData.topic
                        def messages = configData.messages

                        writeFile file: 'kafka_producer.py', text: """
from kafka import KafkaProducer
import sys

topic = sys.argv[1]
messages = sys.argv[2:]

producer = KafkaProducer(bootstrap_servers='kafka-1.platform.stg.ajaib.int:9092')
for message in messages:
    producer.send(topic, value=message.encode('utf-8'))
producer.flush()
"""

                        def messageList = messages.collect { "'${it}'" }.join(' ')
                        sh "python kafka_producer.py ${topic} ${messageList}"
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Messages published successfully!'
        }
        failure {
            echo 'Failed to publish messages.'
        }
    }
}
