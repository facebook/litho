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

package com.facebook.litho.specmodels.generator;

import static com.facebook.litho.specmodels.generator.DelegateMethodGenerator.generateDelegates;
import static com.facebook.litho.specmodels.model.DelegateMethodDescriptions.LAYOUT_SPEC_DELEGATE_METHODS_MAP;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.DelegateMethodDescription;
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.MethodParamModelFactory;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelImpl;
import com.facebook.litho.specmodels.model.TypeSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link DelegateMethodGenerator} */
@RunWith(JUnit4.class)
public class DelegateMethodGeneratorTest {
  private static final String TEST_QUALIFIED_SPEC_NAME = "com.facebook.litho.TestSpec";

  private final DependencyInjectionHelper mDependencyInjectionHelper =
      mock(DependencyInjectionHelper.class);

  private SpecModel mSpecModelWithoutDI;
  private SpecModel mSpecModelWithDI;

  private SpecMethodModel<DelegateMethod, Void> mDelegateMethodModel;

  @Before
  public void setUp() {
    mDelegateMethodModel =
        SpecMethodModel.<DelegateMethod, Void>builder()
            .annotations(ImmutableList.of(createAnnotation(OnCreateLayout.class)))
            .modifiers(ImmutableList.of(Modifier.PROTECTED))
            .name("onCreateLayout")
            .returnTypeSpec(new TypeSpec(DelegateMethodDescriptions.ON_CREATE_LAYOUT.returnType))
            .typeVariables(ImmutableList.of())
            .methodParams(
                ImmutableList.of(
                    MethodParamModelFactory.create(
                        new TypeSpec(ClassNames.COMPONENT_CONTEXT),
                        "c",
                        ImmutableList.of(),
                        new ArrayList<>(),
                        ImmutableList.of(),
                        true,
                        null),
                    MethodParamModelFactory.create(
                        new TypeSpec(TypeName.BOOLEAN),
                        "prop",
                        ImmutableList.of(createAnnotation(Prop.class)),
                        new ArrayList<>(),
                        ImmutableList.of(),
                        true,
                        null),
                    MethodParamModelFactory.create(
                        new TypeSpec(TypeName.INT),
                        "state",
                        ImmutableList.of(createAnnotation(State.class)),
                        new ArrayList<>(),
                        ImmutableList.of(),
                        true,
                        null)))
            .representedObject(null)
            .typeModel(null)
            .build();

    mSpecModelWithoutDI =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
            .componentClass(ClassNames.COMPONENT)
            .contextClassName(ClassNames.COMPONENT_CONTEXT)
            .delegateMethods(ImmutableList.of(mDelegateMethodModel))
            .representedObject(new Object())
            .build();

    mSpecModelWithDI =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
            .componentClass(ClassNames.COMPONENT)
            .contextClassName(ClassNames.COMPONENT_CONTEXT)
            .delegateMethods(ImmutableList.of(mDelegateMethodModel))
            .dependencyInjectionHelper(mDependencyInjectionHelper)
            .representedObject(new Object())
            .build();
  }

  @Test
  public void testGenerateWithoutDependencyInjection() {
    TypeSpecDataHolder typeSpecDataHolder =
        generateDelegates(mSpecModelWithoutDI, LAYOUT_SPEC_DELEGATE_METHODS_MAP, RunMode.normal());

    assertThat(typeSpecDataHolder.getFieldSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getMethodSpecs()).hasSize(1);
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected com.facebook.litho.Component onCreateLayout(com.facebook.litho.ComponentContext c) {\n"
                + "  com.facebook.litho.Component _result;\n"
                + "  _result = (com.facebook.litho.Component) TestSpec.onCreateLayout(\n"
                + "    (com.facebook.litho.ComponentContext) c,\n"
                + "    (boolean) prop,\n"
                + "    (int) state);\n"
                + "  return _result;\n"
                + "}\n");
  }

  @Test
  public void testExtraOptionalParameterIncludedIfSpecMethodUsesIt() throws Exception {
    Map<Class<? extends Annotation>, DelegateMethodDescription> map =
        new TreeMap<>(
            new Comparator<Class<? extends Annotation>>() {
              @Override
              public int compare(Class<? extends Annotation> lhs, Class<? extends Annotation> rhs) {
                return lhs.toString().compareTo(rhs.toString());
              }
            });
    map.put(
        OnCreateLayout.class,
        DelegateMethodDescription.fromDelegateMethodDescription(
                LAYOUT_SPEC_DELEGATE_METHODS_MAP.get(OnCreateLayout.class))
            .optionalParameters(
                ImmutableList.of(
                    MethodParamModelFactory.createSimpleMethodParamModel(
                        new TypeSpec(TypeName.CHAR), "optionalParam", new Object())))
            .build());

    SpecMethodModel<DelegateMethod, Void> delegateMethodExpectingOptionalParameter =
        SpecMethodModel.<DelegateMethod, Void>builder()
            .annotations(ImmutableList.of(createAnnotation(OnCreateLayout.class)))
            .modifiers(ImmutableList.of(Modifier.PROTECTED))
            .name("onCreateLayout")
            .returnTypeSpec(new TypeSpec(DelegateMethodDescriptions.ON_CREATE_LAYOUT.returnType))
            .typeVariables(ImmutableList.of())
            .methodParams(
                ImmutableList.of(
                    MethodParamModelFactory.create(
                        new TypeSpec(ClassNames.COMPONENT_CONTEXT),
                        "c",
                        ImmutableList.of(),
                        new ArrayList<>(),
                        ImmutableList.of(),
                        true,
                        null),
                    MethodParamModelFactory.createSimpleMethodParamModel(
                        new TypeSpec(TypeName.CHAR), "unimportantName", new Object()),
                    MethodParamModelFactory.create(
                        new TypeSpec(TypeName.BOOLEAN),
                        "prop",
                        ImmutableList.of(createAnnotation(Prop.class)),
                        new ArrayList<>(),
                        ImmutableList.of(),
                        true,
                        null)))
            .representedObject(null)
            .typeModel(null)
            .build();

    SpecModel specModel =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
            .componentClass(ClassNames.COMPONENT)
            .delegateMethods(ImmutableList.of(delegateMethodExpectingOptionalParameter))
            .representedObject(new Object())
            .build();

    TypeSpecDataHolder typeSpecDataHolder = generateDelegates(specModel, map, RunMode.normal());

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected com.facebook.litho.Component onCreateLayout(com.facebook.litho.ComponentContext c) {\n"
                + "  com.facebook.litho.Component _result;\n"
                + "  _result = (com.facebook.litho.Component) TestSpec.onCreateLayout(\n"
                + "    (com.facebook.litho.ComponentContext) c,\n"
                + "    (char) optionalParam,\n"
                + "    (boolean) prop);\n"
                + "  return _result;\n"
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
}
