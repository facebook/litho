/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.specmodels.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.facebook.litho.annotations.ResType;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.testing.assertj.LithoAssertions;
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
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests {@link PropNameInterStageStore} */
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
    when(mFiler.getResource(any(JavaFileManager.Location.class), anyString(), anyString()))
        .thenReturn(fileObject);

    final Optional<ImmutableList<String>> strings =
        store.loadNames(new MockName("com.example.MyComponentSpec"));
    LithoAssertions.assertThat(strings.isPresent()).isTrue();
    LithoAssertions.assertThat(strings.get()).containsExactly("arg0", "arg1");

    verify(mFiler)
        .getResource(
            StandardLocation.CLASS_PATH, "", "META-INF/litho/com.example.MyComponentSpec.props");
  }

  @Test
  public void testSave() throws IOException {
    final PropNameInterStageStore store = new PropNameInterStageStore(mFiler);

    final MockSpecModel specModel =
        MockSpecModel.newBuilder()
            .rawProps(ImmutableList.of(makePropModel("param0"), makePropModel("param1")))
            .specTypeName(ClassName.get(MyTestSpec.class))
            .build();
    store.saveNames(specModel);

    verify(mFiler)
        .createResource(
            StandardLocation.CLASS_OUTPUT,
            "",
            "META-INF/litho/com.facebook.litho.specmodels.processor.PropNameInterStageStoreTest.MyTestSpec.props");

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
        MockMethodParamModel.newBuilder().name(name).build(), false, ResType.BOOL, "");
  }
}
