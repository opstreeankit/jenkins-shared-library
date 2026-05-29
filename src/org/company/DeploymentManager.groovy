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

// Standard Deployment Method
def deploy() {

    steps.echo "Deploying attendance microservice to ${environment}"

    // Environment-specific ports
    def portMap = [
        dev     : "8081",
        staging : "8082",
        prod    : "8083"
    ]

    def appPort = portMap[environment]

    steps.sh """

        echo "Creating backup image for rollback..."

        docker tag attendance:${environment} attendance:previous || true

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

// Rolling Deployment Method
def rollingDeploy() {

    steps.echo "Starting rolling deployment for ${environment}"

    def portMap = [
        dev     : "8081",
        staging : "8082",
        prod    : "8083"
    ]

    def appPort = portMap[environment]

    steps.sh """

        echo "Creating backup image for rollback..."

        docker tag attendance:${environment} attendance:previous || true

        echo "Building new Docker image..."

        docker build -t attendance:${environment}-new ./attendance

        echo "Starting temporary container..."

        docker run -d \\
          --name attendance-${environment}-new \\
          -p 9090:8081 \\
          attendance:${environment}-new

        echo "Waiting for container startup..."

        sleep 15

        echo "Performing health check..."

        curl -f http://localhost:9090/attendance || exit 1

        echo "Health check successful"

        echo "Stopping old container..."

        docker stop attendance-${environment} || true

        docker rm attendance-${environment} || true

        echo "Starting new production container..."

        docker run -d \\
          --name attendance-${environment} \\
          -p ${appPort}:8081 \\
          attendance:${environment}-new

        echo "Removing temporary container..."

        docker stop attendance-${environment}-new || true

        docker rm attendance-${environment}-new || true

        echo "Running Containers:"
        docker ps
    """

    steps.echo "Rolling deployment completed successfully"
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

        echo "Running Containers:"
        docker ps
    """

    steps.echo "Rollback completed"
}

}
