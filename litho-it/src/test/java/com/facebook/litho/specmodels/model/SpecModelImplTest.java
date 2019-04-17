/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.model;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.testing.specmodels.MockMethodParamModel;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests {@link SpecModelImpl}
 */
@RunWith(JUnit4.class)
public class SpecModelImplTest {
  private static final String TEST_QUALIFIED_SPEC_NAME = "com.facebook.litho.TestSpec";
  private static final String TEST_QUALIFIED_COMPONENT_NAME = "com.facebook.litho.Test";

  PropModel mPropModel1;
  PropModel mPropModel2;
  PropModel mPropModel3;
  PropModel mPropModel4;
  PropModel mUnderlyingPropModel1;
  PropModel mUnderlyingPropModel2;
  SimpleMethodParamModel mMethodParamModel;
  TreePropModel mTreePropModel1;
  TreePropModel mTreePropModel2;
  InjectPropModel mInjectPropModel;

  SpecMethodModel<DelegateMethod, Void> mMethodModel1;
  SpecMethodModel<DelegateMethod, Void> mMethodModel2;

  SpecMethodModel<EventMethod, EventDeclarationModel> mTriggerMethodModel;
  WorkingRangeMethodModel mWorkingRangeMethodModel;

  TypeVariableName mTypeVariableName1 = TypeVariableName.get("test1");
  TypeVariableName mTypeVariableName2 = TypeVariableName.get("test2");
  List<TypeVariableName> mTypeVariableNames = new ArrayList<>(2);

  PropDefaultModel mPropDefaultModel1;

