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

    steps.sh """
        docker build -t attendance:${environment} ./attendance

        docker stop attendance-${environment} || true
        docker rm attendance-${environment} || true

        docker run -d \\
          --name attendance-${environment} \\
          -p 8081:8081 \\
          attendance:${environment}
    """

    steps.echo "Deployment completed for ${environment}"
}

// Rolling Deployment Method
def rollingDeploy() {

    steps.echo "Starting rolling deployment for ${environment}"

    steps.sh """
        echo "Cleaning old temporary containers..."
        docker stop attendance-${environment}-new || true
        docker rm attendance-${environment}-new || true

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
        curl -f http://localhost:9090/attendance/healthz

        echo "Stopping old container..."
        docker stop attendance-${environment} || true
        docker rm attendance-${environment} || true

        echo "Starting new production container..."
        docker run -d \\
          --name attendance-${environment} \\
          -p 8081:8081 \\
          attendance:${environment}-new

        echo "Cleaning temporary container..."
        docker stop attendance-${environment}-new || true
        docker rm attendance-${environment}-new || true

        echo "Tagging new image as stable..."
        docker tag attendance:${environment}-new attendance:${environment}
    """

    steps.echo "Rolling deployment completed successfully"
}

// Rollback Method
def rollback() {

    steps.echo "Rollback started for ${environment}"

    steps.sh """
        docker stop attendance-${environment} || true
        docker rm attendance-${environment} || true

        docker run -d \\
          --name attendance-${environment} \\
          -p 8081:8081 \\
          attendance:previous
    """

    steps.echo "Rollback completed"
}

}
