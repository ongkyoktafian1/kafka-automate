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
                        def teamDir = "${team}"
                        
                        // Find the latest JSON file in the team's directory
                        def latestFile = sh(script: "ls -t ${teamDir}/*.json | head -n 1", returnStdout: true).trim()
                        
                        // Read and parse the latest JSON file
                        def config = readFile(file: latestFile)
                        def configData = readJSON text: config
                        def topic = configData.topic
                        def messages = configData.messages

                        // Create the Python script
                        writeFile file: 'kafka_producer.py', text: """
from kafka import KafkaProducer
import sys
import json

topic = sys.argv[1]
messages = json.loads(sys.argv[2])

producer = KafkaProducer(bootstrap_servers='kafka-1.platform.stg.ajaib.int:9092')
for message in messages:
    producer.send(topic, value=message.encode('utf-8'))
producer.flush()
"""

                        // Prepare the messages as a JSON string
                        def messageList = groovy.json.JsonOutput.toJson(messages)
                        
                        // Run the Python script
                        sh "python kafka_producer.py ${topic} '${messageList}'"
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
