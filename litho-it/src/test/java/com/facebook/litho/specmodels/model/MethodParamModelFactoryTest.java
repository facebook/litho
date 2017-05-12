/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.internal.ImmutableList;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link MethodParamModelFactory}
 */
public class MethodParamModelFactoryTest {

  @Before
  public void ListUp() {
  }

  @Test
  public void testCreateSimpleMethodParamModel() {
    MethodParamModel methodParamModel = MethodParamModelFactory.create(
        TypeName.BOOLEAN,
        "testParam",
        new ArrayList<Annotation>(),
        new ArrayList<AnnotationSpec>(),
        ImmutableList.<Class<? extends Annotation>>of(),
        null);

    assertThat(methodParamModel).isInstanceOf(SimpleMethodParamModel.class);
  }

  @Test
  public void testCreatePropModel() {
    final List<Annotation> annotations = new ArrayList<>();
    annotations.add(mock(Prop.class));
    MethodParamModel methodParamModel = MethodParamModelFactory.create(
        TypeName.BOOLEAN,
        "testParam",
        annotations,
        new ArrayList<AnnotationSpec>(),
        ImmutableList.<Class<? extends Annotation>>of(),
        null);

    assertThat(methodParamModel).isInstanceOf(PropModel.class);
  }

  @Test
  public void testCreateStateModel() {
    final List<Annotation> annotations = new ArrayList<>();
    annotations.add(mock(State.class));
    MethodParamModel methodParamModel = MethodParamModelFactory.create(
        TypeName.BOOLEAN,
        "testParam",
        annotations,
        new ArrayList<AnnotationSpec>(),
        ImmutableList.<Class<? extends Annotation>>of(),
        null);

    assertThat(methodParamModel).isInstanceOf(StateParamModel.class);
  }

  @Test
  public void testCreateInterStageInputModel() {
    final List<Annotation> annotations = new ArrayList<>();
    Annotation fromPrepare= new Annotation() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return FromPrepare.class;
      }
    };
    annotations.add(fromPrepare);
    MethodParamModel methodParamModel = MethodParamModelFactory.create(
        TypeName.BOOLEAN,
        "testParam",
        annotations,
        new ArrayList<AnnotationSpec>(),
        ImmutableList.<Class<? extends Annotation>>of(FromPrepare.class),
        null);

    assertThat(methodParamModel).isInstanceOf(InterStageInputParamModel.class);
  }
}
