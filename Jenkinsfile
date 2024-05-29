node {
    def kafkaClusterChoicesFile = 'kafka_cluster_choices.txt'
    def kafkaClusterChoices = ''

    try {
        stage('Generate Kafka Cluster Choices') {
            // Generate choices from the provided KAFKA_CLUSTERS parameter
            def kafkaClusters = params.KAFKA_CLUSTERS.split(',').collect { it.trim() }
            kafkaClusterChoices = kafkaClusters.join('\n')
            writeFile file: kafkaClusterChoicesFile, text: kafkaClusterChoices
        }

        def dynamicPipeline = """
node {
    def kafkaClusterChoicesFile = 'kafka_cluster_choices.txt'
    def kafkaClusterChoices = readFile(file: kafkaClusterChoicesFile).split('\\\\n').collect { it.trim() }.join('\\\\n')

    properties([
        parameters([
            string(name: 'JIRA_URL', description: 'Enter the JIRA URL'),
            choice(name: 'KAFKA_CLUSTER', choices: kafkaClusterChoices, description: 'Select the Kafka cluster')
        ])
    ])

    podTemplate(yaml: '''
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
''') {
        node(POD_LABEL) {
            try {
                stage('Clone Repository') {
                    checkout scm: [\$class: 'GitSCM', branches: [[name: '*/main']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/ongkyoktafian1/kafka-automate.git']]]
                }

                stage('Install Dependencies') {
                    container('python') {
                        sh 'pip install kafka-python'
                    }
                }

                stage('Add Git Exception') {
                    container('python') {
                        sh 'git config --global --add safe.directory /home/jenkins/agent/workspace/ongky_test'
                    }
                }

                stage('Extract JIRA Key') {
                    container('python') {
                        script {
                            // Extract the JIRA key from the URL
                            env.JIRA_KEY = params.JIRA_URL.tokenize('/').last()
                        }
                    }
                }

                stage('Publish to Kafka') {
                    container('python') {
                        script {
                            def kafkaCluster = params.KAFKA_CLUSTER
                            def jiraKey = env.JIRA_KEY
                            def jsonDirectory = "\${WORKSPACE}/\${kafkaCluster}/\${jiraKey}"
                            def jsonFilePattern = "\${jsonDirectory}/*.json"

                            // Find all JSON files in the specified directory
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
            } catch (Exception e) {
                currentBuild.result = 'FAILURE'
                throw e
            } finally {
                stage('Post Actions') {
                    if (currentBuild.result == 'SUCCESS') {
                        echo 'Messages published successfully!'
                    } else {
                        echo 'Failed to publish messages.'
                    }
                }
            }
        }
    }
}
"""
        writeFile file: 'dynamic_pipeline.groovy', text: dynamicPipeline
        load 'dynamic_pipeline.groovy'
    } catch (Exception e) {
        currentBuild.result = 'FAILURE'
        throw e
    }
}
