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

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.sections.specmodels.model.GroupSectionSpecModel;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;

public class GroupSectionSpecModelFactoryTestHelper {
  public static void create_forGroupSectionSpec_populateGenericSpecInfo(
      GroupSectionSpecModel groupSectionSpecModel,
      DependencyInjectionHelper mDependencyInjectionHelper) {
    assertThat(groupSectionSpecModel.getSpecName()).isEqualTo("TestGroupSectionSpec");
    assertThat(groupSectionSpecModel.getDelegateMethods()).hasSize(4);
    assertThat(groupSectionSpecModel.getProps()).hasSize(5);
    assertThat(groupSectionSpecModel.getStateValues()).hasSize(2);
    assertThat(groupSectionSpecModel.hasInjectedDependencies()).isTrue();
    assertThat(groupSectionSpecModel.getDependencyInjectionHelper())
        .isSameAs(mDependencyInjectionHelper);
  }

  public static void create_forGroupSectionSpec_populateServiceInfo(
      GroupSectionSpecModel groupSectionSpecModel) {
    MethodParamModel serviceParam = groupSectionSpecModel.getServiceParam();
    assertThat(serviceParam).isNotNull();
    assertThat(serviceParam.getTypeName().toString()).isEqualTo("java.lang.String");
  }

  public static void create_forGroupSectionSpec_populateTriggerInfo(
      GroupSectionSpecModel groupSectionSpecModel) {
    ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> triggers =
        groupSectionSpecModel.getTriggerMethods();
    assertThat(triggers).hasSize(1);
    assertThat(triggers.get(0).name.toString()).isEqualTo("onTestTriggerEvent");
  }

  public static void create_forGroupSectionSpec_populateEventInfo(
      GroupSectionSpecModel groupSectionSpecModel) {
    ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> events =
        groupSectionSpecModel.getEventMethods();
    assertThat(events).hasSize(1);
    assertThat(events.get(0).name.toString()).isEqualTo("onClickEvent");
  }

  public static void create_forGroupSectionSpec_populateUpdateStateInfo(
      GroupSectionSpecModel groupSectionSpecModel) {
    assertThat(groupSectionSpecModel.getUpdateStateMethods()).hasSize(2);
  }
}
