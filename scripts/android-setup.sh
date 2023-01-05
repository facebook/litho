#!/usr/bin/bash
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

[ -z "$ANDROID_HOME" ] && ANDROID_HOME="/opt/android_sdk"

if env | grep -q ^ANDROID_HOME=
then
  echo "ANDROID_HOME env variable is already exported"
else
  echo "ANDROID_HOME env variable was not exported. setting it now"
  export ANDROID_HOME
fi

# New environment variables required
export ANDROID_SDK_ROOT=$ANDROID_HOME

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
  getAndroidSDK

  PROXY_ARGS=""
  if [[ ! -z "$HTTPS_PROXY" ]]; then
    PROXY_HOST="$(echo "$HTTPS_PROXY" | cut -d : -f 1,1)"
    PROXY_PORT="$(echo "$HTTPS_PROXY" | cut -d : -f 2,2)"
    PROXY_ARGS="--proxy=http --proxy_host=$PROXY_HOST --proxy_port=$PROXY_PORT"
  fi

  echo y | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" $PROXY_ARGS "$@"
}

function getAndroidSDK {
  TMP=/tmp/sdk$$.zip

  echo "downloading sdk command line tools"
  download 'https://dl.google.com/android/repository/commandlinetools-linux-7302050_latest.zip' $TMP

  echo "clean $ANDROID_HOME"
  rm -r $ANDROID_HOME/*

  echo "unzipping to $ANDROID_HOME"
  unzip -qod $ANDROID_HOME/cmdline-tools/ $TMP

  echo "rename dir"
  mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest

  echo "deleting $TMP"
  rm $TMP
}

function installAndroidSDK {
  export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$PATH"

  mkdir -p "$ANDROID_HOME/licenses/"
  echo > "$ANDROID_HOME/licenses/android-sdk-license"
  echo -n 24333f8a63b6825ea9c5514f83c2829b004d1fee > "$ANDROID_HOME/licenses/android-sdk-license"

  installsdk 'platforms;android-30' 'cmake;3.6.4111459' 'build-tools;30.0.2'
}
