#!/usr/bin/bash
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

function download() {
  if hash curl 2>/dev/null; then
    curl -s -L -o "$2" "$1"
  elif hash wget 2>/dev/null; then
    wget -O "$2" "$1"
  else
    echo >&2 "No supported download tool installed. Please get either wget or curl."
    exit
  fi
}

function installsdk() {
  # We need an existing SDK with `sdkmanager`, otherwise, install it.
  which sdkmanager &> /dev/null || getAndroidSDK

  PROXY_ARGS=""
  if [[ ! -z "$HTTPS_PROXY" ]]; then
    PROXY_HOST="$(echo "$HTTPS_PROXY" | cut -d : -f 1,1)"
    PROXY_PORT="$(echo "$HTTPS_PROXY" | cut -d : -f 2,2)"
    PROXY_ARGS="--proxy=http --proxy_host=$PROXY_HOST --proxy_port=$PROXY_PORT"
  fi

  echo y | "$ANDROID_HOME/tools/bin/sdkmanager" $PROXY_ARGS "$@"
}

function getAndroidSDK {
  TMP=/tmp/sdk$$.zip
  download 'https://dl.google.com/android/repository/tools_r25.2.3-linux.zip' $TMP
  unzip -qod "$ANDROID_SDK" $TMP
  rm $TMP
}

function getAndroidNDK {
  NDK_HOME="/opt/ndk"
  DEPS="$NDK_HOME/installed-dependencies"

  if [ ! -e $DEPS ]; then
    cd $NDK_HOME
    echo "Downloading NDK..."
    TMP=/tmp/ndk$$.zip
    download https://dl.google.com/android/repository/android-ndk-r15c-linux-x86_64.zip "$TMP"
    unzip -qo "$TMP"
    echo "Installed Android NDK at $NDK_HOME"
    touch $DEPS
    rm "$TMP"
  fi
}

function installAndroidSDK {
  export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$PATH"

  mkdir -p "$ANDROID_HOME/licenses/"
  echo > "$ANDROID_HOME/licenses/android-sdk-license"
  echo -n 24333f8a63b6825ea9c5514f83c2829b004d1fee > "$ANDROID_HOME/licenses/android-sdk-license"

  installsdk 'platforms;android-28' 'cmake;3.6.4111459' 'build-tools;28.0.3'
}
