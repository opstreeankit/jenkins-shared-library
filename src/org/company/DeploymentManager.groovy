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

        steps.sh """
            docker build -t attendance:${environment} attendance/

            docker stop attendance-${environment} || true
            docker rm attendance-${environment} || true

            docker run -d \
              --name attendance-${environment} \
              -p 8080:8080 \
              attendance:${environment}
        """

        steps.echo "Deployment completed for ${environment}"
    }

    // Rollback Method
    def rollback() {

        steps.echo "Rollback started for ${environment}"

        steps.sh """
            docker stop attendance-${environment} || true
            docker rm attendance-${environment} || true

            docker run -d \
              --name attendance-${environment} \
              -p 8080:8080 \
              attendance:previous
        """

        steps.echo "Rollback completed"
    }
}
