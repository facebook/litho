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

import com.facebook.litho.annotations.Event;
import com.facebook.litho.annotations.FromTrigger;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.SpecMethodModel;
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

/** Tests {@link TriggerMethodExtractor} */
public class TriggerMethodExtractorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  static class TestClass {

    @OnCreateLayout
    public void ignored() {}

    @OnTrigger(TestEvent.class)
    public void testMethod(
        @Prop boolean testProp,
        @State int testState,
        @Param Object testPermittedAnnotation,
        @FromTrigger long arg4) {
      // Don't do anything.
    }

    @OnUpdateState
    public void alsoIgnored() {}
  }

  @Event(returnType = Object.class)
  static class TestEvent {
    public long arg4;
  }

  @Test
  public void testMethodExtraction() {
    Elements elements = mCompilationRule.getElements();
    TypeElement typeElement = elements.getTypeElement(TestClass.class.getCanonicalName());

    List<Class<? extends Annotation>> permittedParamAnnotations = new ArrayList<>();

    ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> methods =
        TriggerMethodExtractor.getOnTriggerMethods(
            elements, typeElement, permittedParamAnnotations);

    assertThat(methods).hasSize(1);

    SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod = methods.iterator().next();
    assertThat(eventMethod.typeModel.name)
        .isEqualTo(
            ClassName.bestGuess(
                "com.facebook.litho.specmodels.processor.TriggerMethodExtractorTest.TestEvent"));

    assertThat(eventMethod.modifiers).hasSize(1);
    assertThat(eventMethod.modifiers).contains(Modifier.PUBLIC);

    assertThat(eventMethod.name.toString()).isEqualTo("testMethod");

    assertThat(eventMethod.returnType).isEqualTo(TypeName.VOID);

    assertThat(eventMethod.methodParams).hasSize(4);

    assertThat(eventMethod.methodParams.get(0).getName()).isEqualTo("testProp");
    assertThat(eventMethod.methodParams.get(0).getTypeName()).isEqualTo(TypeName.BOOLEAN);
    assertThat(eventMethod.methodParams.get(0).getAnnotations()).hasSize(1);

    assertThat(eventMethod.methodParams.get(1).getName()).isEqualTo("testState");
    assertThat(eventMethod.methodParams.get(1).getTypeName()).isEqualTo(TypeName.INT);
    assertThat(eventMethod.methodParams.get(1).getAnnotations()).hasSize(1);

    assertThat(eventMethod.methodParams.get(2).getName()).isEqualTo("testPermittedAnnotation");
    assertThat(eventMethod.methodParams.get(2).getTypeName())
        .isEqualTo(ClassName.bestGuess("java.lang.Object"));
    assertThat(eventMethod.methodParams.get(2).getAnnotations()).hasSize(1);

    assertThat(eventMethod.methodParams.get(3).getName()).isEqualTo("arg4");
    assertThat(eventMethod.methodParams.get(3).getTypeName()).isEqualTo(TypeName.LONG);
    assertThat(eventMethod.methodParams.get(3).getAnnotations()).hasSize(1);
  }
}
