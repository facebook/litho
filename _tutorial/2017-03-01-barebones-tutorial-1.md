---
title: Barebones Tutorial Part 1/5
layout: tutorial_post
author: rspencer
category: tutorial
---

## Getting an app off the ground

In this, the first of five basic tutorials in using Litho, we'll build a very simple app.  This app will be built with [buck][buck], and use the [SoLoader][SoLoader] library.  Apart from that, though, it won't do anything.  The purpose of this tutorial is to set the stage for when we add Litho code, so that we don't have to clutter up with unrelated fluff.

**Requirements:** Buck set up with Android SDK.

<!--truncate-->

Lets start with a super basic Android manifest in `src/main/AndroidManifest.xml`.  This manifest simply defines the application `SampleApplication` and the action `SampleActivity`, and makes it runnable.  We don't ask for any special permissions or specify any interesting names or icons.  This is a barebones tutorial!

``` xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="com.company.tutorial">

  <uses-sdk android:minSdkVersion="15" android:targetSdkVersion="19"/>

  <application
      android:name="com.company.tutorial.SampleApplication">

   <activity android:name="com.company.tutorial.SampleActivity">
     <intent-filter>
       <action android:name="android.intent.action.MAIN"/>
       <category android:name="android.intent.category.LAUNCHER"/>
     </intent-filter>
   </activity>

  </application>
</manifest>
```

To build the app, we need a root `BUCK` file:

``` python
android_binary(
    name = "sample-barebones",
    keystore = ":debug_keystore",
    manifest = "src/main/AndroidManifest.xml",
    deps = [
        "//src/main/java/com/company/tutorial:tutorial",
    ],
)

keystore(
    name = "debug_keystore",
    properties = "debug.keystore.properties",
    store = "debug.keystore",
)
```

Look [here][debug_keystore] for instructions on creating a debug keystore, and [here][debug.keystore.properties] for a `debug.keystore.properties` file.

Now, we need some java (we referred to it in `deps` above).  In `src/main/java/com/company/tutorial` add the `BUCK` file:

``` python
android_library(
    name = "tutorial",
    srcs = glob(["**/*.java"]),
    visibility = [
        "PUBLIC",
    ],
    deps = [
        '//lib:soloader',
    ],
)
```

This buck target has all the `*.java` files in `src/main/java/com/company/tutorial` as its sources.  We just need two, a simple application, `SampleApplication.java`

``` java
package com.company.tutorial;

import android.app.Application;

import com.facebook.soloader.SoLoader;

public class SampleApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    SoLoader.init(this, false);
  }
}
```

and a simple activity, `SampleActivity.java`

``` java
package com.company.tutorial;

import android.app.Activity;

public class SampleActivity extends Activity {
}
```

Note the `SoLoader.init` in the application.  SoLoader is a library for loading native libraries, and their dependencies.  Since Litho uses [Yoga][yoga] for layout (which has native dependencies), we need SoLoader.  This init step needs to be called before almost anything else, so we don't try reference unloaded libraries.

Finally, add a target in `/lib/BUCK` to actually download SoLoader

``` python
android_prebuilt_aar(
    name = "soloader",
    aar = ":soloader-aar",
    visibility = ["PUBLIC"],
)

remote_file(
    name = "soloader-aar",
    sha1 = "918573465c94c6bc9bad48ef259f1e0cd6543c1b",
    url = "mvn:com.facebook.soloader:soloader:aar:0.1.0",
)
```

Now we can build and test on an emulator with `buck fetch :sample-barebones` and `buck install -r :sample-barebones`.  With any luck, this should show a blank app!


[debug_keystore]: https://coderwall.com/p/r09hoq/android-generate-release-debug-keystores
[debug.keystore.properties]: https://github.com/{{ site.ghrepo }}/blob/master/sample/debug.keystore.properties
[buck]: https://buckbuild.com
[yoga]: https://facebook.github.io/yoga/
[SoLoader]: https://github.com/facebook/SoLoader
