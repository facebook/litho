/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.litho.specmodels.generator;

import static com.facebook.litho.specmodels.generator.DelegateMethodGenerator.generateDelegates;
import static com.facebook.litho.specmodels.model.DelegateMethodDescriptions.LAYOUT_SPEC_DELEGATE_METHODS_MAP;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions;
import com.facebook.litho.specmodels.model.DelegateMethodModel;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.MethodParamModelFactory;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelImpl;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link PreambleGenerator}
 */
public class DelegateMethodGeneratorTest {
  private static final String TEST_QUALIFIED_SPEC_NAME = "com.facebook.litho.TestSpec";

  private final DependencyInjectionHelper mDependencyInjectionHelper =
      mock(DependencyInjectionHelper.class);

  private SpecModel mSpecModelWithoutDI;
  private SpecModel mSpecModelWithDI;

  private DelegateMethodModel mDelegateMethodModel;

  @Before
  public void setUp() {
    mDelegateMethodModel = new DelegateMethodModel(
        ImmutableList.of(createAnnotation(OnCreateLayout.class)),
        ImmutableList.of(Modifier.PROTECTED),
        "onCreateLayout",
        DelegateMethodDescriptions.ON_CREATE_LAYOUT.returnType,
        ImmutableList.of(
            MethodParamModelFactory.create(
                mock(ExecutableElement.class),
                ClassNames.COMPONENT_CONTEXT,
                "c",
                ImmutableList.<Annotation>of(),
                new ArrayList<AnnotationSpec>(),
                ImmutableList.<Class<? extends Annotation>>of(),
                null),
            MethodParamModelFactory.create(
                mock(ExecutableElement.class),
                TypeName.BOOLEAN,
                "prop",
                ImmutableList.of(createAnnotation(Prop.class)),
                new ArrayList<AnnotationSpec>(),
                ImmutableList.<Class<? extends Annotation>>of(),
                null),
            MethodParamModelFactory.create(
                mock(ExecutableElement.class),
                TypeName.INT,
                "state",
                ImmutableList.of(createAnnotation(State.class)),
                new ArrayList<AnnotationSpec>(),
                ImmutableList.<Class<? extends Annotation>>of(),
                null)),
        null);

    mSpecModelWithoutDI = SpecModelImpl.newBuilder()
        .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
        .delegateMethods(ImmutableList.of(mDelegateMethodModel))
        .representedObject(new Object())
        .build();

    mSpecModelWithDI = SpecModelImpl.newBuilder()
        .qualifiedSpecClassName(TEST_QUALIFIED_SPEC_NAME)
        .delegateMethods(ImmutableList.of(mDelegateMethodModel))
        .dependencyInjectionGenerator(mDependencyInjectionHelper)
        .representedObject(new Object())
        .build();
  }

  @Test
  public void testGenerateWithoutDependencyInjection() {
    TypeSpecDataHolder typeSpecDataHolder =
        generateDelegates(
            mSpecModelWithoutDI,
            LAYOUT_SPEC_DELEGATE_METHODS_MAP);

    assertThat(typeSpecDataHolder.getFieldSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getMethodSpecs()).hasSize(1);
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString()).isEqualTo(
        "@java.lang.Override\n" +
            "protected com.facebook.litho.ComponentLayout onCreateLayout(com.facebook.litho.ComponentContext c,\n" +
            "    com.facebook.litho.Component _abstractImpl) {\n" +
            "  TestImpl _impl = (TestImpl) _abstractImpl;\n" +
            "  com.facebook.litho.ComponentLayout _result = (com.facebook.litho.ComponentLayout) TestSpec.onCreateLayout(\n" +
            "    (com.facebook.litho.ComponentContext) c,\n" +
            "    (boolean) _impl.prop,\n" +
            "    (int) _impl.state);\n" +
            "  return _result;\n" +
            "}\n");
  }

  @Test
  public void testGenerateWithDependencyInjection() {
    when(mDependencyInjectionHelper.getSourceDelegateAccessorMethod(mSpecModelWithDI))
        .thenReturn(".accessorMethod()");
    TypeSpecDataHolder typeSpecDataHolder =
        generateDelegates(
            mSpecModelWithDI,
            LAYOUT_SPEC_DELEGATE_METHODS_MAP);

    assertThat(typeSpecDataHolder.getFieldSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getMethodSpecs()).hasSize(1);
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString()).isEqualTo(
        "@java.lang.Override\n" +
            "protected com.facebook.litho.ComponentLayout onCreateLayout(com.facebook.litho.ComponentContext c,\n" +
            "    com.facebook.litho.Component _abstractImpl) {\n" +
            "  TestImpl _impl = (TestImpl) _abstractImpl;\n" +
            "  com.facebook.litho.ComponentLayout _result = (com.facebook.litho.ComponentLayout) mSpec.accessorMethod().onCreateLayout(\n" +
            "    (com.facebook.litho.ComponentContext) c,\n" +
            "    (boolean) _impl.prop,\n" +
            "    (int) _impl.state);\n" +
            "  return _result;\n" +
            "}\n");
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
