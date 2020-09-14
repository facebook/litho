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

import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;

public class LayoutSpecModelFactoryTestHelper {
  public static void layoutSpec_initModel_populateGenericSpecInfo(
      LayoutSpecModel mLayoutSpecModel, DependencyInjectionHelper mDependencyInjectionHelper) {
    assertThat(mLayoutSpecModel.getSpecName()).isEqualTo("TestLayoutSpec");
    assertThat(mLayoutSpecModel.getComponentName()).isEqualTo("TestLayoutComponentName");
    assertThat(mLayoutSpecModel.getDelegateMethods()).hasSize(4);
    assertThat(mLayoutSpecModel.getProps()).hasSize(4);
    assertThat(mLayoutSpecModel.getStateValues()).hasSize(2);
    assertThat(mLayoutSpecModel.hasInjectedDependencies()).isTrue();
    assertThat(mLayoutSpecModel.getDependencyInjectionHelper())
        .isSameAs(mDependencyInjectionHelper);
  }

  public static void layoutSpec_initModel_populateOnAttachInfo(LayoutSpecModel mLayoutSpecModel) {
    final SpecMethodModel<DelegateMethod, Void> onAttached =
        mLayoutSpecModel.getDelegateMethods().get(2);
    assertThat(onAttached.name.toString()).isEqualTo("onAttached");
    assertThat(onAttached.methodParams).hasSize(3);
  }

  public static void layoutSpec_initModel_populateOnDetachInfo(LayoutSpecModel mLayoutSpecModel) {
    final SpecMethodModel<DelegateMethod, Void> onDetached =
        mLayoutSpecModel.getDelegateMethods().get(3);
    assertThat(onDetached.name.toString()).isEqualTo("onDetached");
    assertThat(onDetached.methodParams).hasSize(3);
  }
}
