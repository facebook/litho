/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.app.Application;

import com.facebook.stetho.InspectorModulesProvider;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.inspector.elements.DescriptorProvider;
import com.facebook.stetho.inspector.elements.android.AndroidDocumentProviderFactory;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;

import java.util.Arrays;

public class LithoWebKitInspector implements InspectorModulesProvider {
  private final Application mApplication;

  public LithoWebKitInspector(Application application) {
    mApplication = application;
  }

  @Override
  public Iterable<ChromeDevtoolsDomain> get() {
    final Stetho.DefaultInspectorModulesBuilder defaultModulesBuilder =
        new Stetho.DefaultInspectorModulesBuilder(mApplication);

    defaultModulesBuilder.documentProvider(
        new AndroidDocumentProviderFactory(
            mApplication,
            Arrays.<DescriptorProvider>asList(new ComponentsDescriptorProvider())));

    return defaultModulesBuilder.finish();
  }
}
