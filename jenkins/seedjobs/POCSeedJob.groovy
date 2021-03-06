def gitUrl="https://github.com/dEkio/microservicesLocalMonolith.git"

List<String> deployToContainerCommands = [ "sudo /usr/bin/docker stop eureka1 || echo 'no container to stop'", "sudo /usr/bin/docker rm eureka1 || echo 'no container to delete'", "cd eureka && sudo /usr/bin/docker build -t eureka .", "sudo /usr/bin/docker run -d --name eureka1 -p=18761:8761 eureka"]
List<String> deployToNexusCommands = [ "mvn -Drepository.address=dockerizedci_nexus_1:8081 -f ${WORKSPACE}/eureka/pom.xml clean deploy" ]
List<String> initial = [ "mvn  -f ${WORKSPACE}/eureka/pom.xml clean install"]

createInitial(gitUrl)
createCodeCoverage(gitUrl)
createIntegrationTest(gitUrl)
createSonar(gitUrl)
createDockerBuild(gitUrl)
createDeployDev(gitUrl)
createJMeter(gitUrl)
createNexus(gitUrl)

def createInitial(def gitRepository) {

  println "############################################################################################################"
  println "Creating Docker Job POC_Initial for gitRepository=${gitRepository}"
  println "############################################################################################################"

  job("POC_Initial") {
    logRotator {
        numToKeep(10)
    }
    if( "${gitRepository}".size() > 0 ) {
      scm {
        git {
          remote {
            url(gitRepository)
			credentials('endavaGit')
          }
          createTag(false)
          clean()
        }
      }
    }
    steps {
      steps {
		shell("mvn  -f eureka/pom.xml clean install")
      }
    }
    publishers {
      chucknorris()
	  downstreamParameterized {
        	trigger('POC_CodeCoverage'){
				parameters {
                    gitRevision(false)
                }
			}
			trigger('POC_SonarQube'){
				parameters {
                    gitRevision(false)
                }
			}
			trigger('POC_IntegrationTest'){
				parameters {
                    gitRevision(false)
                }
			}
			trigger('POC_DockerBuild'){
				parameters {
                    gitRevision(false)
                }
			}
		}
    }
  }
}

def createCodeCoverage(def gitRepository) {

  println "############################################################################################################"
  println "Creating Job POC_CodeCoverage for gitRepository=${gitRepository}"
  println "############################################################################################################"

  job("POC_CodeCoverage") {
    logRotator {
        numToKeep(10)
    }
    if( "${gitRepository}".size() > 0 ) {
      scm {
        git {
          remote {
            url(gitRepository)
          }
          createTag(false)
          clean()
        }
      }
    }
    steps {
      steps {
		shell("mvn -f eureka/pom.xml clean cobertura:cobertura cobertura:check ")
      }
    }
    publishers {
	  configure {
    		it / 'publishers' / 'htmlpublisher.HtmlPublisher' (plugin:"htmlpublisher@1.11") / 'reportTargets' / 'htmlpublisher.HtmlPublisherTarget'  {
          		reportName('CodeCoverage Report')
          		reportDir('HungryGame/target/site/cobertura/')
          		reportFiles('index.html')
          		keepAll('false')
          		allowMissing('false')
          		wrapperName('htmlpublisher-wrapper.html')    
			}
          	
    	}
      chucknorris()
    }
  }
}

def createIntegrationTest(def gitRepository) {

  println "############################################################################################################"
  println "Creating Docker Job POC_IntegrationTest for gitRepository=${gitRepository}"
  println "############################################################################################################"

  job("POC_IntegrationTest") {
    logRotator {
        numToKeep(10)
    }
    if( "${gitRepository}".size() > 0 ) {
      scm {
        git {
          remote {
            url(gitRepository)
          }
          createTag(false)
          clean()
        }
      }
    }
    steps {
      steps {
		shell("mvn -f eureka/pom.xml test")
      }
    }
    publishers {
      chucknorris()
    }
  }
}

def createSonar(def gitRepository) {

  println "############################################################################################################"
  println "Creating Docker Job POC_SonarQube for gitRepository=${gitRepository}"
  println "############################################################################################################"

  job("POC_SonarQube") {
    logRotator {
        numToKeep(10)
    }
    if( "${gitRepository}".size() > 0 ) {
      scm {
        git {
          remote {
            url(gitRepository)
			credentials('endavaGit')
		  }
        }
      }
    }
    steps {
      steps {
		shell("mvn -Drepository.address=dockerizedci_nexus_1:8081 -f eureka/pom.xml clean deploy")
      }
    }
	
	configure { 
      it /builders/'hudson.plugins.sonar.SonarRunnerBuilder' {
        	project()
         	javaOpts()
         	task()
         	jdk()
      	properties("sonar.projectKey=endava.HungryGame\nsonar.projectName=HungryGame\nsonar.projectVersion=$SNAPSHOT-1.0.0\nsonar.forceAnalysis=true\nsonar.sources=HungryGame/src/main/java/\nsonar.binaries=HungryGame/target/classes/\nsonar.tests=HungryGame/src/test/java/\nsonar.dynamicAnalysis=reuseReports\nsonar.java.coveragePlugin=jacoco\nsonar.surefire.reportsPath=HungryGame/target/surefire-reports/\nsonar.jacoco.reportPath=HungryGame/target/coverage-reports/jacoco-unit.exec sonar.language=java\nsonar.cobertura.reportPath=HungryGame/target/site/cobertura/coverage.xml")
     	}
    }
	
    publishers {
      chucknorris()
    }
  }
}

