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

#
# Deploy a SNAPSHOT JAR after every successful Circle CI To Sonatype.
# See https://circleci.com/docs/1.0/environment-variables/
#

set -e

BASEDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
IS_SNAPSHOT="$(grep 'VERSION_NAME=[0-9\.]\+-SNAPSHOT' "$BASEDIR/gradle.properties")"

if [ "$CIRCLE_PROJECT_USERNAME" != "facebook" ]; then
  echo "Skipping repository. Expected project username to be 'facebook', but was '$CIRCLE_PROJECT_USERNAME'."
  exit
elif [ "$CIRCLE_PROJECT_REPONAME" != "litho" ]; then
  echo "Skipping repository. Expected project name to be 'litho', but was '$CIRCLE_PROJECT_REPONAME'."
  exit
elif [ "$CIRCLE_BRANCH" != "master" ]; then
  echo "Skipping build. Expected branch name to be 'master', but was '$CIRCLE_BRANCH'."
  exit
elif [ "$CI_PULL_REQUEST" != "" ]; then
  echo "Skipping build. Only considering non-PR builds, but URL was '$CI_PULL_REQUEST'."
  exit
elif [ "$IS_SNAPSHOT" == "" ]; then
  echo "Skipping build. Given build doesn't appear to be a SNAPSHOT release."
else
  "$BASEDIR/gradlew" uploadArchives
fi
