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
        stage('Auto Approve Scripts') {
            steps {
                build job: 'AutoApproveJob', wait: true
            }
        }

        stage('Generate Kafka Cluster Choices') {
            steps {
                script {
                    def kafkaClusters = params.KAFKA_CLUSTERS?.split(',')?.collect { it.trim() }
                    writeFile file: KAFKA_CLUSTER_CHOICES_FILE, text: kafkaClusters.join('\n')
                }
            }
        }

        stage('Read Kafka Cluster Choices') {
            steps {
                script {
                    def kafkaClusterChoices = readFile(KAFKA_CLUSTER_CHOICES_FILE).split('\n').collect { it.trim() }
                    properties([
                        parameters([
                            string(name: 'JIRA_URL', description: 'Enter the JIRA URL'),
                            choice(name: 'KAFKA_CLUSTER', choices: kafkaClusterChoices.join('\n'), description: 'Select the Kafka cluster')
                        ])
                    ])
                }
            }
        }

        stage('Clone Repository') {
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
            steps {
                container('python') {
                    sh 'apt-get update && apt-get install -y git tzdata'
                    sh 'rm -rf *'  // Ensure the workspace is clean before cloning
                    sh 'git clone https://github.com/ongkyoktafian1/kafka-automate.git .'
                }
            }
        }

        stage('Install Dependencies') {
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
            steps {
                container('python') {
                    sh 'apt-get update && apt-get install -y git tzdata'
                    sh 'pip install kafka-python'
                }
            }
        }

        stage('Add Git Exception') {
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
            steps {
                container('python') {
                    sh 'apt-get update && apt-get install -y git tzdata'
                    sh 'git config --global --add safe.directory /home/jenkins/agent/workspace/ongky_test'
                }
            }
        }

        stage('Extract JIRA Key') {
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
            steps {
                container('python') {
                    script {
                        def jiraKey = params.JIRA_URL.tokenize('/').last()
                        env.JIRA_KEY = jiraKey
                    }
                }
            }
        }

        stage('Publish to Kafka') {
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
            steps {
                container('python') {
                    script {
                        def kafkaCluster = params.KAFKA_CLUSTER
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
