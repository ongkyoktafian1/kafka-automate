pipeline {
    agent any

    environment {
        KAFKA_CLUSTER_CHOICES_FILE = 'kafka_cluster_choices.txt'
    }

    parameters {
        string(name: 'JIRA_URL', description: 'Enter the JIRA URL')
        string(name: 'KAFKA_CLUSTERS', defaultValue: 'kafka-cluster-platform,kafka-cluster-data', description: 'Comma-separated list of Kafka clusters')
    }

    stages {
        stage('Generate Kafka Cluster Choices') {
            steps {
                script {
                    // Generate choices from the provided KAFKA_CLUSTERS parameter
                    def kafkaClusters = params.KAFKA_CLUSTERS?.split(',')?.collect { it.trim() }
                    writeFile file: KAFKA_CLUSTER_CHOICES_FILE, text: kafkaClusters.join('\n')
                }
            }
        }

        stage('Dynamic Pipeline') {
            steps {
                script {
                    // Read the dynamically generated Kafka clusters
                    def kafkaClusterChoices = readFile(env.KAFKA_CLUSTER_CHOICES_FILE).split('\n').collect { it.trim() }
                    def kafkaClusterChoicesFormatted = kafkaClusterChoices.join('\n')

                    // Define the dynamic pipeline
                    def dynamicPipeline = """
pipeline {
    agent {
        kubernetes {
            yaml \"""
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
            \"""
        }
    }

    parameters {
        string(name: 'JIRA_URL', description: 'Enter the JIRA URL')
        choice(name: 'KAFKA_CLUSTER', choices: '${kafkaClusterChoicesFormatted}', description: 'Select the Kafka cluster')
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
                        def jsonDirectory = "\${env.WORKSPACE}/\${kafkaCluster}/\${jiraKey}"
                        def jsonFilePattern = "\${jsonDirectory}/*.json"

                        /// Find all JSON files in the specified directory
                        def jsonFiles = sh(script: "ls \${jsonFilePattern}", returnStdout: true).trim().split("\\n")

                        jsonFiles.each { jsonFile ->
                            if (fileExists(jsonFile)) {
                                def configData = readJSON file: jsonFile
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
                                    kafkaBroker = "kafka-1.platform.stg.ajaib.int:9092"
                                }

                                // Create the Python script file
                                writeFile file: 'kafka_producer.py', text: '''
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
'''

                                // Run the Python script
                                sh "python kafka_producer.py \${topic} \"\$(cat messages.json)\" \${kafkaBroker}"
                            } else {
                                error "File not found: \${jsonFile}"
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
"""
                    writeFile file: 'dynamic_pipeline.groovy', text: dynamicPipeline
                    load 'dynamic_pipeline.groovy'
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline executed successfully!'
        }
        failure {
            echo 'Pipeline execution failed.'
        }
    }
}
