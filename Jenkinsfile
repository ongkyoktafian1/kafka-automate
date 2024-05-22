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
                volumeMounts:
                - name: jenkins-home
                  mountPath: /var/jenkins_home
              volumes:
              - name: jenkins-home
                hostPath:
                  path: /var/jenkins_home
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

        stage('Copy Init Script') {
            steps {
                container('python') {
                    script {
                        // Ensure the directory exists
                        sh 'mkdir -p /var/jenkins_home/init.groovy.d'
                        // Copy the approve_all.groovy script to the init.groovy.d directory
                        sh 'cp init.groovy.d/approve_all.groovy /var/jenkins_home/init.groovy.d/approve_all.groovy'
                    }
                }
            }
        }

        stage('Debug File Timestamps') {
            steps {
                container('python') {
                    script {
                        def team = params.TEAM
                        def teamDir = "${team}"

                        // Use git log to find the most recently committed file
                        def latestFile = sh(script: """
                            git log -1 --name-only --pretty=format: -- ${teamDir}/*.json | head -n 1
                        """, returnStdout: true).trim()

                        echo "Latest file from GitHub: ${latestFile}"

                        // Store the latest file path in an environment variable
                        env.LATEST_FILE = latestFile
                    }
                }
            }
        }

        stage('Publish Messages to Kafka') {
            steps {
                container('python') {
                    script {
                        def latestFile = env.LATEST_FILE
                        echo "Latest file to be processed: ${latestFile}"

                        // Check if the file exists
                        if (fileExists(latestFile)) {
                            // Read the content of the latest file
                            def config = readFile(file: latestFile)
                            echo "Content of the latest file: ${config}"

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

producer = KafkaProducer(bootstrap_servers='kafka-1.platform.stg.ajaib.int:9092')
for message in messages:
    producer.send(topic, value=message.encode('utf-8'))
producer.flush()
"""

                            // Run the Python script
                            sh "python kafka_producer.py ${topic} \"\$(cat messages.json)\""
                        } else {
                            error "File not found: ${latestFile}"
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
