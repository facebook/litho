#!/bin/bash
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

echo "Downloading package lists..."
sudo apt-get update -y

if ! [[ -d ~/vendor/apt ]]; then
  mkdir -p ~/vendor/apt
fi

# First check for archives cache
if ! [[ -d ~/vendor/apt/archives ]]; then
  # It doesn't so download the packages
  echo "Downloading build dependencies..."
  sudo apt-get install --download-only ant autoconf automake g++ gcc libqt5widgets5 lib32z1 lib32stdc++6 make maven python-dev python3-dev qml-module-qtquick-controls qtdeclarative5-dev file -y
  # Then move them to our cache directory
  sudo cp -R /var/cache/apt ~/vendor/
  # Making sure our user has ownership, in order to cache
  sudo chown -R ${USER:=$(/usr/bin/id -run)}:$USER ~/vendor/apt
fi

# Install all packages in the cache
echo "Installing build dependencies..."
sudo dpkg -i ~/vendor/apt/archives/*.deb