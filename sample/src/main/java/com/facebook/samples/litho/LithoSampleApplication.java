/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho;

import android.app.Application;

import com.facebook.litho.ComponentsDescriptorProvider;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.soloader.SoLoader;
import com.facebook.stetho.InspectorModulesProvider;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.inspector.elements.DescriptorProvider;
import com.facebook.stetho.inspector.elements.android.AndroidDocumentProviderFactory;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;

import java.util.Arrays;

public class LithoSampleApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    Fresco.initialize(this);
    SoLoader.init(this, false);
    Stetho.initialize(Stetho.newInitializerBuilder(this).enableWebKitInspector(
        new InspectorModulesProvider() {
          @Override
          public Iterable<ChromeDevtoolsDomain> get() {
            final Stetho.DefaultInspectorModulesBuilder defaultModulesBuilder =
                new Stetho.DefaultInspectorModulesBuilder(LithoSampleApplication.this);

            defaultModulesBuilder.documentProvider(new AndroidDocumentProviderFactory(
                LithoSampleApplication.this,
                Arrays.<DescriptorProvider>asList(new ComponentsDescriptorProvider())));

            return defaultModulesBuilder.finish();
          }
        }).build());
