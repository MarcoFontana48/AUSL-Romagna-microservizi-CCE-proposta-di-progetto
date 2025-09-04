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

a request to 'anamnesi-pregressa' to get an allergy with id '123':

```bash
curl.exe -X GET http://localhost:8080/anamnesi-pregressa/AllergyIntolerance/123
```

a request to 'anamnesi-pregressa' to add an allergy with id '123' to a patient with id '456':
```bash
Invoke-RestMethod -Uri "http://localhost:8080/anamnesi-pregressa/AllergyIntolerance" -Method POST -ContentType "application/json" -Body '{
  "resourceType": "AllergyIntolerance",
  "id": "123",
  "patient": {
    "reference": "Patient/456"
  },
  "code": {
    "coding": [
      {
        "system": "http://snomed.info/sct",
        "code": "227493005",
        "display": "Cashew nuts"
      }
    ],
    "text": "Cashew nuts"
  },
  "clinicalStatus": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical",
        "code": "active",
        "display": "Active"
      }
    ]
  }
}'
```

a request to 'diario-clinico' to get an encounter with id '123':

```bash
curl.exe -X GET http://localhost:8080/diario-clinico/Encounter/123
```

a request to 'diario-clinico' to add an encounter with id '123' to a patient with id '456':
```bash
Invoke-RestMethod -Uri "http://localhost:8080/diario-clinico/Encounter" -Method POST -ContentType "application/json" -Body '{
  "resourceType": "Encounter",
  "id": "123",
  "subject": {
    "reference": "Patient/456"
  },
  "class": {
    "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode",
    "code": "AMB",
    "display": "Ambulatory"
  },
  "status": "finished",
  "serviceType": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/service-type",
        "code": "408",
        "display": "General Medicine"
      }
    ],
    "text": "General Medicine"
  },
  "period": {
    "start": "2025-09-04T12:00:00Z",
    "end": "2025-09-04T12:00:00Z"
  }
}'
```

## Visualize metrics
Go to "http://localhost:9090/" or "http://localhost:31090/" if using kubernetes, to visualize metrics collected by prometheus. You can use the following queries to visualize some metrics:

<b>All of those metrics are updated every a fixed amount of time and their evolution over time can be recorded and visualized by clicking on the 'graph' section in the prometheus GUI. Each time the result of those queries changes an update in the graph can be seen</b>

To see all the next queries go to (it is sufficient to refresh the page to query all the metrics below at once):

IF KUBERNETES:
```text
http://localhost:31090/query?g0.expr=rate%28health_check_duration_seconds_sum%5B1h%5D%29+%2F+rate%28health_check_duration_seconds_count%5B1h%5D%29&g0.show_tree=0&g0.tab=graph&g0.range_input=5m&g0.res_type=auto&g0.res_density=high&g0.display_mode=lines&g0.show_exemplars=0&g1.expr=health_check_success_total+%2F+health_check_requests_total&g1.show_tree=0&g1.tab=graph&g1.range_input=1h&g1.res_type=auto&g1.res_density=medium&g1.display_mode=lines&g1.show_exemplars=0&g2.expr=health_check_success_total&g2.show_tree=0&g2.tab=graph&g2.range_input=1h&g2.res_type=auto&g2.res_density=medium&g2.display_mode=lines&g2.show_exemplars=0&g3.expr=health_check_requests_total&g3.show_tree=0&g3.tab=graph&g3.range_input=1h&g3.res_type=auto&g3.res_density=medium&g3.display_mode=lines&g3.show_exemplars=0&g4.expr=get_dummy_requests_total&g4.show_tree=0&g4.tab=graph&g4.range_input=1h&g4.res_type=auto&g4.res_density=medium&g4.display_mode=lines&g4.show_exemplars=0&g5.expr=create_dummy_requests_total&g5.show_tree=0&g5.tab=graph&g5.range_input=1h&g5.res_type=auto&g5.res_density=medium&g5.display_mode=lines&g5.show_exemplars=0&g6.expr=create_dummy_requests_total+%2B+get_dummy_requests_total&g6.show_tree=0&g6.tab=graph&g6.range_input=1h&g6.res_type=auto&g6.res_density=medium&g6.display_mode=lines&g6.show_exemplars=0&g7.expr=get_dummy_requests_total+%2F+%28create_dummy_requests_total+%2B+get_dummy_requests_total%29&g7.show_tree=0&g7.tab=graph&g7.range_input=1h&g7.res_type=auto&g7.res_density=medium&g7.display_mode=lines&g7.show_exemplars=0&g8.expr=get_dummy_failure_total&g8.show_tree=0&g8.tab=graph&g8.range_input=1h&g8.res_type=auto&g8.res_density=medium&g8.display_mode=lines&g8.show_exemplars=0&g9.expr=get_dummy_success_total&g9.show_tree=0&g9.tab=graph&g9.range_input=1h&g9.res_type=auto&g9.res_density=medium&g9.display_mode=lines&g9.show_exemplars=0&g10.expr=get_dummy_success_total+%2F+%28get_dummy_success_total+%2B+get_dummy_failure_total%29&g10.show_tree=0&g10.tab=graph&g10.range_input=1h&g10.res_type=auto&g10.res_density=medium&g10.display_mode=lines&g10.show_exemplars=0&g11.expr=++metrics_success_read_requests_total&g11.show_tree=0&g11.tab=graph&g11.range_input=1h&g11.res_type=auto&g11.res_density=medium&g11.display_mode=lines&g11.show_exemplars=0&g12.expr=metrics_success_total+%2F+metrics_requests_total&g12.show_tree=0&g12.tab=graph&g12.range_input=1h&g12.res_type=auto&g12.res_density=medium&g12.display_mode=lines&g12.show_exemplars=0&g13.expr=metrics_success_read_requests_total+%2F+metrics_read_requests_total&g13.show_tree=0&g13.tab=graph&g13.range_input=1h&g13.res_type=auto&g13.res_density=medium&g13.display_mode=lines&g13.show_exemplars=0&g14.expr=metrics_read_requests_total+%2F+metrics_requests_total&g14.show_tree=0&g14.tab=graph&g14.range_input=1h&g14.res_type=auto&g14.res_density=medium&g14.display_mode=lines&g14.show_exemplars=0&g15.expr=metrics_read_requests_total&g15.show_tree=0&g15.tab=table&g15.range_input=1h&g15.res_type=auto&g15.res_density=medium&g15.display_mode=lines&g15.show_exemplars=0&g16.expr=metrics_requests_total&g16.show_tree=0&g16.tab=table&g16.range_input=1h&g16.res_type=auto&g16.res_density=medium&g16.display_mode=lines&g16.show_exemplars=0
```

