import jenkins.model.*
import hudson.security.*

def approveAll = [
    'method groovy.json.JsonBuilder new java.lang.Object',
    'method groovy.json.JsonBuilder toPrettyString',
    'staticMethod groovy.json.JsonOutput prettyPrint',
    'method java.util.ArrayList size'
]

def scriptApproval = ScriptApproval.get()
approveAll.each { 
    scriptApproval.approveSignature(it)
}
