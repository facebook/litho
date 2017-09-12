/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor.testing;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.processor.AbstractComponentsProcessor;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ComponentsTestingProcessor extends AbstractComponentsProcessor {

  public ComponentsTestingProcessor() {
    super(ImmutableList.of(new TestSpecModelFactory()), null);
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return new LinkedHashSet<>(Collections.singletonList(ClassNames.TEST_SPEC.toString()));
  }
}
