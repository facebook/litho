/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions;
import com.facebook.litho.specmodels.model.DelegateMethodModel;
import com.google.testing.compile.CompilationRule;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests {@link DelegateMethodExtractor}
 */
public class DelegateMethodExtractorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  static class TestClass {

    @OnCreateLayout
    public void testMethod(
        @Prop boolean testProp,
        @State int testState,
        @Param Object testPermittedAnnotation) {
      // Don't do anything.
    }

    @OnEvent(Object.class)
    public void ignored() {}

    @OnUpdateState
    public void alsoIgnored() {}
  }

  @Test
  public void testMethodExtraction() {
    Elements elements = mCompilationRule.getElements();
    TypeElement typeElement = elements.getTypeElement(TestClass.class.getCanonicalName());

    List<Class<? extends Annotation>> permittedParamAnnotations =
        new ArrayList<>();
    permittedParamAnnotations.add(Param.class);

    ImmutableList<DelegateMethodModel> delegateMethods =
        DelegateMethodExtractor.getDelegateMethods(
            typeElement,
            new ArrayList<>(DelegateMethodDescriptions.LAYOUT_SPEC_DELEGATE_METHODS_MAP.keySet()),
            permittedParamAnnotations,
            ImmutableList.<Class<? extends Annotation>>of());

    assertThat(delegateMethods).hasSize(1);

    DelegateMethodModel delegateMethod = delegateMethods.iterator().next();
    assertThat(delegateMethod.annotations).hasSize(1);

    assertThat(delegateMethod.modifiers).hasSize(1);
    assertThat(delegateMethod.modifiers).contains(Modifier.PUBLIC);

    assertThat(delegateMethod.name.toString()).isEqualTo("testMethod");

    assertThat(delegateMethod.returnType).isEqualTo(TypeName.VOID);

    assertThat(delegateMethod.methodParams).hasSize(3);

    assertThat(delegateMethod.methodParams.get(0).getName()).isEqualTo("testProp");
    assertThat(delegateMethod.methodParams.get(0).getType()).isEqualTo(TypeName.BOOLEAN);
    assertThat(delegateMethod.methodParams.get(0).getAnnotations()).hasSize(1);

    assertThat(delegateMethod.methodParams.get(1).getName()).isEqualTo("testState");
    assertThat(delegateMethod.methodParams.get(1).getType()).isEqualTo(TypeName.INT);
    assertThat(delegateMethod.methodParams.get(1).getAnnotations()).hasSize(1);

    assertThat(delegateMethod.methodParams.get(2).getName()).isEqualTo("testPermittedAnnotation");
    assertThat(delegateMethod.methodParams.get(2).getType())
        .isEqualTo(ClassName.bestGuess("java.lang.Object"));
    assertThat(delegateMethod.methodParams.get(2).getAnnotations()).hasSize(1);
  }
}
