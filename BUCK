# Copyright (c) 2014-present, Facebook, Inc.
# All rights reserved.
#
# This source code is licensed under the BSD-style license found in the
# LICENSE file in the root directory of this source tree. An additional grant
# of patent rights can be found in the PATENTS file in the same directory.

include_defs("//COMPONENTS_DEFS")

android_library(
    name = "components",
    exported_deps = [
        COMPONENTS_DEBUG_JAVA_TARGET,
        COMPONENTS_JAVA_TARGET,
        COMPONENTS_YOGAPREBUILT_TARGET,
        COMPONENTS_ANDROIDSUPPORT_TARGET,
    ],
    visibility = [
        "PUBLIC",
    ],
    deps = [
        ":build_config",
    ],
)

android_aar(
    name = "release-litho-core",
    include_build_config_class = True,
    manifest_skeleton = "src/main/AndroidManifest.xml",
    visibility = [
        "PUBLIC",
    ],
    deps = [
        COMPONENTS_JAVA_TARGET,
        COMPONENTS_CONFIG_TARGET,
        ":build_config",
        ":res",
    ],
)

android_resource(
    name = "res",
