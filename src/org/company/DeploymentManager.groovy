package org.company

class DeploymentManager implements Serializable {

```
def steps
String environment

DeploymentManager(steps, String environment) {
    this.steps = steps
    this.environment = environment
}

// Validation Method
def validate() {

    steps.echo "Validating deployment for environment: ${environment}"

    if (!(environment in ['dev', 'staging', 'prod'])) {
        steps.error("Invalid environment: ${environment}")
    }

    steps.echo "Validation successful"
}

// Deployment Method
def deploy() {

    steps.echo "Starting deployment for ${environment}"

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

        echo "Building Docker Image..."
        docker build -t attendance:${environment} ./attendance

        echo "Docker Images:"
        docker images | grep attendance || true

        echo "Stopping Existing Container..."
        docker stop attendance-${environment} || true

        echo "Removing Existing Container..."
        docker rm attendance-${environment} || true

        echo "Running New Container..."

        docker run -d \\
          --name attendance-${environment} \\
          -p ${appPort}:8081 \\
          attendance:${environment}

        echo "Running Containers:"
        docker ps
    """

    steps.echo "Deployment completed successfully for ${environment}"
}

// Rollback Method
def rollback() {

    steps.echo "Starting rollback for ${environment}"

    def portMap = [
        dev     : "8081",
        staging : "8082",
        prod    : "8083"
    ]

    def appPort = portMap[environment]

    steps.sh """

        echo "Stopping Current Container..."
        docker stop attendance-${environment} || true

        echo "Removing Current Container..."
        docker rm attendance-${environment} || true

        echo "Starting Previous Stable Container..."

        docker run -d \\
          --name attendance-${environment} \\
          -p ${appPort}:8081 \\
          attendance:previous

        docker ps
    """

    steps.echo "Rollback completed successfully"
}
```

}
