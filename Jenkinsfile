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

    environment {
        KAFKA_CLUSTERS = ''
    }

    stages {
        stage('Discover Kafka Clusters') {
            steps {
                script {
                    // Find all Kafka cluster directories
                    def clusters = sh(script: """
                        find . -maxdepth 1 -mindepth 1 -type d -name 'kafka-cluster-*' | sed 's|./||'
                    """, returnStdout: true).trim().split('\n')

                    if (clusters.size() == 0) {
                        error "No Kafka clusters found."
                    }

                    echo "Discovered Kafka clusters: ${clusters.join(', ')}"

                    // Store the discovered clusters in an environment variable
                    KAFKA_CLUSTERS = clusters.join(',')
                }
            }
        }
    }

    parameters {
        string(name: 'JIRA_URL', description: 'Enter the JIRA URL')
        choice(name: 'KAFKA_CLUSTER', choices: KAFKA_CLUSTERS, description: 'Select the Kafka cluster')
    }

    // Rest of the pipeline stages...
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
                        def kafkaCluster = params.KAFKA_CLUSTER
                        def jiraKey = env.JIRA_KEY

                        // Find all JSON files in the JIRA key directory under the selected Kafka cluster
                        def jsonFiles = sh(script: """
                            find ${kafkaCluster}/${jiraKey} -type f -name '*.json'
                        """, returnStdout: true).trim().split('\n')

                        if (jsonFiles.size() == 0) {
                            error "No JSON files found for JIRA key: ${jiraKey} in Kafka cluster: ${kafkaCluster}"
                        }

                        echo "JSON files to be processed: ${jsonFiles.join(', ')}"

                        // Store the JSON files in an environment variable
                        env.JSON_FILES = jsonFiles.join(',')
                    }
                }
            }
        }

        stage('Read Kafka Broker Config') {
            steps {
                container('python') {
                    script {
                        def kafkaCluster = params.KAFKA_CLUSTER
                        def kafkaConfigFile = "${kafkaCluster}/kafka_broker.config"

                        // Read the Kafka broker configuration
                        def kafkaConfig = readProperties file: kafkaConfigFile
                        def kafkaBroker = kafkaConfig['bootstrap_servers']

                        if (!kafkaBroker) {
                            error "Kafka broker configuration not found in ${kafkaConfigFile}"
                        }

                        echo "Using Kafka broker: ${kafkaBroker}"

                        // Store the Kafka broker in an environment variable
                        env.KAFKA_BROKER = kafkaBroker
                    }
                }
            }
        }

        stage('Publish Messages to Kafka') {
            steps {
                container('python') {
                    script {
                        def jsonFiles = env.JSON_FILES.split(',')
                        def kafkaBroker = env.KAFKA_BROKER

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
