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
                - sh
                - -c
                - |
                  apt-get update && apt-get install -y git tzdata
                  cp /usr/share/zoneinfo/Asia/Jakarta /etc/localtime
                  echo "Asia/Jakarta" > /etc/timezone
                  git config --global --add safe.directory /home/jenkins/agent/workspace/ongky_test
                  exec cat
                tty: true
                env:
                - name: TZ
                  value: "Asia/Jakarta"
            """
        }
    }

    parameters {
        string(name: 'JIRA_URL', description: 'Enter the JIRA URL')
        choice(name: 'TEAM', choices: ['money', 'payment', 'core'], description: 'Select the team')
        choice(name: 'KAFKA_CLUSTER', choices: ['kafka-cluster-platform', 'kafka-cluster-data'], description: 'Select the Kafka cluster')
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

        stage('Add Git Exception') {
            steps {
                container('python') {
                    sh 'git config --global --add safe.directory /home/jenkins/agent/workspace/ongky_test'
                }
            }
        }

        stage('Extract JIRA Key') {
            steps {
                container('python') {
                    script {
                        // Extract the JIRA key from the URL
                        def jiraUrl = params.JIRA_URL
                        def jiraKey = jiraUrl.tokenize('/').last()
                        echo "JIRA Key: ${jiraKey}"

                        // Store the JIRA key in an environment variable
                        env.JIRA_KEY = jiraKey
                    }
                }
            }
        }

        stage('Find JSON Files') {
            steps {
                container('python') {
                    script {
                        def team = params.TEAM
                        def jiraKey = env.JIRA_KEY
                        def teamDir = "${team}/${jiraKey}"

                        // Find all JSON files in the JIRA key directory
                        def jsonFiles = sh(script: """
                            find ${teamDir} -type f -name '*.json'
                        """, returnStdout: true).trim().split('\n')

                        if (jsonFiles.length == 0) {
                            error "No JSON files found in directory: ${teamDir}"
                        }

                        echo "JSON files to be processed: ${jsonFiles.join(', ')}"

                        // Store the JSON files in an environment variable
                        env.JSON_FILES = jsonFiles.join(',')
                    }
                }
            }
        }

        stage('Publish Messages to Kafka') {
            steps {
                container('python') {
                    script {
                        def jsonFiles = env.JSON_FILES.split(',')
                        def kafkaCluster = params.KAFKA_CLUSTER

                        jsonFiles.each { jsonFile ->
                            echo "Processing file: ${jsonFile}"

                            // Check if the file exists
                            if (fileExists(jsonFile)) {
                                // Read the content of the file
                                def config = readFile(file: jsonFile)
                                echo "Content of the file: ${config}"

                                def configData = readJSON text: config
                                def topic = configData.topic
                                def messages = configData.messages

                                // Convert the messages array to a JSON string
                                def messagesJson = new groovy.json.JsonBuilder(messages).toPrettyString()

                                // Write the JSON string to the messages.json file
                                writeFile file: 'messages.json', text: messagesJson

                                // Determine the Kafka broker based on the selected Kafka cluster
                                def kafkaBroker = ""
                                if (kafkaCluster == "kafka-cluster-platform") {
                                    kafkaBroker = "kafka-1.platform.stg.ajaib.int:9092"
                                } else if (kafkaCluster == "kafka-cluster-data") {
                                    kafkaBroker = "kafka-1.data.stg.ajaib.int:9092"
                                }

                                // Create the Python script
                                writeFile file: 'kafka_producer.py', text: """
from kafka import KafkaProducer
import json
import sys

topic = sys.argv[1]
messages = json.loads(sys.argv[2])
broker = sys.argv[3]

producer = KafkaProducer(bootstrap_servers=broker)
for message in messages:
    producer.send(topic, value=message.encode('utf-8'))
producer.flush()
"""

                                // Run the Python script
                                sh "python kafka_producer.py ${topic} \"\$(cat messages.json)\" ${kafkaBroker}"
                            } else {
                                error "File not found: ${jsonFile}"
                            }
                        }
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
