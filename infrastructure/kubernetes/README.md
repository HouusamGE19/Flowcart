# Kubernetes deployment templates

These manifests are intentionally **production-oriented templates**, not a replacement for the local Docker Compose stack.

They assume PostgreSQL and Kafka are managed/external services. Before applying:

1. Replace `YOUR_GITHUB_USERNAME` image names.
2. Replace `postgres.example.internal` and the Kafka bootstrap address.
3. Copy `secret.example.yaml` to a secret managed by your preferred secret solution; do not commit real credentials.
4. Publish the service images to a registry.

```bash
kubectl apply -f namespace.yaml
kubectl apply -f configmap.yaml
kubectl apply -f secret.example.yaml   # development only; use a real secret manager in production
kubectl apply -f apps.yaml
kubectl apply -f hpa.yaml
```

The deployments include resource requests/limits and Spring Boot Actuator liveness/readiness probes. The order service also has an HPA example.
