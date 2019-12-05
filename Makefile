SHELL:=/usr/bin/env bash

.PHONY: default
default: | help

.PHONY: start
start: ## Run the application inside a docker container
	docker-compose up -d --build

.PHONY: build
build: ## Build the application with maven
	./mvnw -B clean install dependency:copy-dependencies -DincludeScope=runtime

.PHONY: stop
stop: ## Stop the application
	docker-compose down

.PHONY: restart
restart: | stop start ## Restart the application

.PHONY: start-db
start-db: ## Start the database
	docker-compose up -d --build postgres

.PHONY: stop-db
stop-db: ## Stop the database and empty its data
	docker-compose rm -fsv postgres

.PHONY: restart-db
restart-db: | stop-db start-db ## Restart the database

.PHONY: help
help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-45s\033[0m %s\n", $$1, $$2}'
