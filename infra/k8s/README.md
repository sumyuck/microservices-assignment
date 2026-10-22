# Kubernetes demo instructions

Use a local cluster on the presentation laptop, for example Minikube.

```bash
minikube start --cpus=4 --memory=7000
eval $(minikube docker-env)
docker compose build
kubectl apply -f all.yaml
kubectl -n retailflow get pods
minikube -n retailflow service api-gateway --url
```

`all.yaml` is intentionally plain Kubernetes YAML: no Helm, no cloud account, and no CI/CD. The databases use `emptyDir` so this is a disposable demonstration. The Compose setup has named volumes and is better for repeated local use.

For a presentation, show `kubectl get pods -n retailflow`, the Consul catalogue, RabbitMQ queues, and the gateway workflow. Do **not** claim the demo credentials or `emptyDir` storage are production ready.
