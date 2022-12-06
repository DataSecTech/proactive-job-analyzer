# SecureDL - Proactive Job Analyzer

SecureDL Proactive Job Analyzer helps you to analyzer data science scala code.

## Build docker containers

Build the gradle project with

```shell
./gradlew build
```

Next build docker containers (1. webservice, 2. cli)

```shell
ja_version="0.1.0-SNAPSHOT" # Read it from the build.gradle to be consistent

docker build . -f "Dockerfile" \
    -t "secure-dl/job-analyzer-webservice:$ja_version" \
    -t "secure-dl/job-analyzer-webservice:latest" \
    --build-arg "SDL_JOB_ANALYZER_VERSION=$ja_version"

docker build . -f "cli.Dockerfile" \
    -t "secure-dl/job-analyzer-cli:$ja_version" \
    -t "secure-dl/job-analyzer-cli:latest" \
    --build-arg "SDL_JOB_ANALYZER_VERSION=$ja_version"
```

We provided a convenient command to build docker containers `./dev.sh build --docker`

## Run

### CLI

We use `scalac` to compile scala snippet to byte code, which required spark related dependencies.
We build `secure-dl/job-analyzer-cli` has the necessary binary and jar files in it. Run it with

```shell
docker run -v $PWD/data:/data secure-dl/job-analyzer-cli snippet \
    -c '/sdl/analysis-runtime/*' \
    -s '/data/code-attack.scala'
```

### Run the REST API webservice

The webservice version stores the analysis results into a mysql.
We provide convenient `docker-compose.yml` for building a two container deployment.

This requires few configurations in `.env` file.
Either you can run `./dev.sh init` to generate one or manually create `.env` file with following variables.

```
MYSQL_ROOT_PASSWORD=
MYSQL_PROACTIVE_JOB_ANALYZER_PASSWORD=
VOLUME_BASE=
SDL_JOB_ANALYZER_VERSION=
SDL_JOB_ANALYZER_PORT=
```

Now start the containers

```shell
docker compose up
```

### Endpoints

Run program analysis

```shell
http --form POST :8080/program-analyzer/analyze codeSnippet=@sample/code-attack.scala
```

See list of previous analysis results

```shell
http :8080/program-analysis
```

See a specific analysis result

```shell
http :8080/program-analysis/1
```
