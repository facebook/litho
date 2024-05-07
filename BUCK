# Copyright (c) 2017-present, Facebook, Inc.
#
# This source code is licensed under the Apache 2.0 license found in the
# LICENSE file in the root directory of this source tree.

load("@fbsource//xplat/pfh/FBApp_UIFrameworks_Litho_Litho:DEFS.bzl", "FBApp_UIFrameworks_Litho_Litho")
load("//tools/build_defs/oss:fb_native_wrapper.bzl", "fb_native")
load(
    "//tools/build_defs/oss:litho_defs.bzl",
    "LITHO_ANDROIDSUPPORT_TARGET",
    "LITHO_BUILD_CONFIG_TARGET",
    "LITHO_COMPONENTS_TARGET_VISIBILITY",
    "LITHO_COROUTINES_KOTLIN_TARGET",
    "LITHO_JAVA_TARGET",
    "LITHO_PERF_LOGGER",
    "LITHO_SECTIONS_ANNOTATIONS_TARGET",
    "LITHO_SECTIONS_COMMON_TARGET",
    "LITHO_SECTIONS_TARGET",
    "LITHO_SECTIONS_WIDGET_TARGET",
    "LITHO_TOOLING",
    "LITHO_VISIBILITY",
    "LITHO_WIDGET_TARGET",
    "LITHO_YOGA_TARGET",
    "fb_core_android_library",
    "litho_android_library",
)

litho_android_library(
    name = "components",
    autoglob = False,
    language = "JAVA",
    manifest = "AndroidManifest.xml",
    visibility = LITHO_COMPONENTS_TARGET_VISIBILITY,
    deps = [
        LITHO_BUILD_CONFIG_TARGET,
    ],
    exported_deps = [
        LITHO_ANDROIDSUPPORT_TARGET,
        LITHO_JAVA_TARGET,
        LITHO_YOGA_TARGET,
    ],
)

litho_android_library(
    name = "sections_core",
    autoglob = False,
    language = "JAVA",
    visibility = [
        "PUBLIC",
    ],
    exported_deps = [
        ":components",
        LITHO_SECTIONS_ANNOTATIONS_TARGET,
        LITHO_SECTIONS_TARGET,
    ],
)

litho_android_library(
    name = "litho_core_kotlin",
    autoglob = False,
    language = "JAVA",
    visibility = [
        "PUBLIC",
    ],
    exported_deps = [
        ":components",
        LITHO_JAVA_TARGET,
    ],
)

litho_android_library(
    name = "litho_tooling",
    autoglob = False,
    visibility = [
        "PUBLIC",
    ],
    exported_deps = [
        LITHO_TOOLING,
        LITHO_JAVA_TARGET,
    ],
)

litho_android_library(
    name = "litho_coroutines_kotlin",
    autoglob = False,
    language = "JAVA",
    visibility = [
        "PUBLIC",
    ],
    exported_deps = [
        LITHO_COROUTINES_KOTLIN_TARGET,
    ],
)

litho_android_library(
    name = "litho_perf_logger",
    feature = FBApp_UIFrameworks_Litho_Litho,
    required_for_source_only_abi = True,
    visibility = ["PUBLIC"],
    exported_deps = [
        LITHO_PERF_LOGGER,
    ],
)

fb_core_android_library(
    name = "sections",
    feature = FBApp_UIFrameworks_Litho_Litho,
    labels = [],
    visibility = [
        "PUBLIC",
    ],
    exported_deps = [
        ":sections_no_codegen",
    ],
)

# Same as "sections", but without triggering the codegen
fb_core_android_library(
    name = "sections_no_codegen",
    feature = FBApp_UIFrameworks_Litho_Litho,
    labels = [],
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
    cmd = "SRCARR=($SRCS); cat ${SRCARR[0]} | sed 's/{{IS_DEBUG}}/%s/' > $OUT" % read_config("litho", "is_debug", "true"),
)
