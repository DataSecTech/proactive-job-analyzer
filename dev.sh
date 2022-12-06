#!/usr/bin/env bash

set -euo pipefail

SCRIPT_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

function usage {
    CMD="dev.sh"

    cat <<EOT
Helper script for Job Analyzer

Usage:
    ${CMD} help                 Print this message and quit
    ${CMD} init                 Generate the .env file with basic configurations
    ${CMD} build [--docker]     Build the project optionally the docker images as well
EOT
}

function cmd-version() {
    JA_VERSION=$(grep -Po 'sdlJobAnalyzerVersion\s*=\s*['\''"]\K[^'\''"]+' build.gradle)
    echo "Job analyzer version: $JA_VERSION"

    if [[ -z $JA_VERSION ]]; then
        echo "** Couldn't find the version of job analyzer"
        exit 1
    fi
}

function generate_password() {
    set +o pipefail
    r=$(LC_ALL=C tr -cd '[:alnum:]' </dev/urandom | fold -w24 | head -n 1)
    set -o pipefail
    password="${r:0:6}-${r:6:6}-${r:12:6}-${r:18:6}"
    echo "$password"
}

function cmd-init() {

    if [[ -f .env ]]; then
        echo ".env file exists; not overwriting"
        return 1
    fi

    root_password=$(generate_password)
    db_password=$(generate_password)
    volume_base="$SCRIPT_PATH/data/volume"
    api_port=8080

    cmd-version

    mkdir -p "$volume_base"

    echo "Generating .env file; please adjust accordingly"
    cat <<EOT >"$SCRIPT_PATH/.env"
MYSQL_ROOT_PASSWORD=$root_password
MYSQL_PROACTIVE_JOB_ANALYZER_PASSWORD=$db_password
VOLUME_BASE=$volume_base
SDL_JOB_ANALYZER_VERSION=$JA_VERSION
SDL_JOB_ANALYZER_PORT=$api_port
EOT

}

function cmd-build() {
    cmd-version

    pushd webservice >/dev/null
    ./gradlew clean
    popd >/dev/null

    ./gradlew build

    if [[ "$*" == *"--docker"* ]]; then
        build_docker_image cli
        build_docker_image webservice
    fi
}

function build_docker_image() {

    if [[ $1 == 'cli' ]]; then
        docker_file='cli.Dockerfile'
        docker_image="secure-dl/job-analyzer-cli:$JA_VERSION"
        docker_image_latest="secure-dl/job-analyzer-cli:latest"

    elif [[ $1 == 'webservice' ]]; then

        docker_file='Dockerfile'
        docker_image="secure-dl/job-analyzer-webservice:$JA_VERSION"
        docker_image_latest="secure-dl/job-analyzer-webservice:latest"
    else
        echo "Invalid docker image name"
        exit 1
    fi

    existing_containers=$(docker ps -a -q -f "ancestor=${docker_image}")

    if [[ -n $existing_containers ]]; then
        # shellcheck disable=SC2086
        docker rm ${existing_containers}
    fi

    existing_image=$(docker images -q "$docker_image")

    if [[ -n $existing_image ]]; then
        echo "$existing_image"
        docker rmi "$docker_image"
    fi

    docker build . -f $docker_file -t "$docker_image" -t "$docker_image_latest" --build-arg "SDL_JOB_ANALYZER_VERSION=$JA_VERSION"
}

function cmd-run-docker() {
    cmd-version
    docker run "secure-dl/job-analyzer-webservice:$JA_VERSION"
}

if [[ -z "$*" || "$1" == '-h' || "$1" == '--help' || "$1" == 'help' ]]; then
    usage
    exit 0
fi

command="cmd-${1}"

if [[ $(type -t "${command}") != "function" ]]; then
    echo "Error: No command found"
    usage
    exit 1
fi

${command} "${@:2}"
