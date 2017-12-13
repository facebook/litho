/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link SpecModelImpl}
 */
public class SpecModelImplTest {
  private static final String TEST_QUALIFIED_SPEC_NAME = "com.facebook.litho.TestSpec";
  private static final String TEST_QUALIFIED_COMPONENT_NAME = "com.facebook.litho.Test";

  PropModel mPropModel1;
  PropModel mPropModel2;
  PropModel mPropModel3;
  PropModel mUnderlyingPropModel1;
  PropModel mUnderlyingPropModel2;
  SimpleMethodParamModel mMethodParamModel;
  TreePropModel mTreePropModel;

  SpecMethodModel<DelegateMethod, Void> mMethodModel1;
  SpecMethodModel<DelegateMethod, Void> mMethodModel2;

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
            null,
            "");
    mPropModel2 =
        new PropModel(
            MockMethodParamModel.newBuilder().name("propModel2").type(TypeName.INT).build(),
            false,
            null,
            "");
    mPropModel3 =
        new PropModel(
            MockMethodParamModel.newBuilder().name("propModel3").type(TypeName.LONG).build(),
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
            null,
            "");
    mUnderlyingPropModel2 =
        new PropModel(
            MockMethodParamModel.newBuilder().name("differentName").build(), false, null, "");

    mTreePropModel = new TreePropModel(MockMethodParamModel.newBuilder().name("treeprop").build());
    mMethodParamModel =
        new SimpleMethodParamModel(
            TypeName.INT, "methodparam", ImmutableList.of(), ImmutableList.of(), new Object());
    mPropDefaultModel1 =
        new PropDefaultModel(TypeName.INT, "propdefault", ImmutableList.of(), new Object());

    List<MethodParamModel> params1 = new ArrayList<>();
    params1.add(mPropModel1);
    params1.add(mPropModel2);
    params1.add(mTreePropModel);
    params1.add(new DiffPropModel(mUnderlyingPropModel1));
    params1.add(new DiffPropModel(mUnderlyingPropModel2));

    List<MethodParamModel> params2 = new ArrayList<>();
    params2.add(mPropModel3);
    params2.add(mMethodParamModel);

    mMethodModel1 =
        new SpecMethodModel<DelegateMethod, Void>(
            ImmutableList.<Annotation>of(),
            ImmutableList.<Modifier>of(),
            "method1",
            TypeName.BOOLEAN,
            ImmutableList.of(),
            ImmutableList.copyOf(params1),
            null,
            null);
    mMethodModel2 =
        new SpecMethodModel<DelegateMethod, Void>(
            ImmutableList.<Annotation>of(),
            ImmutableList.<Modifier>of(),
            "method2",
            TypeName.BOOLEAN,
            ImmutableList.of(),
            ImmutableList.copyOf(params2),
            null,
            null);

    mTypeVariableNames.add(mTypeVariableName1);
    mTypeVariableNames.add(mTypeVariableName2);
  }

  @Test
  public void testCreateSpecModelImplWithoutDependencyInjection() {
    SpecModel specModel =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
            .delegateMethods(ImmutableList.of(mMethodModel1, mMethodModel2))
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

    assertThat(specModel.getProps()).hasSize(4);
    assertThat(specModel.getProps())
        .contains(mPropModel1, mPropModel2, mPropModel3, mUnderlyingPropModel2);

    assertThat(specModel.getPropDefaults()).hasSize(1);
    assertThat(specModel.getPropDefaults()).contains(mPropDefaultModel1);

    assertThat(specModel.getTypeVariables()).hasSize(2);
    assertThat(specModel.getTypeVariables()).contains(mTypeVariableName1, mTypeVariableName2);

    assertThat(specModel.getTreeProps()).hasSize(1);
    assertThat(specModel.getTreeProps()).contains(mTreePropModel);

    assertThat(specModel.hasInjectedDependencies()).isFalse();
    assertThat(specModel.getDependencyInjectionHelper()).isNull();
  }

  @Test
  public void testCreateSpecModelImplWithDependencyInjection() {
    DependencyInjectionHelper diGenerator = mock(DependencyInjectionHelper.class);
    SpecModel specModel = SpecModelImpl.newBuilder()
        .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
        .delegateMethods(ImmutableList.of(mMethodModel1, mMethodModel2))
        .typeVariables(ImmutableList.copyOf(mTypeVariableNames))
        .propDefaults(ImmutableList.of(mPropDefaultModel1))
        .dependencyInjectionGenerator(diGenerator)
        .representedObject(new Object())
        .build();

    assertThat(specModel.hasInjectedDependencies()).isTrue();
    assertThat(specModel.getDependencyInjectionHelper()).isSameAs(diGenerator);
  }
}
