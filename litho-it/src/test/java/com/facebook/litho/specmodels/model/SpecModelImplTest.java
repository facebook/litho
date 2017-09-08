/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import static com.facebook.litho.specmodels.internal.ImmutableList.copyOf;
import static com.facebook.litho.specmodels.internal.ImmutableList.of;
import static com.facebook.litho.specmodels.model.SpecModelImpl.newBuilder;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.specmodels.internal.ImmutableList;
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

  PropModel mPropModel1 = mock(PropModel.class);
  PropModel mPropModel2 = mock(PropModel.class);
  PropModel mPropModel3 = mock(PropModel.class);
  PropModel mUnderlyingPropModel1 = mock(PropModel.class);
  PropModel mUnderlyingPropModel2 = mock(PropModel.class);
  SimpleMethodParamModel mMethodParamModel = mock(SimpleMethodParamModel.class);
  TreePropModel mTreePropModel = mock(TreePropModel.class);

  DelegateMethodModel mMethodModel1;
  DelegateMethodModel mMethodModel2;

  TypeVariableName mTypeVariableName1 = TypeVariableName.get("test1");
  TypeVariableName mTypeVariableName2 = TypeVariableName.get("test2");
  List<TypeVariableName> mTypeVariableNames = new ArrayList<>(2);

  PropDefaultModel mPropDefaultModel1 = mock(PropDefaultModel.class);

  @Before
  public void setUp() {
    when(mPropModel1.getName()).thenReturn("propModel1");
    when(mPropModel2.getName()).thenReturn("propModel2");
    when(mPropModel3.getName()).thenReturn("propModel3");
    when(mPropModel1.getType()).thenReturn(TypeName.BOOLEAN);
    when(mPropModel2.getType()).thenReturn(TypeName.INT.box());
    when(mPropModel3.getType()).thenReturn(TypeName.LONG);
    when(mPropModel1.getVarArgsSingleName()).thenReturn("");
    when(mPropModel2.getVarArgsSingleName()).thenReturn("");
    when(mPropModel3.getVarArgsSingleName()).thenReturn("");

    when(mUnderlyingPropModel1.getName()).thenReturn("propModel1");
    when(mUnderlyingPropModel1.getType()).thenReturn(TypeName.BOOLEAN.box());
    when(mUnderlyingPropModel1.getVarArgsSingleName()).thenReturn("");
    when(mUnderlyingPropModel2.getName()).thenReturn("differentName");

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
        new DelegateMethodModel(
            ImmutableList.<Annotation>of(),
            ImmutableList.<Modifier>of(),
            "method1",
            TypeName.BOOLEAN,
            ImmutableList.copyOf(params1),
            null);
    mMethodModel2 =
        new DelegateMethodModel(
            ImmutableList.<Annotation>of(),
            ImmutableList.<Modifier>of(),
            "method2",
            TypeName.BOOLEAN,
            ImmutableList.copyOf(params2),
            null);

    mTypeVariableNames.add(mTypeVariableName1);
    mTypeVariableNames.add(mTypeVariableName2);
  }

  @Test
  public void testCreateSpecModelImplWithoutDependencyInjection() {
    SpecModel specModel = newBuilder()
        .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
        .delegateMethods(of(mMethodModel1, mMethodModel2))
        .typeVariables(copyOf(mTypeVariableNames))
        .propDefaults(of(mPropDefaultModel1))
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
