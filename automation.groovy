pipeline {

    agent any

    options {
        ansiColor('xterm')
        timestamps()
        disableConcurrentBuilds()
        skipStagesAfterUnstable()
        sendSplunkConsoleLog()
    }

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
        awsm_code = 'git@github.worldpay.io:cloudfoundation/cfs-api.git'
    }

    stages {

        stage('prepare') {
            steps {
                cleanWs()
                git url: env.awsm_code, branch: params.Code_branch
                dir('common') {
                    sh 'docker build --file Dockerfiles/ajv --tag ajv .'
                    sh 'docker build --file Dockerfiles/infr --tag taurus/cli .'
                }
            }
        }

        stage('planning') {
            steps {
                withCredentials([file(credentialsId: 'svc_account_management_worker', variable: 'CREDS_FILE')]) {
                    sh "docker run --rm -v $CREDS_FILE:/root/.aws/credentials -v `pwd`:/app -w /app/terraform/cloudfoundation-automation taurus/cli ./cloudautomation-plan-npe.sh"
            }
        }


        stage('human_initiate') {
            steps {
                script {
                    try {
                        timeout(time: 12, unit: 'HOURS') {
                            input 'Initiate "terraform apply" of the plan above?'
                        }
                    }
                    catch (e) {
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }

        stage('applying') {
            steps {
                withCredentials([file(credentialsId: 'svc_account_management_worker', variable: 'CREDS_FILE')]) {
                    sh "docker run --rm -v $CREDS_FILE:/root/.aws/credentials -v `pwd`:/app -w /app/terraform/cloudfoundation-automation taurus/cli ./cloudautomation-apply-npe.sh"
                    sh "sleep 30"
                }
            }
        }

        // Run API Test pipeline here

        stage('planning production') {
            steps {
                withCredentials([file(credentialsId: 'svc_account_management_worker', variable: 'CREDS_FILE')]) {
                    sh "docker run --rm -v $CREDS_FILE:/root/.aws/credentials -v `pwd`:/app -w /app/terraform/cloudfoundation-automation taurus/cli ./cloudautomation-plan-prod.sh"
                }
                script {
                    if (params.Code_branch != "master") {
                        echo 'Skipping the rest since branch is not "master".'
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }


        stage('human_approve_production') {
            input {
                id 'peer_reviewed'
                message 'Are the correct parameters supplied?'
                ok 'Yes, I have checked the parameters supplied.'
                submitterParameter 'APPROVER'
            }
            steps {
                echo ''
            }
            post {
                always {
                    wrap([$class: 'BuildUser']) {
                        script {
                            if (env.BUILD_USER_ID == env.APPROVER) {
                                echo 'ERROR - Operator cannot self approve this change!'
                                currentBuild.result = 'UNSTABLE'
                            }
                        }
                    }
                }
            }

        }

        stage('human_initiate for production') {
            steps {
                script {
                    try {
                        timeout(time: 12, unit: 'HOURS') {
                            input 'Initiate "terraform apply" of the plan above?'
                        }
                    }
                    catch (e) {
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }

        stage('applying production') {
            steps {
                withCredentials([file(credentialsId: 'svc_account_management_worker', variable: 'CREDS_FILE')]) {
                    sh "docker run --rm -v $CREDS_FILE:/root/.aws/credentials -v `pwd`:/app -w /app/terraform/cloudfoundation-automation taurus/cli ./cloudautomation-apply.sh"
                    sh "sleep 30"
                }
            }
        }

        // Run API Test pipeline here

        stage('cleanup') {
            steps {
                sh 'docker run --rm -v `pwd`:/app -w /app/aws-iam/roles/ taurus/cli chmod a+rwX --recursive /app'
            }
        }
    }
}
