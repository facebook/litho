/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.testing.processor;

import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.processor.AbstractComponentsProcessor;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.facebook.litho.specmodels.processor.MountSpecModelFactory;
import com.facebook.litho.specmodels.processor.testing.TestSpecModelFactory;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * A bare-minimum implementation of an {@link AbstractComponentsProcessor}. This allows testing
 * generic specs.
 *
 * <p>This is only to be used for tests as the generated code is rather useless for any production
 * use cases.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class TestingGenericComponentProcessor extends AbstractComponentsProcessor {

  public TestingGenericComponentProcessor() {
    super(
        ImmutableList.of(
            new LayoutSpecModelFactory(), new MountSpecModelFactory(), new TestSpecModelFactory()));
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return new LinkedHashSet<>(
        Arrays.asList(
            ClassNames.LAYOUT_SPEC.toString(),
            ClassNames.MOUNT_SPEC.toString(),
            ClassNames.TEST_SPEC.toString()));
  }
}
