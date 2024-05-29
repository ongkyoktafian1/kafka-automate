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
        KAFKA_CLUSTER_CHOICES_FILE = 'kafka_cluster_choices.txt'
    }

    stages {
        stage('Initialize Parameters') {
            steps {
                script {
                    // Generate Kafka Cluster Choices
                    def kafkaClusters = sh(script: "find . -maxdepth 1 -type d -name 'kafka-*' -exec basename {} \\;", returnStdout: true).trim().split('\n')
                    writeFile file: KAFKA_CLUSTER_CHOICES_FILE, text: kafkaClusters.join('\n')

                    def kafkaClusterChoices = readFile(KAFKA_CLUSTER_CHOICES_FILE).split('\n').collect { it.trim() }
                    currentBuild.rawBuild.getParent().addProperty(new hudson.model.ParametersDefinitionProperty(
                        new hudson.model.StringParameterDefinition('JIRA_URL', '', 'Enter the JIRA URL'),
                        new hudson.model.ChoiceParameterDefinition('KAFKA_CLUSTER', kafkaClusterChoices.join('\n'), 'Select the Kafka cluster')
                    ))
                }
            }
        }

        stage('Input Parameters') {
            steps {
                script {
                    // Prompt user for input
                    def userParams = input message: 'Provide parameters', parameters: [
                        string(name: 'JIRA_URL', description: 'Enter the JIRA URL'),
                        choice(name: 'KAFKA_CLUSTER', choices: readFile(KAFKA_CLUSTER_CHOICES_FILE).split('\n'), description: 'Select the Kafka cluster')
                    ]
                    env.JIRA_URL = userParams['JIRA_URL']
                    env.KAFKA_CLUSTER = userParams['KAFKA_CLUSTER']
                }
            }
        }

        stage('Clone Repository') {
            steps {
                container('python') {
                    deleteDir()  // Ensure the workspace is clean
                    sh 'apt-get update && apt-get install -y git tzdata'
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
                        def jiraKey = env.JIRA_URL.tokenize('/').last()
                        env.JIRA_KEY = jiraKey
                    }
                }
            }
        }

        stage('Publish to Kafka') {
            steps {
                container('python') {
                    script {
                        def kafkaCluster = env.KAFKA_CLUSTER
                        def jiraKey = env.JIRA_KEY
                        def jsonDirectory = "${env.WORKSPACE}/${kafkaCluster}/${jiraKey}"
                        def jsonFilePattern = "${jsonDirectory}/*.json"

                        def jsonFiles = sh(script: "ls ${jsonFilePattern}", returnStdout: true).trim().split("\n")

                        jsonFiles.each { jsonFile ->
                            if (fileExists(jsonFile)) {
                                def configData = readJSON file: jsonFile
                                def topic = configData.topic
                                def messages = configData.messages

                                def messagesJson = new groovy.json.JsonBuilder(messages).toPrettyString()
                                writeFile file: 'messages.json', text: messagesJson

                                def kafkaBroker = ""
                                if (kafkaCluster == "kafka-cluster-platform") {
                                    kafkaBroker = "kafka-1.platform.stg.ajaib.int:9092"
                                } else if (kafkaCluster == "kafka-cluster-data") {
                                    kafkaBroker = "kafka-1.platform.stg.ajaib.int:9092"
                                }

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
