pipeline {
    
    agent any
    
    tools {
        maven 'Maven 3.5.2'
    }

    stages {
        stage ('Initialize') {
            steps {  
                script {
                    def causes = currentBuild.getBuildCauses('com.cloudbees.jenkins.GitHubPushCause').shortDescription
                    if(!causes) {
                        causes = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause').shortDescription
                    }
                    currentBuild.displayName = "#${BUILD_NUMBER} ${GIT_BRANCH} ${causes}"
                    //params.gitBranch = "${GIT_BRANCH}"
                }
      			sh '''
               		echo "PATH = ${PATH}"
               		echo "M2_HOME = ${M2_HOME}"
               	'''
            }
        }

        stage ('Test') {
            steps {
                sh 'mvn clean test' 
            }
        }

        stage ('Package') {
        	steps {
        		sh 'mvn clean package'
        	}
        }

        stage ('Verify') {
        	steps {
                wrap([$class: 'Xvfb', additionalOptions: '', assignedLabels: '', autoDisplayName: true, debug: true, displayNameOffset: 0, installationName: 'default', parallelBuild: true, screen: '']) {
                    sh 'mvn clean verify'
                }
        	}
        }
    }
    post {
        always {
            script {
                def commits = ""
                if(currentBuild) {
                    commits += "${currentBuild.changeSets.getAt(0).getItems()[0].getMsg()} \t From:${currentBuild.changeSets.getAt(0).getItems()[0].getAuthor()}"
                }
                emailext body: "Result can be found at:'${currentBuild.absoluteUrl}' \n \nAffected commits: ${commits}", subject: "Unsuccessful Job '${params.gitBranch}'", to: 'c.orsinger@tu-braunschweig.de'
            }
        }
        //unsuccessful {
        // One or more steps need to be included within each condition's block.
        //}
    } 
}
