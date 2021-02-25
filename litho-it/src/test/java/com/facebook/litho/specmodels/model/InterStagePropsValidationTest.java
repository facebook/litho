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
import static org.mockito.Mockito.when;

import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.internal.ImmutableList;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class InterStagePropsValidationTest {

  private final SpecModel mSpecModel = mock(SpecModel.class);

  @Test
  public void testDuplicateParamName() {
    final String duplicateName = "testParam";

    InterStageInputParamModel paramModel = mock(InterStageInputParamModel.class);
    when(paramModel.getName()).thenReturn(duplicateName);
    when(paramModel.getAnnotations())
        .thenReturn(ImmutableList.of((Annotation) () -> FromPrepare.class));

    StateParamModel stateParam = mock(StateParamModel.class);
    when(stateParam.getName()).thenReturn(duplicateName);

    when(mSpecModel.getInterStageInputs()).thenReturn(ImmutableList.of(paramModel));
    when(mSpecModel.getStateValues()).thenReturn(ImmutableList.of(stateParam));

    List<SpecModelValidationError> validationErrors = new ArrayList<>();
    DelegateMethodValidation.validateDuplicateName(
        duplicateName,
        mSpecModel.getStateValues(),
        FromPrepare.class,
        State.class,
        validationErrors);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "The parameter name of @State \""
                + duplicateName
                + "\" and @FromPrepare \""
                + duplicateName
                + "\" collide!");
  }
}
