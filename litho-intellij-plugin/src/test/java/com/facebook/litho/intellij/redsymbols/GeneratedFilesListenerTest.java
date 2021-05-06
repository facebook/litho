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

package com.facebook.litho.intellij.redsymbols;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import java.io.IOException;
import java.util.Collections;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

public class GeneratedFilesListenerTest extends LithoPluginIntellijTest {

  public GeneratedFilesListenerTest() {
    super("testdata/services");
  }

  @After
  @Override
  public void tearDown() throws Exception {
    PsiSearchUtils.getInstance().clearMocks();
    ComponentsCacheService.getInstance(testHelper.getProject()).dispose();
    super.tearDown();
  }

  @Test
  public void after_Remove() throws IOException {
    final Project project = testHelper.getFixture().getProject();
    final GeneratedFilesListener listener = new GeneratedFilesListener(project);
    final PsiFile file = testHelper.configure("LayoutSpec.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              final PsiClass cls = PsiTreeUtil.findChildOfType(file, PsiClass.class);
              // Add file to cache
              FileGenerateUtils.generateClass(cls);
              final ComponentsCacheService service = ComponentsCacheService.getInstance(project);
              final PsiClass component = service.getComponent("Layout");
              assertThat(component).isNotNull();

              // Mock file created and found on disk
              final VFileEvent mockEvent = mockEvent();
              PsiSearchUtils.getInstance().addMock("Layout", cls);
              listener.after(Collections.singletonList(mockEvent));
              // Ensure cache is cleaned
              final PsiClass component2 = service.getComponent("Layout");
              assertThat(component2).isNull();
            });
  }

  private VFileEvent mockEvent() {
    final VFileEvent event = Mockito.mock(VFileCreateEvent.class);
    VirtualFile mockFile = Mockito.mock(VirtualFile.class);
    when(mockFile.isValid()).thenReturn(true);
    when(mockFile.getPath()).thenReturn(GeneratedFilesListener.BUCK_OUT_BASE);
    when(event.getFile()).thenReturn(mockFile);
    return event;
  }
}
