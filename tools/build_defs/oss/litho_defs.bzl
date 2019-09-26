load(":glob_defs.bzl", "subdir_glob")

LITHO_ROOT = "//"

LITHO_VISIBILITY = [
    "PUBLIC",
]

LITHO_TESTING_UTIL_VISIBILITY = [
    "PUBLIC",
]

LITHO_IS_OSS_BUILD = True

def make_dep_path(pth):
    return LITHO_ROOT + pth

LITHO_ROOT_TARGET = make_dep_path(":components")

# Java source
LITHO_JAVA_TARGET = make_dep_path("litho-core/src/main/java/com/facebook/litho:litho")

LITHO_ANNOTATIONS_TARGET = make_dep_path("litho-annotations/src/main/java/com/facebook/litho/annotations:annotations")

LITHO_CONFIG_TARGET = make_dep_path("litho-core/src/main/java/com/facebook/litho/config:config")

LITHO_PERFBOOST_TARGET = make_dep_path("litho-core/src/main/java/com/facebook/litho/perfboost:perfboost")

LITHO_VIEWCOMPAT_TARGET = make_dep_path("litho-core/src/main/java/com/facebook/litho/viewcompat:viewcompat")

LITHO_UTILS_TARGET = make_dep_path("litho-core/src/main/java/com/facebook/litho/utils:utils")

LITHO_WIDGET_ACCESSIBILITIES_TARGET = make_dep_path("litho-widget/src/main/java/com/facebook/litho/widget/accessibility:accessibility")

LITHO_WIDGET_TARGET = make_dep_path("litho-widget/src/main/java/com/facebook/litho/widget:widget")

LITHO_LITHO_FRESCO_TARGET = make_dep_path("litho-fresco/src/main/java/com/facebook/litho/fresco:fresco")

LITHO_STATS_TARGET = make_dep_path("litho-core/src/main/java/com/facebook/litho/stats:stats")

LITHO_YOGA_FACTORY_TARGET = make_dep_path("litho-core/src/main/java/com/facebook/litho/yoga:yoga")

LITHO_TESTING_CORE_V3_TARGET = make_dep_path("litho-testing/src/main/java/com/facebook/litho:litho")

LITHO_TESTING_CORE_V4_TARGET = make_dep_path("litho-testing/src/main/java/com/facebook/litho:litho-v4")

LITHO_TESTING_V3_TARGET = make_dep_path("litho-testing/src/main/java/com/facebook/litho/testing:testing")

LITHO_TESTING_V4_TARGET = make_dep_path("litho-testing/src/main/java/com/facebook/litho/testing:testing-v4")

LITHO_TESTING_WHITEBOX_TARGET = make_dep_path("litho-testing/src/main/java/com/facebook/litho/testing:whitebox")

LITHO_TESTING_ASSERTJ_V3_TARGET = make_dep_path("litho-testing/src/main/java/com/facebook/litho/testing/assertj:assertj")

LITHO_TESTING_ASSERTJ_V4_TARGET = make_dep_path("litho-testing/src/main/java/com/facebook/litho/testing/assertj:assertj-v4")

LITHO_TESTING_HELPER_V3_TARGET = make_dep_path("litho-testing/src/main/java/com/facebook/litho/testing/helper:helper")

LITHO_TESTING_HELPER_V4_TARGET = make_dep_path("litho-testing/src/main/java/com/facebook/litho/testing/helper:helper-v4")

LITHO_TESTING_SUBCOMPONENTS_TARGET = make_dep_path("litho-testing/src/main/java/com/facebook/litho/testing/subcomponents:subcomponents")

LITHO_TESTING_WIDGET_TARGET = make_dep_path("litho-testing/src/main/java/com/facebook/litho/widget:widget")

LITHO_TESTING_ESPRESSO_TARGET = make_dep_path("litho-espresso/src/main/java/com/facebook/litho/testing/espresso:espresso")

LITHO_TEST_RES = make_dep_path("litho-it/src/main:res")

LITHO_SECTIONS_TARGET = make_dep_path("litho-sections-core/src/main/java/com/facebook/litho/sections:sections")

LITHO_SECTIONS_COMMON_TARGET = make_dep_path("litho-sections-core/src/main/java/com/facebook/litho/sections/common:common")

LITHO_SECTIONS_WIDGET_TARGET = make_dep_path("litho-sections-widget/src/main/java/com/facebook/litho/sections/widget:widget")

