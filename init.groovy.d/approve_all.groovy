import jenkins.model.Jenkins
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval

def j = Jenkins.instance
def approval = ScriptApproval.get()

approval.pendingScripts.each { pending ->
  approval.approveScript(pending.hash)
}

approval.pendingSignatures.each { pending ->
  approval.approveSignature(pending.signature)
}

j.save()
