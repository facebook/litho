#!/usr/bin/env bash

set -ex
shopt -s globstar

# Deploys build artifacts to your local m2 repository.
VERSION="0.1.0-SNAPSHOT"
TMPDIR=/tmp/m2tmp

./gradlew assemble

mkdir -p $TMPDIR

for file in **/build/libs/*.jar; do
  ARTIFACT=$(basename "$file" | sed 's/\.jar$//')

  mvn install:install-file \
    -Dfile=$file \
    -DgroupId=com.facebook.litho \
    -DartifactId=$ARTIFACT \
    -Dversion=$VERSION \
    -Dpackaging=jar \
    -DcreateChecksum=true \
    -DlocalRepositoryPath=$TMPDIR
done

for file in **/build/outputs/**/*-release.aar; do
  ARTIFACT=$(basename "$file" | sed 's/-release\.aar$//')

  mvn install:install-file \
    -Dfile=$file \
    -DgroupId=com.facebook.litho \
    -DartifactId=$ARTIFACT \
    -Dversion=$VERSION \
    -Dpackaging=aar \
    -DcreateChecksum=true \
    -DlocalRepositoryPath=$TMPDIR
done

for file in $TMPDIR/**/maven-metadata-local.xml*; do
  RENAMED=$(echo $file | sed 's/-local\.xml/.xml/')
  mv $file $RENAMED
done

echo "Local artifact deployment complete."
echo "You can now upload you stuff, e.g. with "
echo "rsync --delete -avP /tmp/m2tmp/ $USER@remotehost:www/html/m2repo/"