LITHO_SECTIONS_ANNOTATIONS_TARGET = make_dep_path("litho-sections-annotations/src/main/java/com/facebook/litho/sections/annotations:annotations")

LITHO_SECTIONS_PROCESSOR_TARGET = make_dep_path("litho-sections-processor/src/main/java/com/facebook/litho/sections/specmodels/processor:processor")

LITHO_SECTIONS_CONFIG_TARGET = make_dep_path("litho-sections-core/src/main/java/com/facebook/litho/sections/config:config")

LITHO_SECTIONS_DEBUG_TARGET = make_dep_path("litho-sections-debug/src/main/java/com/facebook/litho/sections/debug:debug")

LITHO_SECTIONS_DEBUG_WIDGET_TARGET = make_dep_path("litho-sections-debug/src/main/java/com/facebook/litho/sections/debug/widget:widget")

LITHO_FBJNI_JAVA_TARGET = make_dep_path("lib/fbjni/src/main/java/com/facebook/jni:jni")

# Test source
LITHO_TEST_TARGET = make_dep_path("litho-it/src/test/java/com/facebook/litho:litho")

# Java source with local upstream
LITHO_PROGUARDANNOTATIONS_TARGET = make_dep_path("litho-annotations/src/main/java/com/facebook/proguard/annotations:annotations")

# Resources
LITHO_RES_TARGET = make_dep_path("litho-core:res")

# Libraries
LITHO_INFERANNOTATIONS_TARGET = make_dep_path("lib/infer-annotations:infer-annotations")

LITHO_JSR_TARGET = make_dep_path("lib/jsr-305:jsr-305")

LITHO_ANDROIDSUPPORT_TARGET = make_dep_path("lib/androidx:androidx")

LITHO_ANDROIDSUPPORT_RECYCLERVIEW_TARGET = make_dep_path("lib/androidx:androidx-recyclerview")

LITHO_ANDROIDSUPPORT_APPCOMPAT_TARGET = make_dep_path("lib/androidx:androidx-appcompat")

LITHO_ANDROIDSUPPORT_TESTING_TARGET = make_dep_path("lib/androidx:androidx-testing")

LITHO_YOGA_TARGET = make_dep_path("lib/yoga:yoga")

LITHO_YOGAJNI_TARGET = make_dep_path("lib/yogajni:jni")

LITHO_PROGUARD_ANNOTATIONS_TARGET = make_dep_path("lib/yoga:proguard-annotations")

LITHO_BUILD_CONFIG_TARGET = make_dep_path(":build_config")

LITHO_COMMONS_CLI_TARGET = make_dep_path("lib/commons-cli:commons-cli")

LITHO_TEXTLAYOUTBUILDER_TARGET = make_dep_path("lib/textlayoutbuilder:textlayoutbuilder")

LITHO_JAVAPOET_TARGET = make_dep_path("lib/javapoet:javapoet")

LITHO_FBCORE_TARGET = make_dep_path("lib/fbcore:fbcore")

LITHO_SOLOADER_TARGET = make_dep_path("lib/soloader:soloader")

LITHO_ASSERTJ_TARGET = make_dep_path("lib/assertj:assertj")

LITHO_COMPILE_TESTING_TARGET = make_dep_path("lib/compile-testing:compile-testing")

LITHO_TRUTH_TARGET = make_dep_path("lib/truth:truth")

LITHO_MOCKITO_TARGET = make_dep_path("lib/mockito:mockito")

LITHO_POWERMOCK_MOCKITO_TARGET = make_dep_path("lib/powermock:powermock-mockito")

LITHO_JNI_TARGET = make_dep_path("lib/jni-hack:jni-hack")

LITHO_FBJNI_TARGET = make_dep_path("lib/fbjni:jni")

LITHO_GUAVA_TARGET = make_dep_path("lib/guava:guava")

LITHO_DIFFUTILS_TARGET = make_dep_path("lib/diff-utils:diff-utils")

LITHO_ESPRESSO_TARGET = make_dep_path("lib/espresso:espresso")

LITHO_SCREENSHOT_TARGET = make_dep_path("lib/screenshot:screenshot")

LITHO_JAVAC_TOOLS_TARGET = make_dep_path("lib/javac-tools:javac-tools")

# Fresco
LITHO_FRESCO_TARGET = make_dep_path("lib/fresco:fresco")

LITHO_ROBOLECTRIC_V3_TARGET = make_dep_path("lib/robolectric3:robolectric3")

