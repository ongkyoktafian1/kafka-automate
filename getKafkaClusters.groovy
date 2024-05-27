def call() {
    def clusters = []
    def workspace = new FilePath(Jenkins.getInstance().getWorkspaceFor(this.getClass()))
    def clusterDirs = workspace.listDirectories().findAll { it.name.startsWith('kafka-cluster-') }

    clusterDirs.each {
        clusters.add(it.name.replace('kafka-cluster-', ''))
    }
    return clusters
}
