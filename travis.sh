#!/bin/bash

set -euo pipefail

if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
  echo 'Build, deploy and analyze commit in master'
  mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy sonar:sonar \
    -Pcoverage-per-test,repox,deploy-artifactory \
    -Dmaven.test.redirectTestOutputToFile=false \
    -Dsonar.host.url=$SONAR_HOST_URL \
    -Dsonar.login=$SONAR_TOKEN \
    -Dartifactory.deploy.username=$REPOX_DEPLOY_USERNAME \
    -Dartifactory.deploy.password=$REPOX_DEPLOY_PASSWORD \
    -B -e -V \
    -s settings-repox.xml

elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ -n "${GITHUB_TOKEN-}" ]; then
  echo 'Build and analyze pull request'
  mvn org.jacoco:jacoco-maven-plugin:prepare-agent verify \
    -Pcoverage-per-test,repox \
    -Dmaven.test.redirectTestOutputToFile=false \
    -Dsonar.analysis.mode=issues \
    -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
    -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
    -Dsonar.github.oauth=$GITHUB_TOKEN \
    -Dsonar.host.url=$SONAR_HOST_URL \
    -Dsonar.login=$SONAR_TOKEN \
    -B -e -V \
    -s settings-repox.xml
    
else
  echo 'Build, no deploy, no analysis'
  # Build branch, without any analysis
  mvn verify \
    -Dmaven.test.redirectTestOutputToFile=false \
    -B -e -V \
    -s settings-repox.xml
fi

