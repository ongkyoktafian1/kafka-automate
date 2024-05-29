import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval

ScriptApproval scriptApproval = ScriptApproval.get()
def hashesToApprove = []
scriptApproval.pendingScripts.each {
    hashesToApprove.add(it.hash)
}
hashesToApprove.each { hash ->
    scriptApproval.approveScript(hash)
}
