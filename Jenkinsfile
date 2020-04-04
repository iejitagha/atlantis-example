pipeline {

    agent any

    options {
        ansiColor('xterm')
        timestamps()
        disableConcurrentBuilds()
        skipStagesAfterUnstable()
    }

    stages {

        stage('load_em_up') {
            when { branch 'master' }
            steps {
                jobDsl(targets: 'job_dsl.groovy')
            }
        }

    }

}
