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

package com.facebook.litho.specmodels.processor;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.BindDynamicValueMethod;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.MountSpecModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;

public class MountSpecModelFactoryTestHelper {
  public static void create_forMountSpecWithExplicitMountType_populateGenericSpecInfo(
      MountSpecModel mountSpecModel, DependencyInjectionHelper mDependencyInjectionHelper) {
    assertThat(mountSpecModel.getSpecName()).isEqualTo("TestMountSpecWithExplicitMountType");
    assertThat(mountSpecModel.getComponentName()).isEqualTo("TestMountComponentName");
    assertThat(mountSpecModel.getMountType())
        .isEqualTo(ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_DRAWABLE);
    assertThat(mountSpecModel.getEventDeclarations()).hasSize(1);
    assertThat(mountSpecModel.getEventDeclarations().get(0).name.simpleName())
        .isEqualTo("TestTriggerEvent");

    final ImmutableList<SpecMethodModel<BindDynamicValueMethod, Void>> bindDynamicValueMethods =
        mountSpecModel.getBindDynamicValueMethods();
    assertThat(bindDynamicValueMethods).hasSize(1);
    assertThat(bindDynamicValueMethods.get(0).name.toString()).isEqualTo("onBindDynamicValue");
    assertThat(bindDynamicValueMethods.get(0).methodParams).hasSize(2);
    assertThat(bindDynamicValueMethods.get(0).methodParams.get(0).getName())
        .isEqualTo("colorDrawable");
    assertThat(bindDynamicValueMethods.get(0).methodParams.get(1).getName()).isEqualTo("prop10");

    assertThat(mountSpecModel.getDelegateMethods()).hasSize(9);

    final SpecMethodModel<DelegateMethod, Void> shouldRemeasureMethod =
        mountSpecModel.getDelegateMethods().get(6);
    assertThat(shouldRemeasureMethod.name).isEqualToIgnoringCase("shouldAlwaysRemeasure");
    assertThat(shouldRemeasureMethod.methodParams).hasSize(1);

    assertThat(mountSpecModel.getProps()).hasSize(10);
    assertThat(mountSpecModel.getStateValues()).hasSize(4);
    assertThat(mountSpecModel.getInterStageInputs()).hasSize(1);
    assertThat(mountSpecModel.getTreeProps()).hasSize(1);

    assertThat(mountSpecModel.isPublic()).isFalse();
    assertThat(mountSpecModel.isPureRender()).isTrue();

    assertThat(mountSpecModel.hasInjectedDependencies()).isTrue();
    assertThat(mountSpecModel.getDependencyInjectionHelper()).isSameAs(mDependencyInjectionHelper);

    assertThat(mountSpecModel.getTriggerMethods()).hasSize(1);
    SpecMethodModel<EventMethod, EventDeclarationModel> triggerMethodModel =
        mountSpecModel.getTriggerMethods().get(0);
    assertThat(triggerMethodModel.name).isEqualToIgnoringCase("testTrigger");
    assertThat(triggerMethodModel.methodParams).hasSize(2);

    assertThat(mountSpecModel.getUpdateStateWithTransitionMethods()).hasSize(1);
    assertThat(mountSpecModel.getUpdateStateWithTransitionMethods().get(0).name.toString())
        .isEqualTo("onUpdateStateWithTransition");
    assertThat(mountSpecModel.getUpdateStateWithTransitionMethods().get(0).methodParams).hasSize(1);
  }

  public static void create_forMountSpecWithExplicitMountType_populateOnAttachInfo(
      MountSpecModel mountSpecModel) {
    final SpecMethodModel<DelegateMethod, Void> onAttached =
        mountSpecModel.getDelegateMethods().get(7);
    assertThat(onAttached.name).isEqualToIgnoringCase("onAttached");
    assertThat(onAttached.methodParams).hasSize(3);
  }

  public static void create_forMountSpecWithExplicitMountType_populateOnDetachInfo(
      MountSpecModel mountSpecModel) {
    final SpecMethodModel<DelegateMethod, Void> onDetached =
        mountSpecModel.getDelegateMethods().get(8);
    assertThat(onDetached.name).isEqualToIgnoringCase("onDetached");
    assertThat(onDetached.methodParams).hasSize(3);
  }
}
