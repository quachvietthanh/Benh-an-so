pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        BACKEND_DIR = 'backend'
        IMAGE_NAME = 'benh-an-so-backend'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        TEST_IMAGE_NAME = 'benh-an-so-backend-test'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Check Agent OS') {
            steps {
                sh 'uname -a'
                sh 'pwd'
                sh 'docker --version'
            }
        }

        stage('Build & Test Backend') {
            steps {
                dir("${BACKEND_DIR}") {
                    sh '''
                        set -eu

                        rm -rf target/surefire-reports target/exit-code
                        mkdir -p target/surefire-reports

                        docker build --target test -t ${TEST_IMAGE_NAME}:${IMAGE_TAG} .

                        TEST_CONTAINER=$(docker create ${TEST_IMAGE_NAME}:${IMAGE_TAG})
                        trap 'docker rm -f "$TEST_CONTAINER" >/dev/null 2>&1 || true' EXIT

                        docker cp "$TEST_CONTAINER:/test-results/." target/
                        TEST_EXIT_CODE=$(cat target/exit-code)

                        if [ "$TEST_EXIT_CODE" -ne 0 ]; then
                          echo "Backend tests failed inside Docker build."
                          exit "$TEST_EXIT_CODE"
                        fi
                    '''
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build Runtime Image') {
            steps {
                dir("${BACKEND_DIR}") {
                    sh '''
                        docker build --target runtime \
                          -t ${IMAGE_NAME}:${IMAGE_TAG} \
                          -t ${IMAGE_NAME}:latest .
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'CI PASSED'
        }
        failure {
            echo 'CI FAILED'
        }
    }
}