LITHO_ROBOLECTRIC_V4_TARGET = make_dep_path("lib/robolectric4:robolectric4")

LITHO_JUNIT_TARGET = make_dep_path("lib/junit:junit")

LITHO_HAMCREST_LIBRARY_TARGET = make_dep_path("lib/hamcrest:hamcrest")

LITHO_HAMCREST_CORE_TARGET = make_dep_path("lib/hamcrest:hamcrest")

# Annotation processors
LITHO_PROCESSOR_TARGET = make_dep_path("litho-processor/src/main/java/com/facebook/litho/specmodels/processor:processor")

LITHO_PROCESSOR_LIB_TARGET = make_dep_path("litho-processor/src/main/java/com/facebook/litho/specmodels/processor:processor-lib")

LITHO_SECTIONS_PROCESSOR_LIB_TARGET = make_dep_path("litho-sections-processor/src/main/java/com/facebook/litho/sections/specmodels/processor:processor-lib")

# Sample app
LITHO_SAMPLE_JAVA = make_dep_path("sample/src/main/java/com/facebook/samples/litho:litho")

LITHO_SAMPLE_BAREBONES_JAVA = make_dep_path("sample-barebones/src/main/java/com/facebook/samples/lithobarebones:lithobarebones")

LITHO_SAMPLE_BAREBONES_RES = make_dep_path("sample-barebones:res")

LITHO_SAMPLE_CODELAB_JAVA = make_dep_path("sample-codelab/src/main/java/com/facebook/samples/lithocodelab:lithocodelab")

LITHO_SAMPLE_CODELAB_RES = make_dep_path("sample-codelab:res")

LITHO_SAMPLE_RES = make_dep_path("sample:res")

# Other targets
LITHO_OSS_TARGET = make_dep_path(":components")

# Targets that sometimes exist and sometimes don't
LITHO_TEXTLAYOUTBUILDER_UTILS_TARGET = []

LITHO_FRESCO_TARGETS = [
    make_dep_path("lib/fbcore:fbcore"),
    make_dep_path("lib/fresco:fresco-drawee"),
    make_dep_path("lib/fresco:fresco"),
]

LITHO_FLIPPER_TARGETS = [
    make_dep_path("lib/flipper:flipper"),
]

LITHO_FRESCO_PIPELINE_TARGET = [make_dep_path("lib/fresco:imagepipeline")]

LITHO_FRESCO_CONTROLLER_TARGET = []

LITHO_FRESCO_INTERFACES_TARGET = []

def components_robolectric_test(
        name,
        *args,
        **kwargs):
    """Tests that can successfully run from the library root folder."""
    extra_vm_args = [
        "-Drobolectric.dependency.dir=lib/android-all",
        "-Dcom.facebook.litho.is_oss=true",
    ]
    kwargs["vm_args"] = extra_vm_args
    kwargs["use_cxx_libraries"] = True
    kwargs["cxx_library_whitelist"] = [
        "//lib/yogajni:jni",
        "//lib/fbjni:jni",
    ]

    # T41117446 Remove after AndroidX conversion is done.
    kwargs.pop("is_androidx", False)

    native.robolectric_test(
        name = name,
        *args,
        **kwargs
    )

def fb_java_test(*args, **kwargs):
    """Uses native java_test for OSS project."""

    # T41117446 Remove after AndroidX conversion is done.
    kwargs.pop("is_androidx", False)
    java_test(*args, **kwargs)

def litho_android_library(name, srcs = None, *args, **kwargs):
    srcs = srcs or []

    # This has no meaning in OSS.
    kwargs.pop("fblite", None)

    # T41117446 Remove after AndroidX conversion is done.
    kwargs.pop("is_androidx", False)
    native.android_library(name, srcs = srcs, *args, **kwargs)

components_robolectric_powermock_test = components_robolectric_test

def fb_xplat_cxx_library(*args, **kwargs):
    """Delegates to cxx_library for OSS project."""

    # T41117446 Remove after AndroidX conversion is done.
    kwargs.pop("is_androidx", False)
    native.cxx_library(*args, **kwargs)

def fb_android_resource(**kwargs):
    """Delegates to native android_resource rule."""

    # T41117446 Remove after AndroidX conversion is done.
    kwargs.pop("is_androidx", False)
    android_resource(**kwargs)

def fb_java_binary(**kwargs):
    """Delegates to native java_binary rule."""
    native.java_binary(**kwargs)

