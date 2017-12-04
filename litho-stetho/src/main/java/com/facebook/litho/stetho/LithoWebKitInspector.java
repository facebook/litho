/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.stetho;

import android.app.Application;
import android.widget.Toast;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.stetho.InspectorModulesProvider;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.inspector.elements.DescriptorProvider;
import com.facebook.stetho.inspector.elements.android.AndroidDocumentProviderFactory;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import java.util.Arrays;

/**
 * @deprecated We are deprecating Stetho integration as of 0.12.0. We are looking into better
 *     solutions which will improve the litho debugging experience.
 */
@Deprecated
public class LithoWebKitInspector implements InspectorModulesProvider {
  private final Application mApplication;

  public LithoWebKitInspector(Application application) {
    mApplication = application;
    ComponentsConfiguration.isDebugModeEnabled = true;
  }

  @Override
  public Iterable<ChromeDevtoolsDomain> get() {
    final Stetho.DefaultInspectorModulesBuilder defaultModulesBuilder =
        new Stetho.DefaultInspectorModulesBuilder(mApplication);

    defaultModulesBuilder.documentProvider(
        new AndroidDocumentProviderFactory(
            mApplication,
            Arrays.<DescriptorProvider>asList(new ComponentsDescriptorProvider())));

    Toast.makeText(
            mApplication,
            "The litho-stetho plugin is being deprecated and will be removed with 0.12.0.",
            Toast.LENGTH_LONG)
        .show();

    return defaultModulesBuilder.finish();
  }
}
