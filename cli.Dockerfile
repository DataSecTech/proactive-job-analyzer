FROM maven:3.6.3-openjdk-8 as build-stage-analysis-dep

WORKDIR /sdl
ENV MAVEN_OPTS -Dhttp.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyHost= -Dhttps.proxyPort= -Dhttp.nonProxyHosts= -Dmaven.repo.local=/usr/share/maven/ref/repository

ADD dependency-project/pom.xml /sdl/pom.xml
RUN ["mvn", "dependency:copy-dependencies", "-DincludeScope=runtime", "-DskipTests=true", "-Dmdep.prependGroupId=true", "-DoutputDirectory=target", "--fail-never"]

FROM mcr.microsoft.com/java/jdk:8-zulu-debian10 as job-analyzer-cli

ARG SDL_JOB_ANALYZER_VERSION

RUN mkdir -p /usr/share/man/man1/ /sdl
WORKDIR /sdl
COPY --from=build-stage-analysis-dep /sdl/target/*.jar /sdl/analysis-runtime/

RUN apt-get update \
    && apt-get install -y scala

COPY "cli/build/libs/proactive-job-analyzer-cli-${SDL_JOB_ANALYZER_VERSION}.jar" "/sdl/proactive-job-analyzer-cli.jar"
ENTRYPOINT ["java", "-jar", "/sdl/proactive-job-analyzer-cli.jar"]
