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
  PROXY_ARGS=""
  if [[ ! -z "$HTTPS_PROXY" ]]; then
    PROXY_HOST="$(echo $HTTPS_PROXY | cut -d : -f 1,1)"
    PROXY_PORT="$(echo $HTTPS_PROXY | cut -d : -f 2,2)"
    PROXY_ARGS="--proxy=http --proxy_host=$PROXY_HOST --proxy_port=$PROXY_PORT"
  fi

  echo y | "$ANDROID_HOME"/tools/bin/sdkmanager $PROXY_ARGS "$@"
}

function getAndroidNDK {
  NDK_HOME="/opt/ndk"
  DEPS="$NDK_HOME/installed-dependencies"

  if [ ! -e $DEPS ]; then
    cd $NDK_HOME
    echo "Downloading NDK..."
    curl -o ndk.zip https://dl.google.com/android/repository/android-ndk-r15c-linux-x86_64.zip
    unzip -o -q ndk.zip
    echo "Installed Android NDK at $NDK_HOME"
    touch $DEPS
    rm ndk.zip
  fi
}

function installAndroidSDK {
  export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$PATH"

  mkdir -p $ANDROID_HOME/licenses/
  echo > $ANDROID_HOME/licenses/android-sdk-license
  echo -n d56f5187479451eabf01fb78af6dfcb131a6481e > $ANDROID_HOME/licenses/android-sdk-license

  ls -la $ANDROID_HOME

  installsdk 'build-tools;25.0.3' 'build-tools;26.0.2' 'platforms;android-25' 'platforms;android-26' 'ndk-bundle' 'extras;android;m2repository'
}
