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

        stage('Create Kafka Producer Script') {
            steps {
                container('python') {
                    // Create the Python script
                    writeFile file: 'kafka_producer.py', text: """
from kafka import KafkaProducer
import json
import sys

topic = sys.argv[1]
messages = json.loads(sys.argv[2])
brokers = sys.argv[3].split(',')

producer = KafkaProducer(
    bootstrap_servers=brokers,  # brokers is a list of broker addresses
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

for message in messages:
    producer.send(topic, value=message)
producer.flush()
"""
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
                        def jsonFiles = sh(script: "ls ${jsonFilePattern}", returnStdout: true).trim().split('\n')

                        jsonFiles.each { jsonFile ->
                            if (fileExists(jsonFile)) {
                                // Read and convert the JSON file using jq
                                def messagesJson = sh(script: "jq -c .messages < ${jsonFile}", returnStdout: true).trim()
                                def topic = sh(script: "jq -r .topic < ${jsonFile}", returnStdout: true).trim()

                                // Write the JSON string to the messages.json file
                                writeFile file: 'messages.json', text: messagesJson

                                // Determine the Kafka brokers based on the selected Kafka cluster
                                def kafkaBrokers = sh(script: "cat kafka-config/${kafkaCluster} | tr '\\n' ',' | sed 's/,\$//'", returnStdout: true).trim()

                                // Run the Python script
                                sh "python kafka_producer.py ${topic} \"${messagesJson.replace('"', '\\"')}\" ${kafkaBrokers}"
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
