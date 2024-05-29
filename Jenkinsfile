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
                  apt-get update && apt-get install -y git tzdata jq
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
        choice(name: 'KAFKA_CLUSTER', choices: ['kafka-cluster-platform', 'kafka-cluster-data'], description: 'Select the Kafka cluster')
    }

    stages {
        stage('Auto Approve Scripts') {
            steps {
                script {
                    // Trigger the AutoApproveJob
                    build job: 'AutoApproveJob', wait: true
                }
            }
        }

        stage('Clone Repository') {
            steps {
                container('python') {
                    deleteDir()  // Ensure the workspace is clean
                    sh 'git clone https://github.com/ongkyoktafian1/kafka-automate.git .'
                }
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
                        def jiraKey = params.JIRA_URL.tokenize('/').last()
                        env.JIRA_KEY = jiraKey
                    }
                }
            }
        }

        stage('Publish to Kafka') {
            steps {
                container('python') {
                    script {
                        def kafkaCluster = params.KAFKA_CLUSTER
                        def jiraKey = env.JIRA_KEY
                        def jsonDirectory = "${env.WORKSPACE}/${kafkaCluster}/${jiraKey}"
                        def jsonFilePattern = "${jsonDirectory}/*.json"

                        // Find all JSON files in the specified directory
                        def jsonFiles = sh(script: "ls ${jsonFilePattern}", returnStdout: true).trim().split('\\n')

                        jsonFiles.each { jsonFile ->
                            if (fileExists(jsonFile)) {
                                // Read and convert the JSON file using jq
                                def messagesJson = sh(script: "jq -c . < ${jsonFile}", returnStdout: true).trim()

                                def configData = readJSON file: jsonFile
                                def topic = configData.topic

                                // Write the JSON string to the messages.json file
                                writeFile file: 'messages.json', text: messagesJson

                                // Determine the Kafka broker based on the selected Kafka cluster
                                def kafkaBroker = ""
                                if (kafkaCluster == "kafka-cluster-platform") {
                                    kafkaBroker = "kafka-1.platform.stg.ajaib.int:9092"
                                } else if (kafkaCluster == "kafka-cluster-data") {
                                    kafkaBroker = "kafka-1.platform.stg.ajaib.int:9092"
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
                                sh "python kafka_producer.py ${topic} \"${messagesJson}\" ${kafkaBroker}"
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
