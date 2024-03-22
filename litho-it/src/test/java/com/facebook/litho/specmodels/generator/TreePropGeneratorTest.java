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

package com.facebook.litho.specmodels.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.MethodParamModelFactory;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.StateParamModel;
import com.facebook.litho.specmodels.model.TreePropModel;
import com.facebook.litho.specmodels.model.TypeSpec;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link TreePropGenerator} */
@RunWith(JUnit4.class)
public class TreePropGeneratorTest {

  private final SpecModel mSpecModel = mock(SpecModel.class);
  private final SpecModel mGenericSpecModel = mock(SpecModel.class);
  private final TreePropModel mTreeProp = mock(TreePropModel.class);
  private final TreePropModel mGenericTreeProp = mock(TreePropModel.class);
  private SpecMethodModel<DelegateMethod, Void> mOnCreateTreePropMethodModel;
  private SpecMethodModel<DelegateMethod, Void> mGenericOnCreateTreePropMethodModel;

  @Before
  public void setUp() {
    mOnCreateTreePropMethodModel =
        SpecMethodModel.<DelegateMethod, Void>builder()
            .annotations(
                ImmutableList.of(
                    new OnCreateTreeProp() {

                      @Override
                      public Class<? extends Annotation> annotationType() {
                        return OnCreateTreeProp.class;
                      }
                    }))
            .modifiers(ImmutableList.of(Modifier.PROTECTED))
            .name("onCreateTreeProp")
            .returnTypeSpec(new TypeSpec(TypeName.BOOLEAN))
            .typeVariables(ImmutableList.<TypeVariableName>of())
            .methodParams(
                ImmutableList.of(
                    MethodParamModelFactory.create(
                        new TypeSpec(ClassNames.COMPONENT_CONTEXT),
                        "componentContext",
                        new ArrayList<Annotation>(),
                        new ArrayList<AnnotationSpec>(),
                        new ArrayList<Class<? extends Annotation>>(),
                        ImmutableList.of(),
                        true,
                        null),
                    MethodParamModelFactory.create(
                        new TypeSpec(TypeName.BOOLEAN),
                        "prop",
                        ImmutableList.of(createAnnotation(Prop.class)),
                        new ArrayList<AnnotationSpec>(),
                        new ArrayList<Class<? extends Annotation>>(),
                        ImmutableList.of(),
                        true,
                        null),
                    MethodParamModelFactory.create(
                        new TypeSpec(TypeName.INT),
                        "state",
                        ImmutableList.of(createAnnotation(State.class)),
                        new ArrayList<AnnotationSpec>(),
                        new ArrayList<Class<? extends Annotation>>(),
                        ImmutableList.of(),
                        true,
                        null)))
            .build();

    when(mTreeProp.getName()).thenReturn("treeProp");
    when(mTreeProp.getTypeName()).thenReturn(TypeName.INT);

    when(mSpecModel.getContextClass()).thenReturn(ClassNames.COMPONENT_CONTEXT);
    when(mSpecModel.getComponentClass()).thenReturn(ClassNames.COMPONENT);
    when(mSpecModel.getComponentName()).thenReturn("Test");
    when(mSpecModel.getSpecName()).thenReturn("TestSpec");
    when(mSpecModel.getDelegateMethods())
        .thenReturn(ImmutableList.of(mOnCreateTreePropMethodModel));
    when(mSpecModel.getTreeProps()).thenReturn(ImmutableList.of(mTreeProp));

    ImmutableList<StateParamModel> states =
        ImmutableList.of(
            new StateParamModel(
                MethodParamModelFactory.create(
                    new TypeSpec(TypeName.INT),
                    "state",
                    ImmutableList.of(createAnnotation(State.class)),
                    new ArrayList<AnnotationSpec>(),
                    new ArrayList<Class<? extends Annotation>>(),
                    ImmutableList.of(),
                    true,
                    null),
                false));
    when(mSpecModel.getStateValues()).thenReturn(states);

    mGenericOnCreateTreePropMethodModel =
        SpecMethodModel.<DelegateMethod, Void>builder()
            .annotations(
                ImmutableList.<Annotation>of(
                    new OnCreateTreeProp() {

                      @Override
                      public Class<? extends Annotation> annotationType() {
                        return OnCreateTreeProp.class;
                      }
                    }))
            .modifiers(ImmutableList.of(Modifier.PROTECTED))
            .name("onCreateTreeProp")
            .returnTypeSpec(
                new TypeSpec.DeclaredTypeSpec(
                    ClassName.get(GenericObject.class),
                    GenericObject.class.getName(),
                    () -> new TypeSpec(TypeName.OBJECT),
                    () -> ImmutableList.of(),
                    () -> ImmutableList.of(new TypeSpec(TypeName.INT))))
            .typeVariables(ImmutableList.<TypeVariableName>of())
            .methodParams(
                ImmutableList.of(
                    MethodParamModelFactory.create(
                        new TypeSpec(ClassNames.COMPONENT_CONTEXT),
                        "componentContext",
                        new ArrayList<Annotation>(),
                        new ArrayList<AnnotationSpec>(),
                        new ArrayList<Class<? extends Annotation>>(),
                        ImmutableList.of(),
                        true,
                        null),
                    MethodParamModelFactory.create(
                        new TypeSpec.DeclaredTypeSpec(
                            ClassName.get(GenericObject.class),
                            GenericObject.class.getName(),
                            () -> new TypeSpec(TypeName.OBJECT),
                            () -> ImmutableList.of(),
                            () -> ImmutableList.of(new TypeSpec(TypeName.INT))),
                        "prop",
                        ImmutableList.of(createAnnotation(Prop.class)),
                        new ArrayList<AnnotationSpec>(),
                        new ArrayList<Class<? extends Annotation>>(),
                        ImmutableList.of(),
                        true,
                        null)))
            .representedObject(null)
            .typeModel(null)
            .build();

    when(mGenericTreeProp.getName()).thenReturn("genericTreeProp");
    when(mGenericTreeProp.getTypeName())
        .thenReturn(ParameterizedTypeName.get(GenericObject.class, Boolean.class));

    when(mGenericSpecModel.getContextClass()).thenReturn(ClassNames.COMPONENT_CONTEXT);
    when(mGenericSpecModel.getComponentClass()).thenReturn(ClassNames.COMPONENT);
    when(mGenericSpecModel.getComponentName()).thenReturn("Test");
    when(mGenericSpecModel.getSpecName()).thenReturn("TestSpec");
    when(mGenericSpecModel.getDelegateMethods())
        .thenReturn(ImmutableList.of(mGenericOnCreateTreePropMethodModel));
    when(mGenericSpecModel.getTreeProps())
        .thenReturn(ImmutableList.of(mGenericTreeProp, mTreeProp));
  }