  @Before
  public void setUp() {
    mPropModel1 =
        new PropModel(
            MockMethodParamModel.newBuilder().name("propModel1").type(TypeName.BOOLEAN).build(),
            false,
            false,
            false,
            null,
            "");
    mPropModel2 =
        new PropModel(
            MockMethodParamModel.newBuilder().name("propModel2").type(TypeName.INT).build(),
            false,
            false,
            false,
            null,
            "");
    mPropModel3 =
        new PropModel(
            MockMethodParamModel.newBuilder().name("propModel3").type(TypeName.LONG).build(),
            false,
            false,
            false,
            null,
            "");
    mPropModel4 =
        new PropModel(
            MockMethodParamModel.newBuilder().name("propModel4").type(TypeName.INT).build(),
            false,
            false,
            false,
            null,
            "");

    mUnderlyingPropModel1 =
        new PropModel(
            MockMethodParamModel.newBuilder()
                .name("propModel1")
                .type(TypeName.BOOLEAN.box())
                .build(),
            false,
            false,
            false,
            null,
            "");
    mUnderlyingPropModel2 =
        new PropModel(
            MockMethodParamModel.newBuilder().name("differentName").build(),
            false,
            false,
            false,
            null,
            "");

    mInjectPropModel =
        new InjectPropModel(MockMethodParamModel.newBuilder().name("injectProp").build(), true);

    mTreePropModel1 =
        new TreePropModel(MockMethodParamModel.newBuilder().name("treeprop1").build());
    mMethodParamModel =
        new SimpleMethodParamModel(
            new TypeSpec(TypeName.INT),
            "methodparam",
            ImmutableList.of(),
            ImmutableList.of(),
            new Object());
    mPropDefaultModel1 =
        new PropDefaultModel(TypeName.INT, "propdefault", ImmutableList.of(), new Object());

    List<MethodParamModel> params1 = new ArrayList<>();
    params1.add(mPropModel1);
    params1.add(mPropModel2);
    params1.add(mTreePropModel1);
    params1.add(new DiffPropModel(mUnderlyingPropModel1));
    params1.add(new DiffPropModel(mUnderlyingPropModel2));

    List<MethodParamModel> params2 = new ArrayList<>();
    params2.add(mPropModel3);
    params2.add(mMethodParamModel);

    mTreePropModel2 =
        new TreePropModel(MockMethodParamModel.newBuilder().name("treeprop2").build());

    List<MethodParamModel> params3 = new ArrayList<>();
    params3.add(mPropModel4);
    params3.add(mTreePropModel2);

    List<MethodParamModel> params4 = new ArrayList<>();
    params3.add(mPropModel1);
    params3.add(mTreePropModel2);

    List<MethodParamModel> params5 = new ArrayList<>();
    params3.add(mPropModel3);
    params3.add(mTreePropModel1);

    mMethodModel1 =
        SpecMethodModel.<DelegateMethod, Void>builder()
            .annotations(ImmutableList.of())
            .modifiers(ImmutableList.<Modifier>of())
            .name("method1")
            .returnTypeSpec(new TypeSpec(TypeName.BOOLEAN))
            .methodParams(ImmutableList.copyOf(params1))
            .build();
    mMethodModel2 =
        SpecMethodModel.<DelegateMethod, Void>builder()
            .annotations(ImmutableList.<Annotation>of())
            .modifiers(ImmutableList.<Modifier>of())
            .name("method2")
            .returnTypeSpec(new TypeSpec(TypeName.BOOLEAN))
            .methodParams(ImmutableList.copyOf(params2))
            .build();

    mTriggerMethodModel =
        SpecMethodModel.<EventMethod, EventDeclarationModel>builder()
            .annotations(ImmutableList.<Annotation>of())
            .modifiers(ImmutableList.<Modifier>of())
            .name("method3")
            .returnTypeSpec(new TypeSpec(TypeName.BOOLEAN))
            .methodParams(ImmutableList.copyOf(params3))
            .build();

    mWorkingRangeMethodModel = new WorkingRangeMethodModel("workingRange");
    mWorkingRangeMethodModel.enteredRangeModel =
        SpecMethodModel.<EventMethod, WorkingRangeDeclarationModel>builder()
            .annotations(ImmutableList.<Annotation>of())
            .modifiers(ImmutableList.<Modifier>of())
            .name("method4")
            .returnTypeSpec(new TypeSpec(TypeName.VOID))
            .methodParams(ImmutableList.copyOf(params4))
            .build();
    mWorkingRangeMethodModel.exitedRangeModel =
        SpecMethodModel.<EventMethod, WorkingRangeDeclarationModel>builder()
            .annotations(ImmutableList.<Annotation>of())
            .modifiers(ImmutableList.<Modifier>of())
            .name("method5")
            .returnTypeSpec(new TypeSpec(TypeName.VOID))
            .methodParams(ImmutableList.copyOf(params5))
            .build();

    mTypeVariableNames.add(mTypeVariableName1);
    mTypeVariableNames.add(mTypeVariableName2);
  }

  @Test
  public void testCreateSpecModelImplWithoutDependencyInjection() {
    SpecModel specModel =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
            .delegateMethods(ImmutableList.of(mMethodModel1, mMethodModel2))
            .triggerMethods(ImmutableList.of(mTriggerMethodModel))
            .workingRangeMethods(ImmutableList.of(mWorkingRangeMethodModel))
            .typeVariables(ImmutableList.copyOf(mTypeVariableNames))
            .propDefaults(ImmutableList.of(mPropDefaultModel1))
            .representedObject(new Object())
            .build();

    assertThat(specModel.getSpecName()).isEqualTo("TestSpec");
    assertThat(specModel.getComponentName()).isEqualTo("Test");
    assertThat(specModel.getSpecTypeName().toString()).isEqualTo(TEST_QUALIFIED_SPEC_NAME);
    assertThat(specModel.getComponentTypeName().toString())
        .isEqualTo(TEST_QUALIFIED_COMPONENT_NAME);

    assertThat(specModel.getDelegateMethods()).hasSize(2);
    assertThat(specModel.getDelegateMethods()).contains(mMethodModel1, mMethodModel2);

    assertThat(specModel.getTriggerMethods()).hasSize(1);
    assertThat(specModel.getTriggerMethods()).contains(mTriggerMethodModel);

    assertThat(specModel.getWorkingRangeMethods()).hasSize(1);
    assertThat(specModel.getWorkingRangeMethods()).contains(mWorkingRangeMethodModel);

    assertThat(specModel.getProps()).hasSize(5);
    assertThat(specModel.getProps())
        .contains(mPropModel1, mPropModel2, mPropModel3, mPropModel4, mUnderlyingPropModel2);

    assertThat(specModel.getPropDefaults()).hasSize(1);
    assertThat(specModel.getPropDefaults()).contains(mPropDefaultModel1);

    assertThat(specModel.getTypeVariables()).hasSize(2);
    assertThat(specModel.getTypeVariables()).contains(mTypeVariableName1, mTypeVariableName2);

    assertThat(specModel.getTreeProps()).hasSize(2);
    assertThat(specModel.getTreeProps()).contains(mTreePropModel1, mTreePropModel2);

    assertThat(specModel.hasInjectedDependencies()).isFalse();
    assertThat(specModel.getDependencyInjectionHelper()).isNull();
  }

