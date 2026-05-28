package org.company

class DeploymentManager implements Serializable {

def steps
String environment

DeploymentManager(steps, String environment) {
    this.steps = steps
    this.environment = environment
}

// Validation Method
def validate() {

    steps.echo "Validating deployment for ${environment}"

    if (!(environment in ['dev', 'staging', 'prod'])) {
        steps.error("Invalid environment: ${environment}")
    }

    steps.echo "Validation successful"
}

// Deployment Method
def deploy() {

    steps.echo "Deploying attendance microservice to ${environment}"

    // Environment-specific port mapping
    def portMap = [
        dev     : "8081",
        staging : "8082",
        prod    : "8083"
    ]

    def appPort = portMap[environment]

    steps.sh """

        echo "Current Workspace:"
        pwd

        echo "Attendance Directory:"
        ls -la attendance

        echo "Building Docker image..."

        docker build -t attendance:${environment} ./attendance

        echo "Stopping old container if exists..."

        docker stop attendance-${environment} || true

        docker rm attendance-${environment} || true

        echo "Starting new container..."

        docker run -d \\
          --name attendance-${environment} \\
          -p ${appPort}:8081 \\
          attendance:${environment}

        echo "Running Containers:"
        docker ps
    """

    steps.echo "Deployment completed for ${environment}"
}

// Rollback Method
def rollback() {

    steps.echo "Rollback started for ${environment}"

    def portMap = [
        dev     : "8081",
        staging : "8082",
        prod    : "8083"
    ]

    def appPort = portMap[environment]

    steps.sh """

        echo "Stopping current container..."

        docker stop attendance-${environment} || true

        docker rm attendance-${environment} || true

        echo "Starting previous stable container..."

        docker run -d \\
          --name attendance-${environment} \\
          -p ${appPort}:8081 \\
          attendance:previous

        docker ps
    """

    steps.echo "Rollback completed"
}

}
