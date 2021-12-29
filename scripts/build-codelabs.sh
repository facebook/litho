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

set -e

HERE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CODELABS=$HERE/../codelabs

for dir in "$CODELABS"/*/     # list directories in the form "full/dirname/"
do
    codelab="$( basename "$dir" )"
    echo >&2 "Building '${codelab}' codelab..."
    cd "$dir"
    ./gradlew clean assembleDebug --max-workers 2
done
