#!/bin/bash
# Copyright (c) Facebook, Inc. and its affiliates.
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

set -e
set -o pipefail

# litho-intellij-plugin
DIR=$(dirname "$0")
cd "$DIR"
rm -rf build
echo "./gradlew :litho-intellij-plugin:buildPlugin"
../gradlew :litho-intellij-plugin:buildPlugin &> /dev/null

cd build/distributions
mkdir tmp
unzip -uoq litho-intellij-plugin.zip -d tmp
cd tmp/litho-intellij-plugin/lib
mkdir tmp

for filename in ./*.jar; do
    [ -e "$filename" ] || continue
    unzip -uoq "$filename" -d tmp
done

rm ./*.jar

_JAR="litho-intellij-plugin.jar"
jar -cf "$_JAR" -C tmp .

echo "$(pwd)/$_JAR"

while getopts "p:" opt; do
  case $opt in
    p)
      cp "$_JAR" "$OPTARG"
    ;;
    \?)
      exit 1
    ;;
  esac
done
