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

package com.facebook.litho.sections.specmodels.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.facebook.litho.Component;
import com.facebook.litho.Diff;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.ChangeSet;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.OnDiff;
import com.facebook.litho.sections.specmodels.model.DiffSectionSpecModel;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.google.testing.compile.CompilationRule;
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DiffSectionSpecModelFactoryTest {

  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  private final DependencyInjectionHelper mDependencyInjectionHelper =
      mock(DependencyInjectionHelper.class);

  private final DiffSectionSpecModelFactory mFactory = new DiffSectionSpecModelFactory();

  private DiffSectionSpecModel mDiffSectionSpecModel;

  @DiffSectionSpec(value = "TestDiffSectionComponentName", isPublic = false)
  static class TestDiffSectionSpec {

    @OnDiff
    public static void onCreateChangeSet(
        SectionContext context,
        ChangeSet changeSet,
        @Prop Diff<Component> component,
        @Prop(optional = true) Diff<Object> data) {
      final Object prevData = data.getPrevious();
      final Object nextData = data.getNext();
      final Component prevComponent = component.getPrevious();
      final Component nextComponent = component.getNext();

      if (prevComponent == null && nextComponent == null) {
        return;
      }

      if (prevComponent != null && nextComponent == null) {
        changeSet.delete(0, prevData);
        return;
      }
    }
  }

  @Before
  public void setUp() {
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();
    TypeElement typeElement =
        elements.getTypeElement(
            DiffSectionSpecModelFactoryTest.TestDiffSectionSpec.class.getCanonicalName());

    mDiffSectionSpecModel =
        mFactory.create(
            elements,
            types,
            typeElement,
            mock(Messager.class),
            RunMode.normal(),
            mDependencyInjectionHelper,
            null);
  }

  @Test
  public void create_forGroupSectionSpec_populateGenericInfo() {
    DiffSectionSpecModelFactoryTestHelper.create_forDiffSectionSpec_populateGenericSpecInfo(
        mDiffSectionSpecModel);
  }

  @Test
  public void testUpdateStateWithTransitionMethodsIsNotNull() {
    assertThat(mDiffSectionSpecModel.getUpdateStateWithTransitionMethods())
        .describedAs(
            "UpdateStateWithTransitionMethods cannot be null as otherwise the Litho Structure will not render")
        .isNotNull();
  }
}
