import jenkins.model.*
import hudson.model.*

def kafkaClusters = ["kafka-cluster-platform", "kafka-cluster-data"] // Replace with your dynamic cluster list

// Create a list of choices as a newline-separated string
def choices = kafkaClusters.join("\n")

// Get the current Jenkins job
def jobName = 'ongky_test' // Replace with your main pipeline job name
def job = Jenkins.instance.getItemByFullName(jobName)
def property = job.getProperty(ParametersDefinitionProperty)
def parameterDefinitions = property.getParameterDefinitions()

// Find the KAFKA_CLUSTER parameter and update its choices
parameterDefinitions.each { param ->
    if (param.name == "KAFKA_CLUSTER") {
        param.choices = choices.split("\n")
    }
}

// Save the job to persist the changes
job.save()