def createDockerBuild(def gitRepository) {

  println "############################################################################################################"
  println "Creating Docker Job POC_DockerBuild for gitRepository=${gitRepository}"
  println "############################################################################################################"

  job("POC_DockerBuild") {
    logRotator {
        numToKeep(10)
    }
    if( "${gitRepository}".size() > 0 ) {
      scm {
        git {
          remote {
            url(gitRepository)
			credentials('admin')
          }
          createTag(false)
          clean()
        }
      }
    }
    steps {
      steps {
		shell("sudo /usr/bin/docker stop eureka1 || echo 'no container to stop'")
		shell("sudo /usr/bin/docker rm eureka1 || echo 'no container to delete'")
		shell("cd eureka && sudo /usr/bin/docker build -t eureka .")
		shell("sudo /usr/bin/docker run -d --name eureka1 -p=18761:8761 eureka")
      }
    }
    publishers {
      chucknorris()
	  downstreamParameterized {
        	trigger('POC_DeployDev'){
				parameters {
                    gitRevision(false)
                }
			}
		}
    }
  }
}

def createDeployDev(def gitRepository) {

  println "############################################################################################################"
  println "Creating Docker Job POC_DeployDev for gitRepository=${gitRepository}"
  println "############################################################################################################"

  job("POC_DeployDev") {
    logRotator {
        numToKeep(10)
    }
    if( "${gitRepository}".size() > 0 ) {
      scm {
        git {
          remote {
            url(gitRepository)
			credentials('admin')
          }
          createTag(false)
          clean()
        }
      }
    }
    steps {
      steps {
		shell("sudo /usr/bin/docker stop eureka1 || echo 'no container to stop'")
		shell("sudo /usr/bin/docker rm eureka1 || echo 'no container to delete'")
		shell("cd eureka && sudo /usr/bin/docker build -t eureka .")
		shell("sudo /usr/bin/docker run -d --name eureka1 -p=18761:8761 eureka")
      }
    }
    publishers {
      chucknorris()
	  downstreamParameterized {
        	trigger('POC_JMeterTests'){
				parameters {
                    gitRevision(false)
                }
			}
		}
    }
  }
}

def createJMeter(def gitRepository) {

  println "############################################################################################################"
  println "Creating Docker Job POC_JMeterTests for gitRepository=${gitRepository}"
  println "############################################################################################################"

  job("POC_JMeterTests") {
    logRotator {
        numToKeep(10)
    }
    if( "${gitRepository}".size() > 0 ) {
      scm {
        git {
          remote {
            url(gitRepository)
          }
          createTag(false)
          clean()
        }
      }
    }
    steps {
      steps {
		shell("mvn -Drepository.address=dockerizedci_nexus_1:8081 -f eureka/pom.xml clean deploy")
      }
    }
    publishers {
      chucknorris()
	  downstreamParameterized {
        	trigger('POC_NexusRelease'){
				parameters {
                    gitRevision(false)
                }
			}
		}
    }
  }
}

def createNexus(def gitRepository) {

  println "############################################################################################################"
  println "Creating Docker Job POC_NexusRelease for gitRepository=${gitRepository}"
  println "############################################################################################################"

  job("POC_NexusRelease") {
    logRotator {
        numToKeep(10)
    }
    if( "${gitRepository}".size() > 0 ) {
      scm {
        git {
          remote {
            url(gitRepository)
          }
          createTag(false)
          clean()
        }
      }
    }
    steps {
      steps {
		shell("mvn -Drepository.address=dockerizedci_nexus_1:8081 -f eureka/pom.xml clean deploy")
      }
    }
    publishers {
      chucknorris()
	  downstreamParameterized {
        	trigger('eureka-deploy_jar_container'){
				parameters {
                    gitRevision(false)
                }
			}
		}
    }
  }
}

listView('POC') {
  description('')
  filterBuildQueue()
  filterExecutors()
  jobs {
    regex(/POC_.*/)
  }
  columns {
    buildButton()
    status()
    weather()
    name()
    lastSuccess()
    lastFailure()
    lastDuration()
  }
}