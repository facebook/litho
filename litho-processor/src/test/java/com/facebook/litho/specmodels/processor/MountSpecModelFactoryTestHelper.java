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
  public static void mountSpec_initModel_populateGenericSpecInfo(
      MountSpecModel mMountSpecModel, DependencyInjectionHelper mDependencyInjectionHelper) {
    assertThat(mMountSpecModel.getSpecName()).isEqualTo("TestMountSpec");
    assertThat(mMountSpecModel.getComponentName()).isEqualTo("TestMountComponentName");
    assertThat(mMountSpecModel.getMountType())
        .isEqualTo(ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_DRAWABLE);
    assertThat(mMountSpecModel.getEventDeclarations()).hasSize(1);
    assertThat(mMountSpecModel.getEventDeclarations().get(0).name.simpleName())
        .isEqualTo("TestTriggerEvent");

    final ImmutableList<SpecMethodModel<BindDynamicValueMethod, Void>> bindDynamicValueMethods =
        mMountSpecModel.getBindDynamicValueMethods();
    assertThat(bindDynamicValueMethods).hasSize(1);
    assertThat(bindDynamicValueMethods.get(0).name.toString()).isEqualTo("onBindDynamicValue");
    assertThat(bindDynamicValueMethods.get(0).methodParams).hasSize(2);
    assertThat(bindDynamicValueMethods.get(0).methodParams.get(0).getName())
        .isEqualTo("colorDrawable");
    assertThat(bindDynamicValueMethods.get(0).methodParams.get(1).getName()).isEqualTo("prop10");

    assertThat(mMountSpecModel.getDelegateMethods()).hasSize(9);

    final SpecMethodModel<DelegateMethod, Void> shouldRemeasureMethod =
        mMountSpecModel.getDelegateMethods().get(6);
    assertThat(shouldRemeasureMethod.name).isEqualToIgnoringCase("shouldAlwaysRemeasure");
    assertThat(shouldRemeasureMethod.methodParams).hasSize(1);

    assertThat(mMountSpecModel.getProps()).hasSize(10);
    assertThat(mMountSpecModel.getStateValues()).hasSize(4);
    assertThat(mMountSpecModel.getInterStageInputs()).hasSize(1);
    assertThat(mMountSpecModel.getTreeProps()).hasSize(1);

    assertThat(mMountSpecModel.isPublic()).isFalse();
    assertThat(mMountSpecModel.isPureRender()).isTrue();

    assertThat(mMountSpecModel.hasInjectedDependencies()).isTrue();
    assertThat(mMountSpecModel.getDependencyInjectionHelper()).isSameAs(mDependencyInjectionHelper);

    assertThat(mMountSpecModel.getTriggerMethods()).hasSize(1);
    SpecMethodModel<EventMethod, EventDeclarationModel> triggerMethodModel =
        mMountSpecModel.getTriggerMethods().get(0);
    assertThat(triggerMethodModel.name).isEqualToIgnoringCase("testTrigger");
    assertThat(triggerMethodModel.methodParams).hasSize(2);
  }

  public static void mountSpec_initModel_populateOnAttachInfo(MountSpecModel mMountSpecModel) {
    final SpecMethodModel<DelegateMethod, Void> onAttached =
        mMountSpecModel.getDelegateMethods().get(7);
    assertThat(onAttached.name).isEqualToIgnoringCase("onAttached");
    assertThat(onAttached.methodParams).hasSize(3);
  }

  public static void mountSpec_initModel_populateOnDetachInfo(MountSpecModel mMountSpecModel) {
    final SpecMethodModel<DelegateMethod, Void> onDetached =
        mMountSpecModel.getDelegateMethods().get(8);
    assertThat(onDetached.name).isEqualToIgnoringCase("onDetached");
    assertThat(onDetached.methodParams).hasSize(3);
  }
}
