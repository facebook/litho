/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.specmodels.generator;

import javax.lang.model.element.Modifier;

import java.lang.annotation.Annotation;
import java.util.ArrayList;

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.components.specmodels.model.ClassNames;
import com.facebook.components.specmodels.model.DelegateMethodModel;
import com.facebook.components.specmodels.model.MethodParamModelFactory;
import com.facebook.components.specmodels.model.SpecModel;
import com.facebook.components.specmodels.model.TreePropModel;
import com.facebook.testing.robolectric.v3.WithTestDefaultsRunner;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link TreePropGenerator}
 */
@RunWith(WithTestDefaultsRunner.class)
public class TreePropGeneratorTest {
  private SpecModel mSpecModel = mock(SpecModel.class);
  private TreePropModel mTreeProp = mock(TreePropModel.class);
  private DelegateMethodModel mOnCreateTreePropMethodModel;

  @Before
  public void setUp() {
    mOnCreateTreePropMethodModel = new DelegateMethodModel(
        ImmutableList.<Annotation>of(new OnCreateTreeProp() {

          @Override
          public Class<? extends Annotation> annotationType() {
            return OnCreateTreeProp.class;
          }

        }),
        ImmutableList.of(Modifier.PROTECTED),
        "onCreateTreeProp",
        TypeName.BOOLEAN,
        ImmutableList.of(
            MethodParamModelFactory.create(
                ClassNames.COMPONENT_CONTEXT,
                "componentContext",
                new ArrayList<Annotation>(),
                new ArrayList<AnnotationSpec>(),
                null),
            MethodParamModelFactory.create(
                TypeName.BOXED_BOOLEAN,
                "prop",
                ImmutableList.of(createAnnotation(Prop.class)),
                new ArrayList<AnnotationSpec>(),
                null),
            MethodParamModelFactory.create(
                TypeName.BOXED_INT,
                "state",
                ImmutableList.of(createAnnotation(State.class)),
                new ArrayList<AnnotationSpec>(),
                null)),
        null);

    when(mTreeProp.getName()).thenReturn("treeProp");
    when(mTreeProp.getType()).thenReturn(TypeName.INT);

    when(mSpecModel.getContextClass()).thenReturn(ClassNames.COMPONENT_CONTEXT);
    when(mSpecModel.getComponentClass()).thenReturn(ClassNames.COMPONENT);
    when(mSpecModel.getComponentName()).thenReturn("Test");
    when(mSpecModel.getDelegateMethods())
        .thenReturn(ImmutableList.of(mOnCreateTreePropMethodModel));
    when(mSpecModel.getTreeProps()).thenReturn(ImmutableList.of(mTreeProp));
  }

  @Test
  public void testGenerate() {
    TypeSpecDataHolder typeSpecDataHolder =
        TreePropGenerator.generate(mSpecModel);

    assertThat(typeSpecDataHolder.getFieldSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getMethodSpecs()).hasSize(2);
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString()).isEqualTo(
        "@java.lang.Override\n" +
            "protected void populateTreeProps(com.facebook.components.Component _abstractImpl, com.facebook.components.TreeProps treeProps) {\n" +
            "  if (treeProps == null) {\n" +
            "    return;\n" +
            "  }\n" +
            "  final TestImpl _impl = (TestImpl) _abstractImpl;\n" +
