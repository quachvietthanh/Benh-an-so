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
        MAVEN_IMAGE = 'maven:3.9.9-eclipse-temurin-21'
        MAVEN_CACHE_VOLUME = 'benh-so-an-maven-repo'
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

                        TEST_CONTAINER="benh-so-an-test-${BUILD_NUMBER}"

                        cleanup() {
                          docker rm -f "$TEST_CONTAINER" >/dev/null 2>&1 || true
                        }

                        trap cleanup EXIT
                        cleanup

                        rm -rf target/surefire-reports
                        mkdir -p target/surefire-reports

                        docker volume create ${MAVEN_CACHE_VOLUME} >/dev/null

                        docker create \
                          --name "$TEST_CONTAINER" \
                          -w /app \
                          -v ${MAVEN_CACHE_VOLUME}:/root/.m2 \
                          -v /var/run/docker.sock:/var/run/docker.sock \
                          ${MAVEN_IMAGE} \
                          sh -c 'mvn -B -ntp clean test'

                        docker cp pom.xml "$TEST_CONTAINER:/app/pom.xml"
                        docker cp src "$TEST_CONTAINER:/app/src"

                        docker start -a "$TEST_CONTAINER" || true

                        TEST_EXIT_CODE=$(docker inspect "$TEST_CONTAINER" --format='{{.State.ExitCode}}')
                        docker cp "$TEST_CONTAINER:/app/target/surefire-reports/." target/surefire-reports/ 2>/dev/null || true

                        if [ "$TEST_EXIT_CODE" != "0" ]; then
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

        stage('Package Backend') {
            steps {
                dir("${BACKEND_DIR}") {
                    sh '''
                        set -eu

                        PACKAGE_CONTAINER="benh-so-an-package-${BUILD_NUMBER}"

                        cleanup() {
                          docker rm -f "$PACKAGE_CONTAINER" >/dev/null 2>&1 || true
                        }

                        trap cleanup EXIT
                        cleanup

                        rm -rf target
                        mkdir -p target

                        docker volume create ${MAVEN_CACHE_VOLUME} >/dev/null

                        docker create \
                          --name "$PACKAGE_CONTAINER" \
                          -w /app \
                          -v ${MAVEN_CACHE_VOLUME}:/root/.m2 \
                          ${MAVEN_IMAGE} \
                          sh -c 'mvn -B -ntp clean package -DskipTests'

                        docker cp pom.xml "$PACKAGE_CONTAINER:/app/pom.xml"
                        docker cp src "$PACKAGE_CONTAINER:/app/src"

                        docker start -a "$PACKAGE_CONTAINER"
                        docker cp "$PACKAGE_CONTAINER:/app/target/." target/
                    '''
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
                }
            }
        }

        stage('Build Runtime Image') {
            steps {
                dir("${BACKEND_DIR}") {
                    sh '''
                        docker build \
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
