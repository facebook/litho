/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import static org.mockito.Mockito.mock;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.annotations.Event;
import com.facebook.litho.annotations.FromTrigger;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnBindService;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.annotations.OnCreateService;
import com.facebook.litho.sections.specmodels.model.GroupSectionSpecModel;
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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test {@link GroupSectionSpecModelFactory} * */
@RunWith(JUnit4.class)
public class GroupSectionSpecModelFactoryTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  private final DependencyInjectionHelper mDependencyInjectionHelper =
      mock(DependencyInjectionHelper.class);

  private final GroupSectionSpecModelFactory mFactory = new GroupSectionSpecModelFactory();

  private GroupSectionSpecModel mGroupSectionSpecModel;

  @Event
  static class TestTriggerEvent {
    int testTriggerVar;
  }

  @GroupSectionSpec(value = "TestGroupSectionComponentName", isPublic = false)
  static class TestGroupSectionSpec {
    @OnCreateInitialState
    static void onCreateInitialState(@Prop int prop1, @State Integer state1) {}

    @OnUpdateState
    static void onUpdateState1(Integer state1) {}

    @OnUpdateState
    static void onUpdateState2(Integer state2) {}

    @OnCreateChildren
    static Component onCreateChildren(
        SectionContext c, @Prop int prop2, @Prop int prop3, @State Integer state1) {
      return null;
    }

    @OnCreateService
    static String onCreateService(SectionContext c, @Prop int prop4, @State Integer state2) {
      return "Test";
    }

    @OnBindService
    static void onBindService(SectionContext c, String service, @Prop String prop5) {}

    @OnTrigger(TestTriggerEvent.class)
    static void onTestTriggerEvent(SectionContext c, @FromTrigger int testTriggerVar) {}

    @OnEvent(ClickEvent.class)
    static void onClickEvent(SectionContext c) {}
  }

  @Before
  public void setUp() {
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();
    TypeElement typeElement =
        elements.getTypeElement(
            GroupSectionSpecModelFactoryTest.TestGroupSectionSpec.class.getCanonicalName());

    mGroupSectionSpecModel =
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
    GroupSectionSpecModelFactoryTestHelper.create_forGroupSectionSpec_populateGenericSpecInfo(
        mGroupSectionSpecModel, mDependencyInjectionHelper);
  }

  @Test
  public void create_forGroupSectionSpec_populateServiceInfo() {
    GroupSectionSpecModelFactoryTestHelper.create_forGroupSectionSpec_populateServiceInfo(
        mGroupSectionSpecModel);
  }

  @Test
  public void create_forGroupSectionSpec_populateTriggerInfo() {
    GroupSectionSpecModelFactoryTestHelper.create_forGroupSectionSpec_populateTriggerInfo(
        mGroupSectionSpecModel);
  }

  @Test
  public void create_forGroupSectionSpec_populateEventInfo() {
    GroupSectionSpecModelFactoryTestHelper.create_forGroupSectionSpec_populateEventInfo(
        mGroupSectionSpecModel);
  }

  @Test
  public void create_forGroupSectionSpec_populateUpdateStateInfo() {
    GroupSectionSpecModelFactoryTestHelper.create_forGroupSectionSpec_populateUpdateStateInfo(
        mGroupSectionSpecModel);
  }
}
