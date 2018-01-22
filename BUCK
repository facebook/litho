# Copyright (c) 2017-present, Facebook, Inc.
# All rights reserved.
#
# This source code is licensed under the BSD-style license found in the
# LICENSE file in the root directory of this source tree. An additional grant
# of patent rights can be found in the PATENTS file in the same directory.

include_defs("//LITHO_DEFS")

litho_android_library(
    name = "components",
    exported_deps = [
        LITHO_JAVA_TARGET,
        LITHO_ANDROIDSUPPORT_TARGET,
        LITHO_YOGA_TARGET,
    ],
    visibility = [
        "PUBLIC",
    ],
    deps = [
        LITHO_BUILD_CONFIG_TARGET,
    ],
)

litho_android_library(
    name = "sections_core",
    exported_deps = [
        ":components",
        LITHO_SECTIONS_TARGET,
        LITHO_SECTIONS_ANNOTATIONS_TARGET,
    ],
    visibility = [
        "PUBLIC",
    ],
)

android_library(
    name = "sections",
    exported_deps = [
        ":sections_core",
        LITHO_SECTIONS_COMMON_TARGET,
        LITHO_SECTIONS_WIDGET_TARGET,
        LITHO_WIDGET_TARGET,
    ],
    visibility = [
        "PUBLIC",
    ],
)

android_build_config(
    name = "build_config",
    package = "com.facebook.litho",
    values_file = ":build_config_values",
    visibility = LITHO_VISIBILITY,
)

genrule(
    name = "build_config_values",
    srcs = [
        "config/build_config_values",
    ],
    out = "extra_build_config_values",
    cmd = "SRCARR=($SRCS); cat ${SRCARR[0]} | sed 's/{{IS_DEBUG}}/%s/' > $OUT" % (read_config("litho", "is_debug", "true")),
)
