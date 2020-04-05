pipeline {

    agent any

    options {
        ansiColor('xterm')
        timestamps()
        disableConcurrentBuilds()
        skipStagesAfterUnstable()
        // sendSplunkConsoleLog()
    }
 
    // Test 
    parameters {
        string(name: 'Rally_Ticket_Number',
                defaultValue: '',
                description: 'What is the Rally Number?')

        string(name: 'Change_Request_Number',
                defaultValue: '',
                description: 'If this is a Production change, what is the CR number?')

        string(name: 'Code_branch',
                defaultValue: 'master',
                description: 'Override branch name. Branches not called "master" will not apply changes to infrastructure.')
    }

    environment {
        // awsm_code = 'git@github.worldpay.io:cloudfoundation/cfs-api.git'
        awsm_code = 'git@github.com:iejitagha/atlantis-example.git'
    }

    stages {

        stage('prepare') {
            steps {
                cleanWs()
                git url: env.awsm_code
                sh "echo \"This is a test\""
            }
        }

        stage('Test1') {
            steps {                   
                sh "echo \"This is a test1\""
            }
         }
        
        // Run API Test pipeline here

        stage('cleanup') {
            steps {
                sh "echo \"This is a the clean up step\""
            }
        }
    }
}
