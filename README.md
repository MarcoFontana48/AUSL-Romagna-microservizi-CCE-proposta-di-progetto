[![ci-cd](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/actions/workflows/ci-cd.yml/badge.svg?branch=master)](https://github.com/MarcoFontana48/AUSL-Romagna-CCE-Microservices-Project-Proposal/actions/workflows/ci-cd.yml)
[![semantic-release: angular](https://img.shields.io/badge/semantic--release-angular-e10079?logo=semantic-release)](https://github.com/semantic-release/semantic-release)
[![Renovate](https://img.shields.io/badge/renovate-enabled-brightgreen.svg)](https://renovatebot.com)

# How to run the project
how to build and run the entire project (it also builds all images before running):

- make sure to have docker installed and using dockerhub with kubernetes enabled
- make sure to have gradle installed
- move to the project root directory (the directory where this README.md is located)
- run the following commands to stop any running containers and remove existing images and volumes, then build and run the project:

## Kubernetes
If you want to deploy the project on kubernetes, run the following commands (then wait some minutes to make sure everything is up and running correctly before running other commands):

```bash
kubectl delete -f kubernetes/
kubectl apply -f kubernetes/
```

to only delete containers

```bash
kubectl delete -f kubernetes/
```

to only apply the manifest and run the project:

```bash
kubectl apply -f kubernetes/
```

to test if the application is running, you can use the following command (windows), if linux remove the `.exe`. It sends an health check request to the API Gateway of the application:

```bash
curl.exe -X GET http://localhost:31080/health
```

or

```bash
curl http://localhost:31080/health
```

to check if the application reaches the service from api-gateway:

```bash
curl.exe http://localhost:31080/service/health
```

to check all endpoints that can be reached from the API Gateway, you can use the following command:

```bash
curl.exe http://localhost:31080/route
```

to get metrics from API gateway

```bash
curl.exe http://localhost:31080/metrics
```

to get metrics from the service

```bash
curl.exe http://localhost:31080/service/metrics
```

it is also possible to query prometheus via its browser GUI, connecting to "http://localhost:31090/"

[//]: # (To test the autoscaler, first install the metrics server:)

[//]: # ()
[//]: # (```bash)

[//]: # ()
[//]: # (kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml)

[//]: # ()
[//]: # (```)

[//]: # ()
[//]: # (then make sure that the metrics server is running:)

[//]: # ()
[//]: # (```bash)

[//]: # (kubectl top nodes)

[//]: # (kubectl top pods -n monitoring-app)

[//]: # (```)

To test the autoscaler, first check if the metrics server is running:

```bash
kubectl top nodes
kubectl top pods -n monitoring-app
```

then run this command on another terminal to watch the horizontal pod autoscaler for the API Gateway:

```bash
kubectl get hpa -n monitoring-app --watch
```

[//]: # (then go back to previous terminal and run this command that uses 'hey' to runs 50 workers that concurrently send requests for 2 minutes to the health endpoint of the API Gateway and then prints metrics about the requests:)

[//]: # ()
[//]: # (```bash)

[//]: # (hey -c 50 -z 2m http://localhost:31080/health)

[//]: # (```)

[//]: # ()
[//]: # (you should be seeing the number of pods for the API Gateway increasing and decreasing based on the requests sent by 'hey' in the terminal where you ran the watch command.)

[//]: # ()
[//]: # (You can also run the same command for the service:)

[//]: # ()
[//]: # (```bash)

[//]: # (hey -c 50 -z 2m http://localhost:31080/service/health)

[//]: # (```)

To send a post request for dummy entities
```bash
Invoke-RestMethod -Uri "http://localhost:31080/service/dummies" -Method POST -ContentType "application/json" -Body '{"id":{"value":"123"},"field":"dummy_field_value"}'
```

To send a get request about the dummy entity just created
```bash
curl.exe -X GET http://localhost:31080/service/dummies/123
```

## Docker
If you want to use docker, run those commands instead:

```bash
./gradlew assemble
docker-compose down --rmi all -v
docker-compose up --build -d
```

or not to remove images:

```bash
./gradlew assemble
docker-compose down -v
docker-compose up --build -d
```

to build and run without removing images:

```bash
docker-compose down -v
docker-compose up --build -d
```

to only build the project and run it in detached mode:

```bash
docker-compose up --build -d
```

to only stop the project and remove all containers, networks, images and volumes created by `docker-compose up`:

```bash
docker-compose down --rmi all -v
```

to only stop the project and remove only volumes and containers:

```bash
docker-compose down -v
```

to test if the application is running, you can use the following command (windows), if linux remove the `.exe`. It sends an health check request to the API Gateway of the application:

```bash
curl.exe -X GET http://localhost:8080/health
```

or

```bash
curl http://localhost:8080/health
```

to check if the application reaches the service from api-gateway:

```bash
curl.exe http://localhost:8080/service/health
```

to check all endpoints that can be reached from the API Gateway, you can use the following command:

```bash
curl.exe http://localhost:8080/route
```

to get metrics from API gateway

```bash
curl.exe http://localhost:8080/metrics
```

to get metrics from the service

```bash
curl.exe http://localhost:8080/service/metrics
```

it is also possible to query prometheus via its browser GUI, connecting to "http://localhost:9090/"

to send a post request for dummy entities

```bash
Invoke-RestMethod -Uri "http://localhost:8080/service/dummies" -Method POST -ContentType "application/json" -Body '{"id":{"value":"123"},"field":"dummy_field_value"}'
```

to send a get request about the dummy entity just created

```bash
curl.exe -X GET http://localhost:8080/service/dummies/123
```

## Visualize metrics
Go to "http://localhost:9090/" or "http://localhost:31090/" if using kubernetes, to visualize metrics collected by prometheus. You can use the following queries to visualize some metrics:

<b>All of those metrics are updated every a fixed amount of time and their evolution over time can be recorded and visualized by clicking on the 'graph' section in the prometheus GUI. Each time the result of those queries changes an update in the graph can be seen</b>

See the 95th percentile of the request duration for the health check endpoint of the service over the last 5 minutes:
```text
histogram_quantile(0.95, sum(rate(health_check_duration_seconds_bucket{service="service"}[5m])) by (le))
```

See the average latency of requests to the health check endpoint of the service over the last 5 minutes, divided by service type (click on graph to visualize it):
```text
rate(health_check_duration_seconds_sum[5m]) / rate(health_check_duration_seconds_count[5m])
```

See the total number of requests to the health check endpoint of the service over the last 5 minutes, divided by service type:
```text
health_check_duration_seconds_count
```

See the amount of READ requests for a specific entity:
```text
get_dummy_requests_total
```

See the amount of WRITE requests for a specific entity:
```text
create_dummy_requests_total
```

See the total amount of requests for a specific entity:
```text
create_dummy_requests_total + get_dummy_requests_total
```

See the amount of READs over WRITEs (useful to see if a CQRS pattern can be applied and where to apply it!)
```text
get_dummy_requests_total / (create_dummy_requests_total + get_dummy_requests_total)
```

See the amount of failed requests for a specific entity:
```text
get_dummy_failure_total
```

See the amount of successful requests for a specific entity:
```text
get_dummy_success_total
```

See the amount of successful requests over total amount of requests for a specific entity (useful to see the reliability of the service):
```text
get_dummy_success_total / (get_dummy_success_total + get_dummy_failure_total)
```

It's possible to query any metric exposed by the application, this is just an example considering simply health checks, anything that is exposed can be queried.
