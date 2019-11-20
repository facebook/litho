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

package com.facebook.litho.specmodels.model;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link MethodParamModelFactory} */
@RunWith(JUnit4.class)
public class MethodParamModelFactoryTest {

  private TypeSpec mDiffTypeSpecWrappingInt;

  @Before
  public void ListUp() {
    mDiffTypeSpecWrappingInt =
        new TypeSpec.DeclaredTypeSpec(
            ClassNames.DIFF,
            ClassNames.DIFF.packageName() + "." + ClassNames.DIFF.simpleName(),
            () -> new TypeSpec(TypeName.OBJECT),
            () -> ImmutableList.of(),
            () -> ImmutableList.of(new TypeSpec(TypeName.INT.box())));
  }

  @Test
  public void testCreateSimpleMethodParamModel() {
    MethodParamModel methodParamModel =
        MethodParamModelFactory.create(
            new TypeSpec(TypeName.BOOLEAN),
            "testParam",
            new ArrayList<Annotation>(),
            new ArrayList<AnnotationSpec>(),
            ImmutableList.<Class<? extends Annotation>>of(),
            true,
            null);

    assertThat(methodParamModel).isInstanceOf(SimpleMethodParamModel.class);
  }

  @Test
  public void testCreatePropModel() {
    final List<Annotation> annotations = new ArrayList<>();
    annotations.add(mock(Prop.class));
    MethodParamModel methodParamModel =
        MethodParamModelFactory.create(
            new TypeSpec(TypeName.BOOLEAN),
            "testParam",
            annotations,
            new ArrayList<AnnotationSpec>(),
            ImmutableList.<Class<? extends Annotation>>of(),
            true,
            null);

    assertThat(methodParamModel).isInstanceOf(PropModel.class);
  }

  @Test
  public void testCreateStateModel() {
    final List<Annotation> annotations = new ArrayList<>();
    annotations.add(mock(State.class));
    MethodParamModel methodParamModel =
        MethodParamModelFactory.create(
            new TypeSpec(TypeName.BOOLEAN),
            "testParam",
            annotations,
            new ArrayList<AnnotationSpec>(),
            ImmutableList.<Class<? extends Annotation>>of(),
            true,
            null);

    assertThat(methodParamModel).isInstanceOf(StateParamModel.class);
  }

  @Test
  public void testCreateInterStageInputModel() {
    final List<Annotation> annotations = new ArrayList<>();
    Annotation fromPrepare =
        new Annotation() {
          @Override
          public Class<? extends Annotation> annotationType() {
            return FromPrepare.class;
          }
        };
    annotations.add(fromPrepare);
    MethodParamModel methodParamModel =
        MethodParamModelFactory.create(
            new TypeSpec(TypeName.BOOLEAN),
            "testParam",
            annotations,
            new ArrayList<AnnotationSpec>(),
            ImmutableList.<Class<? extends Annotation>>of(FromPrepare.class),
            true,
            null);

    assertThat(methodParamModel).isInstanceOf(InterStageInputParamModel.class);
  }

  @Test
  public void testCreateDiffModel() {
    final List<Annotation> annotations = new ArrayList<>();
    Annotation annotation =
        new Annotation() {
          @Override
          public Class<? extends Annotation> annotationType() {
            return OnCreateTransition.class;
          }
        };
    annotations.add(annotation);

    MethodParamModel methodParamModel =
        MethodParamModelFactory.create(
            mDiffTypeSpecWrappingInt,
            "testParam",
            annotations,
            new ArrayList<AnnotationSpec>(),
            ImmutableList.<Class<? extends Annotation>>of(),
            true,
            null);

    assertThat(methodParamModel).isInstanceOf(RenderDataDiffModel.class);
  }

  @Test
  public void testDontCreateDiffForShouldUpdate() {
    final List<Annotation> annotations = new ArrayList<>();
    Annotation annotation =
        new Annotation() {
          @Override
          public Class<? extends Annotation> annotationType() {
            return ShouldUpdate.class;
          }
        };
    annotations.add(annotation);

    MethodParamModel methodParamModel =
        MethodParamModelFactory.create(
            mDiffTypeSpecWrappingInt,
            "testParam",
            annotations,
            new ArrayList<AnnotationSpec>(),
            ImmutableList.<Class<? extends Annotation>>of(),
            false,
            null);

    assertThat(methodParamModel).isNotInstanceOf(RenderDataDiffModel.class);
  }

  @Test
  public void testCreateSimpleMethodParamModelWithSpecificType() {
    Object representedObject = new Object();
    SimpleMethodParamModel param =
        MethodParamModelFactory.createSimpleMethodParamModel(
            new TypeSpec(TypeName.CHAR), "customParamModel", representedObject);

    assertThat(param.getTypeName()).isEqualTo(TypeName.CHAR);
    assertThat(param.getName()).isEqualTo("customParamModel");
    assertThat(param.getRepresentedObject()).isEqualTo(representedObject);
    assertThat(param.getAnnotations()).isEmpty();
    assertThat(param.getExternalAnnotations()).isEmpty();
  }
}
