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
    package = "com.facebook",
    res = "src/main/res",
    visibility = [
        "PUBLIC",
    ],
    deps = [
    ],
)

android_build_config(
    name = "build_config",
    package = "com.facebook.components",
    values = [
        "boolean IS_INTERNAL_BUILD = true",
    ],
    visibility = [
        COMPONENTS_VISIBILITY,
    ],
)
