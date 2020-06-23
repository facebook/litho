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

package com.facebook.litho.specmodels.processor;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.facebook.litho.annotations.ResType;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.InjectPropModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.testing.specmodels.MockMethodParamModel;
import com.facebook.litho.testing.specmodels.MockSpecModel;
import com.squareup.javapoet.ClassName;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests {@link PropNameInterStageStore} */
@RunWith(JUnit4.class)
public class PropNameInterStageStoreTest {
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  Filer mFiler;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testLoad() throws IOException {
    final PropNameInterStageStore store = new PropNameInterStageStore(mFiler);

    final FileObject fileObject = makeFileObjectForString("arg0\narg1\n");
    when(mFiler.getResource((JavaFileManager.Location) any(), anyString(), anyString()))
        .thenReturn(fileObject);

    final Optional<ImmutableList<String>> strings =
        store.loadNames(new MockName("com.example.MyComponentSpec"));
    assertThat(strings.isPresent()).isTrue();
    assertThat(strings.get()).containsExactly("arg0", "arg1");

    verify(mFiler)
        .getResource(
            StandardLocation.CLASS_PATH,
            "",
            "_STRIPPED_RESOURCES/litho/com.example.MyComponentSpec.props");
  }

  @Test
  public void testSave() throws IOException {
    final PropNameInterStageStore store = new PropNameInterStageStore(mFiler);

    final MockSpecModel specModel =
        MockSpecModel.newBuilder()
            .rawProps(ImmutableList.of(makePropModel("param0"), makePropModel("param1")))
            .rawInjectProps(ImmutableList.of(makeInjectPropModel("injectParam0")))
            .specTypeName(ClassName.get(MyTestSpec.class))
            .build();
    store.saveNames(specModel);

    verify(mFiler)
        .createResource(
            StandardLocation.CLASS_OUTPUT,
            "",
            "_STRIPPED_RESOURCES/litho/com.facebook.litho.specmodels.processor.PropNameInterStageStoreTest.MyTestSpec.props");

    // Not checking the actually written values here because Java IO is a horrible mess.
  }

  public static class MyTestSpec {}

  static FileObject makeFileObjectForString(String value) throws IOException {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(value.getBytes());
    final FileObject file = mock(FileObject.class);
    when(file.openInputStream()).thenReturn(inputStream);
    return file;
  }

  static PropModel makePropModel(String name) {
    return new PropModel(
        MockMethodParamModel.newBuilder().name(name).build(),
        false,
        false,
        false,
        false,
        ResType.BOOL,
        "");
  }

  private InjectPropModel makeInjectPropModel(String name) {
    return new InjectPropModel(MockMethodParamModel.newBuilder().name(name).build());
  }
}
