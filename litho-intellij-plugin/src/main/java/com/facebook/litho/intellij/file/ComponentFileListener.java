/*
 * Copyright 2019-present Facebook, Inc.
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
package com.facebook.litho.intellij.file;

import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.completion.ComponentGenerateUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.DebounceEventLogger;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Listener updates Generated Component if saving file contains {@link LayoutSpec}
 *
 * @see FileDocumentManagerListener
 */
public class ComponentFileListener implements FileDocumentManagerListener {
  private static final String TAG = EventLogger.EVENT_GENERATE_COMPONENT + ".saving";
  private static final EventLogger logger =
      new DebounceEventLogger(60 /*minutes*/ * 60 /*seconds*/ * 1000);
  private final Consumer<PsiClass> savingFileConsumer;

  public ComponentFileListener() {
    this(
        layoutSpecCls -> {
          if (ComponentGenerateUtils.updateLayoutComponent(layoutSpecCls)) {
            logger.log(TAG);
          }
        });
  }

  @VisibleForTesting
  ComponentFileListener(Consumer<PsiClass> savingFileConsumer) {
    this.savingFileConsumer = savingFileConsumer;
  }

  @Override
  public void beforeAllDocumentsSaving() {}

  @Override
  public void beforeDocumentSaving(Document document) {
    beforeDocumentSavingInternal(document);
  }

  /** Uses Psi access and must be called on EventDispatch Thread or within Read Action */
  private void beforeDocumentSavingInternal(Document document) {
    Arrays.stream(ProjectManager.getInstance().getOpenProjects())
        .filter(project -> !DumbService.isDumb(project))
        .map(PsiDocumentManager::getInstance)
        .map(psiDocumentManager -> psiDocumentManager.getPsiFile(document))
        .filter(Objects::nonNull)
        .filter(this::notComponent)
        .forEach(this::beforeFileSaving);
  }

  @VisibleForTesting
  boolean notComponent(PsiFile psiFile) {
    return !LithoPluginUtils.getFirstComponent(psiFile).isPresent();
  }

  @VisibleForTesting
  void beforeFileSaving(PsiFile psiFile) {
    LithoPluginUtils.getFirstLayoutSpec(psiFile).ifPresent(savingFileConsumer);
  }

  @Override
  public void beforeFileContentReload(VirtualFile file, Document document) {}

  @Override
  public void fileWithNoDocumentChanged(VirtualFile file) {}

  @Override
  public void fileContentReloaded(VirtualFile file, Document document) {}

  @Override
  public void fileContentLoaded(VirtualFile file, Document document) {}

  @Override
  public void unsavedDocumentsDropped() {}
}
