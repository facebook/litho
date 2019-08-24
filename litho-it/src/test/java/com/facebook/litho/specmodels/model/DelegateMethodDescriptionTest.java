/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link DelegateMethodDescription} */
@RunWith(JUnit4.class)
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

    DelegateMethodDescription delegateMethodDescription =
        DelegateMethodDescription.newBuilder()
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
