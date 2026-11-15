pipeline {
    agent any

    tools {
        jdk 'JDK17'
        nodejs 'NodeJS'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Validate PostgreSQL Env') {
            steps {
                script {
                    // Inspect the inherited environment before Maven runs. Only variable
                    // names are reported; values are never printed, cleared or masked.
                    def pgCloneUrl = env.LIBRARY_PG_CLONE_URL
                    def pgItUrl = env.LIBRARY_PG_IT_URL

                    if (pgCloneUrl != null && !pgCloneUrl.trim().isEmpty()) {
                        error 'LIBRARY_PG_CLONE_URL is set in the job or agent environment. This would activate LegacyCloneMigrationTests against a real PostgreSQL server. Unset it for standard CI.'
                    }
                    if (pgItUrl != null && !pgItUrl.trim().isEmpty()) {
                        error 'LIBRARY_PG_IT_URL is set in the job or agent environment. This would activate PostgresBorrowingIntegrationTests against a real PostgreSQL server. Unset it for standard CI.'
                    }
                }
            }
        }

        stage('Backend Tests') {
            steps {
                dir('library-backend') {
                    bat 'call mvnw.cmd clean test'
                }
            }
            post {
                always {
                    // JUnit reports published regardless of test pass/fail status.
                    // allowEmptyResults: true ensures the step does not fail the pipeline
                    // when no TEST-*.xml files are generated (e.g. if all tests are ignored/skipped).
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/TEST-*.xml'
                }
            }
        }

        stage('Frontend Dependencies') {
            steps {
                dir('library-frontend') {
                    bat 'call npm ci'
                }
            }
        }

        stage('Frontend Lint') {
            steps {
                dir('library-frontend') {
                    bat 'call npm run lint'
                }
            }
        }

        stage('Frontend Build') {
            steps {
                dir('library-frontend') {
                    bat 'call npm run build'
                }
            }
        }
    }

    post {
        success {
            echo '=== Jenkins Pipeline Completed Successfully ==='

            echo ''
            echo 'Summary:'
            echo '- Backend: Maven tests executed via mvnw.cmd (H2 test database)'
            echo '  - PostgreSQL integration tests are skipped; inherited LIBRARY_PG_* variables are rejected by the Validate stage'
            echo '- Frontend: Dependencies installed (npm ci), lint passed, build completed'
            echo '- Reports: Backend JUnit reports published above (allowEmptyResults: true)'
            echo ''
            echo 'Next steps:'
            echo '- Review test results in Jenkins dashboard'
            echo '- Frontend build output in library-frontend/dist/'
        }
        failure {
            echo '=== Jenkins Pipeline FAILED ==='

            echo ''
            echo 'Investigate:'
            echo '- Backend test failures in library-backend/target/surefire-reports/'
            echo '- Frontend lint errors in library-frontend/'
            echo '- Verify JDK17 and NodeJS tool configurations in Jenkins'
            echo '- Check that LIBRARY_PG_* env vars are not set in this job'
        }
    }
}