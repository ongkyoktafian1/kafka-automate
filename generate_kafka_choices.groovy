import jenkins.model.*
import hudson.model.*

def kafkaClusters = ["kafka-cluster-platform", "kafka-cluster-data"] // Default values if not reading from a file or directory

// Create a list of choices as a newline-separated string
def choices = kafkaClusters.join("\n")

// Get the current Jenkins job
def job = Jenkins.instance.getItemByFullName(env.JOB_NAME)
def property = job.getProperty(ParametersDefinitionProperty)
def parameterDefinitions = property.getParameterDefinitions()

// Find the KAFKA_CLUSTER parameter and update its choices
parameterDefinitions.each { param ->
    if (param.name == "KAFKA_CLUSTER") {
        param.setChoices(choices.split("\n"))
    }
}

// Save the job to persist the changes
job.save()