IF DOCKER:
```text
http://localhost:9090/query?g0.expr=rate%28health_check_duration_seconds_sum%5B1h%5D%29+%2F+rate%28health_check_duration_seconds_count%5B1h%5D%29&g0.show_tree=0&g0.tab=graph&g0.range_input=5m&g0.res_type=auto&g0.res_density=high&g0.display_mode=lines&g0.show_exemplars=0&g1.expr=health_check_success_total+%2F+health_check_requests_total&g1.show_tree=0&g1.tab=graph&g1.range_input=1h&g1.res_type=auto&g1.res_density=medium&g1.display_mode=lines&g1.show_exemplars=0&g2.expr=health_check_success_total&g2.show_tree=0&g2.tab=graph&g2.range_input=1h&g2.res_type=auto&g2.res_density=medium&g2.display_mode=lines&g2.show_exemplars=0&g3.expr=health_check_requests_total&g3.show_tree=0&g3.tab=graph&g3.range_input=1h&g3.res_type=auto&g3.res_density=medium&g3.display_mode=lines&g3.show_exemplars=0&g4.expr=get_dummy_requests_total&g4.show_tree=0&g4.tab=graph&g4.range_input=1h&g4.res_type=auto&g4.res_density=medium&g4.display_mode=lines&g4.show_exemplars=0&g5.expr=create_dummy_requests_total&g5.show_tree=0&g5.tab=graph&g5.range_input=1h&g5.res_type=auto&g5.res_density=medium&g5.display_mode=lines&g5.show_exemplars=0&g6.expr=create_dummy_requests_total+%2B+get_dummy_requests_total&g6.show_tree=0&g6.tab=graph&g6.range_input=1h&g6.res_type=auto&g6.res_density=medium&g6.display_mode=lines&g6.show_exemplars=0&g7.expr=get_dummy_requests_total+%2F+%28create_dummy_requests_total+%2B+get_dummy_requests_total%29&g7.show_tree=0&g7.tab=graph&g7.range_input=1h&g7.res_type=auto&g7.res_density=medium&g7.display_mode=lines&g7.show_exemplars=0&g8.expr=get_dummy_failure_total&g8.show_tree=0&g8.tab=graph&g8.range_input=1h&g8.res_type=auto&g8.res_density=medium&g8.display_mode=lines&g8.show_exemplars=0&g9.expr=get_dummy_success_total&g9.show_tree=0&g9.tab=graph&g9.range_input=1h&g9.res_type=auto&g9.res_density=medium&g9.display_mode=lines&g9.show_exemplars=0&g10.expr=get_dummy_success_total+%2F+%28get_dummy_success_total+%2B+get_dummy_failure_total%29&g10.show_tree=0&g10.tab=graph&g10.range_input=1h&g10.res_type=auto&g10.res_density=medium&g10.display_mode=lines&g10.show_exemplars=0&g11.expr=++metrics_success_read_requests_total&g11.show_tree=0&g11.tab=graph&g11.range_input=1h&g11.res_type=auto&g11.res_density=medium&g11.display_mode=lines&g11.show_exemplars=0&g12.expr=metrics_success_total+%2F+metrics_requests_total&g12.show_tree=0&g12.tab=graph&g12.range_input=1h&g12.res_type=auto&g12.res_density=medium&g12.display_mode=lines&g12.show_exemplars=0&g13.expr=metrics_success_read_requests_total+%2F+metrics_read_requests_total&g13.show_tree=0&g13.tab=graph&g13.range_input=1h&g13.res_type=auto&g13.res_density=medium&g13.display_mode=lines&g13.show_exemplars=0&g14.expr=metrics_read_requests_total+%2F+metrics_requests_total&g14.show_tree=0&g14.tab=graph&g14.range_input=1h&g14.res_type=auto&g14.res_density=medium&g14.display_mode=lines&g14.show_exemplars=0&g15.expr=metrics_read_requests_total&g15.show_tree=0&g15.tab=table&g15.range_input=1h&g15.res_type=auto&g15.res_density=medium&g15.display_mode=lines&g15.show_exemplars=0&g16.expr=metrics_requests_total&g16.show_tree=0&g16.tab=table&g16.range_input=1h&g16.res_type=auto&g16.res_density=medium&g16.display_mode=lines&g16.show_exemplars=0
```

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

See the amount of WRITE requests for a specific entity (creation only):
```text
create_dummy_requests_total
```

See the total amount of requests for a specific entity:
```text
create_dummy_requests_total + get_dummy_requests_total
```

See the amount of READs over WRITEs (considering only creation for this example) (useful to see if a CQRS pattern can be applied and where to apply it!)
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
