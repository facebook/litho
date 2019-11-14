# Copyright (c) 2017-present, Facebook, Inc.
#
# This source code is licensed under the Apache 2.0 license found in the
# LICENSE file in the root directory of this source tree.

load("//tools/build_defs/oss:fb_native_wrapper.bzl", "fb_native")
load(
    "//tools/build_defs/oss:litho_defs.bzl",
    "LITHO_ANDROIDSUPPORT_TARGET",
    "LITHO_BUILD_CONFIG_TARGET",
    "LITHO_JAVA_TARGET",
    "LITHO_KOTLIN_TARGET",
    "LITHO_SECTIONS_ANNOTATIONS_TARGET",
    "LITHO_SECTIONS_COMMON_TARGET",
    "LITHO_SECTIONS_TARGET",
    "LITHO_SECTIONS_WIDGET_TARGET",
    "LITHO_VISIBILITY",
    "LITHO_WIDGET_TARGET",
    "LITHO_YOGA_TARGET",
    "fb_core_android_library",
    "litho_android_library",
)

litho_android_library(
    name = "components",
    visibility = [
        "PUBLIC",
    ],
    deps = [
        LITHO_BUILD_CONFIG_TARGET,
    ],
    exported_deps = [
        LITHO_JAVA_TARGET,
        LITHO_ANDROIDSUPPORT_TARGET,
        LITHO_YOGA_TARGET,
    ],
)

litho_android_library(
    name = "sections_core",
    visibility = [
        "PUBLIC",
    ],
    exported_deps = [
        ":components",
        LITHO_SECTIONS_TARGET,
        LITHO_SECTIONS_ANNOTATIONS_TARGET,
    ],
)

litho_android_library(
    name = "litho_core_kotlin",
    visibility = [
        "PUBLIC",
    ],
    exported_deps = [
        ":components",
        LITHO_KOTLIN_TARGET,
    ],
)

fb_core_android_library(
    name = "sections",
    visibility = [
        "PUBLIC",
    ],
    exported_deps = [
        ":sections_core",
        LITHO_SECTIONS_COMMON_TARGET,
        LITHO_SECTIONS_WIDGET_TARGET,
        LITHO_WIDGET_TARGET,
    ],
)

fb_native.android_build_config(
    name = "build_config",
    package = "com.facebook.litho",
    values_file = ":build_config_values",
    visibility = LITHO_VISIBILITY,
)

fb_native.genrule(
    name = "build_config_values",
    srcs = [
        "config/build_config_values",
    ],
    out = "extra_build_config_values",
    cmd = "SRCARR=($SRCS); cat ${SRCARR[0]} | sed 's/{{IS_DEBUG}}/%s/' > $OUT" % (read_config("litho", "is_debug", "true")),
)
