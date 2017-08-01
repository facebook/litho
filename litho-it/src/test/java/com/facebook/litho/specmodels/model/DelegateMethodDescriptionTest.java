/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.PROP;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.STATE;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import javax.lang.model.element.Modifier;
import org.junit.Test;

/**
 * Tests {@link DelegateMethodDescription}
 */
public class DelegateMethodDescriptionTest {

  @Test
  public void testBuilder() {
    ImmutableList<AnnotationSpec> annotations =
        ImmutableList.of(AnnotationSpec.builder(Override.class).build());
    Modifier accessType = Modifier.PRIVATE;
    TypeName returnType = ClassName.bestGuess("ClassName");
    String name = "test";
    ImmutableList<TypeName> parameterTypes =
        ImmutableList.<TypeName>of(ClassName.bestGuess("ParameterType"));
    ImmutableList<TypeName> exceptions =
        ImmutableList.<TypeName>of(ClassName.bestGuess("ExceptionType"));
    ImmutableList<OptionalParameterType> optionalParameterTypes = ImmutableList.of(PROP, STATE);

    DelegateMethodDescription delegateMethodDescription = DelegateMethodDescription.newBuilder()
        .annotations(annotations)
        .accessType(accessType)
        .returnType(returnType)
        .name(name)
        .definedParameterTypes(parameterTypes)
        .optionalParameterTypes(optionalParameterTypes)
        .exceptions(exceptions)
        .build();

    assertThat(delegateMethodDescription.accessType).isEqualTo(accessType);
    assertThat(delegateMethodDescription.returnType).isEqualTo(returnType);
    assertThat(delegateMethodDescription.name).isEqualTo(name);
    assertThat(delegateMethodDescription.annotations).isSameAs(annotations);
    assertThat(delegateMethodDescription.definedParameterTypes).isSameAs(parameterTypes);
    assertThat(delegateMethodDescription.optionalParameterTypes).isSameAs(optionalParameterTypes);
    assertThat(delegateMethodDescription.exceptions).isSameAs(exceptions);
  }
}
