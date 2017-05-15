#!/usr/bin/env bash

set -ex

echo >&2 "Building litho targets"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd "$DIR/../.."

echo -e "[download]\n   proxy_host=fwdproxy.any\n   proxy_port=8080\n proxy_type=HTTP\n" > .buckconfig.local
echo -e "[build]\n    thread_core_ratio = 1\n thread_core_ratio_reserved_cores = 0\n" >> .buckconfig.local

export BUCKVERSION="$(cat "$DIR"/../../../../.buckversion)"
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK=$HOME/android-sdk
export BUCK_PATH=/mnt/dewey/buck/.commits/"$BUCKVERSION"/cli/linux
export PATH=$BUCK_PATH:$PATH

buck fetch //...
buck build sample
buck build //:components
