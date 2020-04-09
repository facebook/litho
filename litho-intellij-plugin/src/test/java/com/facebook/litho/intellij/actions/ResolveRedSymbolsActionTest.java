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

package com.facebook.litho.intellij.actions;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import java.io.IOException;
import java.util.Collection;
import org.junit.Test;

public class ResolveRedSymbolsActionTest extends LithoPluginIntellijTest {

  public ResolveRedSymbolsActionTest() {
    super("testdata/actions");
  }

  @Test
  public void collectRedSymbols() throws IOException {
    final PsiFile file = testHelper.configure("ResolveRedSymbolsActionTest.java");

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final VirtualFile vf = file.getViewProvider().getVirtualFile();
              final Collection<String> symbols =
                  ResolveRedSymbolsAction.collectRedSymbols(
                      vf, testHelper.getFixture().getProject());

              assertThat(symbols.size()).isOne();
              assertThat(symbols.contains("Layout")).isTrue();
            });
  }
}