def fb_java_library(**kwargs):
    """Delegates to native java_library rule."""

    # T41117446 Remove after AndroidX conversion is done.
    kwargs.pop("is_androidx", False)
    native.java_library(**kwargs)

def fb_android_library(**kwargs):
    """Delegates to native android_library rule."""

    # T41117446 Remove after AndroidX conversion is done.
    kwargs.pop("is_androidx", False)
    native.android_library(**kwargs)

def fb_prebuilt_cxx_library(**kwargs):
    """Delegates to native prebuilt_cxx_library."""
    native.prebuilt_cxx_library(**kwargs)

def instrumentation_test(**kwargs):
    """
    We don't support this in the OSS build for now.
    Please use Gradle instead.
    """

    # T41117446 Remove after AndroidX conversion is done.
    kwargs.pop("is_androidx", False)
    _ignore = kwargs
    pass

def fb_core_android_library(**kwargs):
    # T41117446 Remove after AndroidX conversion is done.
    kwargs.pop("is_androidx", False)
    native.android_library(**kwargs)

def define_fbjni_targets():
    # This target is only used in open source
    fb_prebuilt_cxx_library(
        name = "ndklog",
        exported_platform_linker_flags = [
            (
                "^android.*",
                ["-llog"],
            ),
        ],
        header_only = True,
        visibility = LITHO_VISIBILITY,
    )

    fb_xplat_cxx_library(
        name = "jni",
        srcs = native.glob(
            [
                "src/main/cpp/fb/**/*.cpp",
            ],
        ),
        header_namespace = "",
        exported_headers = subdir_glob(
            [
                ("src/main/cpp", "fb/**/*.h"),
                ("src/main/cpp", "fbjni/**/*.h"),
            ],
        ),
        compiler_flags = [
            "-fno-omit-frame-pointer",
            "-fexceptions",
            "-frtti",
            "-Wall",
            "-std=c++11",  # FIXME
            "-DDISABLE_CPUCAP",
            "-DDISABLE_XPLAT",
        ],
        exported_platform_headers = [
            (
                "^(?!android-arm$).*$",
                subdir_glob([
                    ("src/main/cpp", "lyra/*.h"),
                ]),
            ),
        ],
        platform_srcs = [
            (
                "^(?!android-arm$).*$",
                native.glob([
                    "src/main/cpp/lyra/*.cpp",
                ]),
            ),
        ],
        soname = "libfb.$(ext)",
        visibility = LITHO_VISIBILITY,
        deps = [
            LITHO_JNI_TARGET,
            ":ndklog",
        ],
    )

# This target is only used in open source and will break the monobuild
# because we cannot define `soname` multiple times.
def define_yogajni_targets():
    fb_prebuilt_cxx_library(
        name = "ndklog",
        exported_platform_linker_flags = [
            (
                "^android.*",
                ["-llog"],
            ),
        ],
        header_only = True,
        visibility = LITHO_VISIBILITY,
    )

    fb_xplat_cxx_library(
        name = "jni",
        srcs = native.glob(["src/main/cpp/jni/*.cpp"]),
        header_namespace = "",
        compiler_flags = [
            "-fno-omit-frame-pointer",
            "-fexceptions",
            "-Wall",
            "-O3",
            "-std=c++11",  # FIXME
        ],
        soname = "libyoga.$(ext)",
        visibility = LITHO_VISIBILITY,
        deps = [
            make_dep_path("lib/yoga/src/main/cpp:yoga"),
            LITHO_FBJNI_TARGET,
            ":ndklog",
        ],
    )

# This target is only used in open source and will break the monobuild
# because we cannot define `soname` multiple times.
def define_cpp_yoga_targets():
    fb_prebuilt_cxx_library(
        name = "ndklog",
        exported_platform_linker_flags = [
            (
                "^android.*",
                ["-llog"],
            ),
        ],
        header_only = True,
        visibility = LITHO_VISIBILITY,
    )
    fb_xplat_cxx_library(
        name = "yoga",
        srcs = native.glob(["yoga/**/*.cpp"]),
        header_namespace = "",
        exported_headers = native.glob(["yoga/**/*.h"]),
        compiler_flags = [
            "-fno-omit-frame-pointer",
            "-fexceptions",
            "-Wall",
            "-std=c++11",  # FIXME
            "-O3",
        ],
        force_static = True,
        visibility = LITHO_VISIBILITY,
        deps = [
            ":ndklog",
        ],
    )