  @Test
  public void testCreateSpecModelImplWithDependencyInjection() {
    DependencyInjectionHelper diGenerator = mock(DependencyInjectionHelper.class);
    SpecModel specModel =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
            .delegateMethods(ImmutableList.of(mMethodModel1, mMethodModel2))
            .triggerMethods(ImmutableList.of(mTriggerMethodModel))
            .workingRangeMethods(ImmutableList.of(mWorkingRangeMethodModel))
            .typeVariables(ImmutableList.copyOf(mTypeVariableNames))
            .propDefaults(ImmutableList.of(mPropDefaultModel1))
            .dependencyInjectionHelper(diGenerator)
            .representedObject(new Object())
            .build();

    assertThat(specModel.hasInjectedDependencies()).isTrue();
    assertThat(specModel.getDependencyInjectionHelper()).isSameAs(diGenerator);
  }

  @Test
  public void testOffsetRegressionForInjectProp() {
    final List<MethodParamModel> params1 = new ArrayList<>();
    params1.add(mPropModel1);
    params1.add(mPropModel2);
    params1.add(mInjectPropModel);

    final List<MethodParamModel> params2 = new ArrayList<>();
    params2.add(mPropModel1);

    final SpecMethodModel<DelegateMethod, Void> method1 =
        SpecMethodModel.<DelegateMethod, Void>builder()
            .annotations(ImmutableList.of())
            .modifiers(ImmutableList.of())
            .name("method1")
            .returnTypeSpec(new TypeSpec(TypeName.BOOLEAN))
            .methodParams(ImmutableList.copyOf(params1))
            .build();
    final SpecMethodModel<DelegateMethod, Void> method2 =
        SpecMethodModel.<DelegateMethod, Void>builder()
            .annotations(ImmutableList.of())
            .modifiers(ImmutableList.of())
            .name("method2")
            .returnTypeSpec(new TypeSpec(TypeName.BOOLEAN))
            .methodParams(ImmutableList.copyOf(params2))
            .build();

    DependencyInjectionHelper diGenerator = mock(DependencyInjectionHelper.class);
    SpecModel specModel =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
            .delegateMethods(ImmutableList.of(method1, method2))
            .cachedPropNames(ImmutableList.of("savedOne", "savedTwo", "savedThree", "savedFour"))
            .dependencyInjectionHelper(diGenerator)
            .representedObject(new Object())
            .build();

    final ImmutableList<PropModel> props = specModel.getProps();
    final ImmutableList<InjectPropModel> injectProps = specModel.getInjectProps();

    assertThat(props).hasSize(3);
    assertThat(injectProps).hasSize(1);

    assertThat(props.stream().map(PropModel::getName).collect(Collectors.toList()))
        .containsExactly("savedOne", "savedThree", "savedTwo");
    assertThat(injectProps.stream().map(InjectPropModel::getName).collect(Collectors.toList()))
        .containsExactly("savedFour");
  }
}
