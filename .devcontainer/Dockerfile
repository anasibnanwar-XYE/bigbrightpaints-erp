# BigBright ERP Dev Container - Java 21 Development Environment
FROM mcr.microsoft.com/devcontainers/java:21

# Install additional tools needed for development
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get install -y --no-install-recommends \
        postgresql-client \
        curl \
        jq \
        less \
        tree \
        vim \
    && apt-get clean -y \
    && rm -rf /var/lib/apt/lists/*

# Set Maven version to match the project's expected version
ARG MAVEN_VERSION=3.9.9
RUN su vscode -c "umask 0002 && . /usr/local/sdkman/bin/sdkman-init.sh && sdk install maven ${MAVEN_VERSION}"

# Set default shell
SHELL ["/bin/bash", "-c"]

# Set working directory
WORKDIR /workspace

# Environment defaults
ENV MIGRATION_SET=v2
ENV SPRING_PROFILES_ACTIVE=dev
