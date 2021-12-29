#!/usr/bin/env bash
# Copyright (c) Meta Platforms, Inc. and affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

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
