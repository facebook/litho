#!/bin/bash

HERE=libraries/components/facebook/lego
LEGO_DIR=${LEGO_DIR:-/tmp/lego-litho-oss}
cat <<EOF
[{
  "alias": "litho-oss-test",
  "command": "SandcastleUniversalCommand",
  "vcsType": "fb4a-fbsource",
  "capabilities": {
    "vcs": "fb4a-fbsource",
    "type": "android",
    "network": "ipv6"
  },
  "commandArgs": {
    "name": "litho-oss-test",
    "oncall": "components-android",
    "timeout": 10800,
    "env": {
      "http_proxy": "fwdproxy:8080",
      "https_proxy": "fwdproxy:8080",
      "BUCK_DISTCC": "0",
      "NO_BUCKD": "1"
    },
    "steps": [
      {
        "user": "root",
        "name": "Set up Android environment",
        "shell": "$HERE/setup-android.sh",
        "required": true
      },
      {
        "user": "root",
        "name": "Build litho targets",
        "shell": "$HERE/build-litho.sh",
        "required": true
      },
      {
        "user": "root",
        "name": "Test litho targets",
        "shell": "$HERE/test-litho.sh",
        "required": true
      }
    ]
  }
}]
EOF