  @Test
  public void testGenerate() {
    TypeSpecDataHolder typeSpecDataHolder =
        TreePropGenerator.generate(mSpecModel, RunMode.normal());

    assertThat(typeSpecDataHolder.getFieldSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getMethodSpecs()).hasSize(2);
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected void populateTreePropContainer(com.facebook.litho.TreePropContainer"
                + " treePropContainer) {\n"
                + "  if (treePropContainer == null) {\n"
                + "    return;\n"
                + "  }\n"
                + "  treeProp = treePropContainer.get(int.class);\n"
                + "}\n");

    assertThat(typeSpecDataHolder.getMethodSpecs().get(1).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected com.facebook.litho.TreePropContainer"
                + " getTreePropContainerForChildren(\n"
                + "    com.facebook.litho.ComponentContext c,\n"
                + "    com.facebook.litho.TreePropContainer parentTreePropContainer) {\n"
                + "  final com.facebook.litho.TreePropContainer childTreePropContainer ="
                + " com.facebook.litho.TreePropContainer.acquire(parentTreePropContainer);\n"
                + "  TestStateContainer _state = getStateContainerImpl(c);\n"
                + "  childTreePropContainer.put(boolean.class, TestSpec.onCreateTreeProp(\n"
                + "      (com.facebook.litho.ComponentContext) c,\n"
                + "      prop,\n"
                + "      _state.state));\n"
                + "  return childTreePropContainer;\n"
                + "}\n");
  }

  @Test
  public void testGenericGenerate() {
    TypeSpecDataHolder typeSpecDataHolder =
        TreePropGenerator.generate(mGenericSpecModel, RunMode.normal());

    assertThat(typeSpecDataHolder.getFieldSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getMethodSpecs()).hasSize(2);
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected void populateTreePropContainer(com.facebook.litho.TreePropContainer"
                + " treePropContainer) {\n"
                + "  if (treePropContainer == null) {\n"
                + "    return;\n"
                + "  }\n"
                + "  genericTreeProp ="
                + " treePropContainer.get(com.facebook.litho.specmodels.generator.TreePropGeneratorTest.GenericObject.class);\n"
                + "  treeProp = treePropContainer.get(int.class);\n"
                + "}\n");

    assertThat(typeSpecDataHolder.getMethodSpecs().get(1).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected com.facebook.litho.TreePropContainer"
                + " getTreePropContainerForChildren(\n"
                + "    com.facebook.litho.ComponentContext c,\n"
                + "    com.facebook.litho.TreePropContainer parentTreePropContainer) {\n"
                + "  final com.facebook.litho.TreePropContainer childTreePropContainer ="
                + " com.facebook.litho.TreePropContainer.acquire(parentTreePropContainer);\n"
                + "  childTreePropContainer.put(com.facebook.litho.specmodels.generator.TreePropGeneratorTest.GenericObject.class,"
                + " TestSpec.onCreateTreeProp(\n"
                + "      (com.facebook.litho.ComponentContext) c,\n"
                + "      prop));\n"
                + "  return childTreePropContainer;\n"
                + "}\n");
  }

  private static Annotation createAnnotation(final Class<? extends Annotation> annotationClass) {
    return new Annotation() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return annotationClass;
      }
    };
  }

  private static class GenericObject<T> {

    T value;
  }
}
