.PHONY: up down logs build test clean demo-success demo-failure

up:
	docker compose up --build -d

down:
	docker compose down

logs:
	docker compose logs -f --tail=150

build:
	mvn -B -ntp clean package -DskipTests

test:
	mvn -B -ntp test

clean:
	mvn clean
	docker compose down -v --remove-orphans

demo-success:
	curl -sS -X POST http://localhost:8082/api/orders -H 'Content-Type: application/json' -d '{"customerEmail":"demo@flowcart.dev","items":[{"sku":"HEADSET-NC-01","quantity":1}],"simulatePaymentFailure":false}'

demo-failure:
	curl -sS -X POST http://localhost:8082/api/orders -H 'Content-Type: application/json' -d '{"customerEmail":"demo@flowcart.dev","items":[{"sku":"KEYBOARD-MECH-01","quantity":1}],"simulatePaymentFailure":true}'
