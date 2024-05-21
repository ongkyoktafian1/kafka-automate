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

        stage('Approve All Script') {
            steps {
                script {
                    approveAllScripts()
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
                        
                        // Read the content of the latest JSON file
                        def config = readFile(file: latestFile)
                        def configData = readJSON text: config
                        def topic = configData.topic
                        def messages = configData.messages

                        // Convert the messages array to a JSON string
                        def messagesJson = new groovy.json.JsonBuilder(messages).toPrettyString()

                        // Write the JSON string to the messages.json file
                        writeFile file: 'messages.json', text: messagesJson

                        // Create the Python script
                        writeFile file: 'kafka_producer.py', text: """
from kafka import KafkaProducer
import json

topic = sys.argv[1]
messages = json.loads(sys.argv[2])

producer = KafkaProducer(bootstrap_servers='kafka-1.platform.stg.ajaib.int:9092')
for message in messages:
    producer.send(topic, value=message.encode('utf-8'))
producer.flush()
"""

                        // Run the Python script
                        sh "python kafka_producer.py ${topic} \"\$(cat messages.json)\""
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

def approveAllScripts() {
    ScriptApproval scriptApproval = ScriptApproval.get()
    def hashesToApprove = []
    scriptApproval.pendingScripts.each {
        hashesToApprove.add(it.hash)
    }
    for (String hash : hashesToApprove) {
        scriptApproval.approveScript(hash)
    }
}
