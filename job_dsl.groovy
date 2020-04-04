pipelineJob('automation') {
    definition {
        cps {
            script(readFileFromWorkspace('automation.groovy'))
        }
    }
}
