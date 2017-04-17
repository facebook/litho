function installAndroidSDK {
  # temporarily
  rm -rf /home/ubuntu/android-sdk

  TMP=/tmp/sdk$$.zip
  curl -L -o $TMP 'https://dl.google.com/android/repository/tools_r25.2.3-linux.zip'
  unzip -d /home/ubuntu/android-sdk $TMP
  rm $TMP

  export ANDROID_HOME=/home/ubuntu/android-sdk
  export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$PATH"

  echo > $ANDROID_HOME/licenses/android-sdk-license
  echo -n 8933bad161af4178b1185d1a37fbf41ea5269c55 > $ANDROID_HOME/licenses/android-sdk-license

  echo y | sdkmanager 'build-tools;23.0.2' 'build-tools;25.0.2' 'build-tools;25.0.1' 'platforms;android-23' 'platforms;android-25' 'ndk-bundle' 'extras;android;m2repository'
}
