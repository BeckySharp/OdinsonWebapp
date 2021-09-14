docker-build:
	sbt "project webapp" "dist"
	docker-compose build

docker-run: docker-build
	docker-compose up -d

docker-push: docker-build
	docker-compose push

docker-stop:
	docker-compose down