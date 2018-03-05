/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.specmodels.model;

import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnError;
import com.facebook.litho.specmodels.generator.EventCaseGenerator;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import javax.lang.model.element.Modifier;

public final class ErrorEventHandlerGenerator {
  private ErrorEventHandlerGenerator() {}

  /**
   * Generate a spec method model which corresponds to a source error declaration like this:
   *
   * <pre><code>
   * {@literal @}OnEvent(ErrorEvent.class)
   *  static void __internalOnErrorEvent(ComponentContext c, @FromEvent Exception exception) {}
   * </code></pre>
   *
   * This is used to automatically generate this method stub when an <code>@OnError</code>
   * declaration is used.
   */
  public static SpecMethodModel<EventMethod, EventDeclarationModel>
      generateErrorEventHandlerDefinition() {
    return SpecMethodModel.<EventMethod, EventDeclarationModel>builder()
        .modifiers(ImmutableList.of(Modifier.STATIC))
        .name(EventCaseGenerator.INTERNAL_ON_ERROR_HANDLER_NAME)
        .returnTypeSpec(new TypeSpec(TypeName.VOID))
        .typeVariables(ImmutableList.of())
        .methodParams(
            ImmutableList.of(
                new SimpleMethodParamModel(
                    new TypeSpec(ClassNames.COMPONENT_CONTEXT),
                    "c",
                    ImmutableList.of(),
                    ImmutableList.of(),
                    null),
                new SimpleMethodParamModel(
                    new TypeSpec(ClassName.bestGuess("java.lang.Exception")),
                    "exception",
                    ImmutableList.of((Annotation) () -> FromEvent.class),
                    ImmutableList.of(),
                    null)))
        .typeModel(
            new EventDeclarationModel(
                ClassNames.ERROR_EVENT,
                TypeName.VOID,
                ImmutableList.of(
                    new EventDeclarationModel.FieldModel(
                        FieldSpec.builder(
                                ClassName.bestGuess("java.lang.Exception"),
                                "exception",
                                Modifier.PUBLIC)
                            .build(),
                        null)),
                null))
        .build();
  }

  /** Check whether the delegate methods contain an <code>@OnError</code> declaration. */
  public static boolean hasOnErrorDelegateMethod(
      ImmutableList<SpecMethodModel<DelegateMethod, Void>> delegateMethods) {
    return delegateMethods
        .stream()
        .flatMap(m -> m.annotations.stream())
        .anyMatch(ann -> ann.annotationType().equals(OnError.class));
  }
}
