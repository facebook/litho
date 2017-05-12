#!/usr/bin/env bash

echo >&2 "Setting up Android environment"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source "$DIR/../../scripts/circle-ci-android-setup.sh" && installAndroidSDK
