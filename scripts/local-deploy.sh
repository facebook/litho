#!/usr/bin/env bash

set -ex
shopt -s globstar

# Deploys build artifacts to your local m2 repository.
VERSION="0.1.0-SNAPSHOT"

./gradlew assemble

for file in **/build/libs/*.jar; do
  ARTIFACT=$(basename "$file" | sed 's/\.jar$//')

  mvn install:install-file \
    -Dfile=$file \
    -DgroupId=com.facebook.litho \
    -DartifactId=$ARTIFACT \
    -Dversion=$VERSION \
    -Dpackaging=jar
done

for file in **/build/outputs/**/*-release.aar; do
  ARTIFACT=$(basename "$file" | sed 's/-release\.aar$//')

  mvn install:install-file \
    -Dfile=$file \
    -DgroupId=com.facebook.litho \
    -DartifactId=$ARTIFACT \
    -Dversion=$VERSION \
    -Dpackaging=aar
done
