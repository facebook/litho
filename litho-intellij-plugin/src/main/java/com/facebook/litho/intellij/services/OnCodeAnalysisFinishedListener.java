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

package com.facebook.litho.intellij.services;

import com.facebook.litho.intellij.actions.ResolveRedSymbolsAction;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.facebook.litho.intellij.settings.AppSettingsState;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.messages.MessageBusConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Daemon finished listener invokes red symbols analysis. */
public class OnCodeAnalysisFinishedListener
    implements DaemonCodeAnalyzer.DaemonListener, FileEditorManagerListener, Disposable {
  private static final Logger LOG = Logger.getInstance(OnCodeAnalysisFinishedListener.class);
  private final Set<String> analyzedFiles = Collections.synchronizedSet(new HashSet<>());
  private final Set<String> pendingFiles = Collections.synchronizedSet(new HashSet<>());
  private final Project project;
  private final MessageBusConnection bus;

  OnCodeAnalysisFinishedListener(Project project) {
    this.project = project;
    this.bus = project.getMessageBus().connect();
    bus.subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, this);
    bus.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this);
    LOG.debug("Connected");
  }

  @Override
  public void dispose() {
    bus.disconnect();
    LOG.debug("Disposed");
  }

  @Override
  public void daemonFinished() {
    LOG.debug("Daemon finished");
    final AppSettingsState.Model state = AppSettingsState.getInstance(project).getState();
    if (!state.resolveRedSymbols) return;

    // As in com.intellij.codeInsight.daemon.impl.StatusBarUpdater.java
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) return;

    final Document document = editor.getDocument();
    final VirtualFile vf = FileDocumentManager.getInstance().getFile(document);
    if (vf == null) return;

    final String path = vf.getPath();
    boolean stopProcessing = analyzedFiles.contains(path) || pendingFiles.contains(path);
    if (stopProcessing) return;

    final PsiFile pf = PsiManager.getInstance(project).findFile(vf);
    if (!(pf instanceof PsiJavaFile)) return;

    pendingFiles.add(path);
    final Map<String, String> eventMetadata = new HashMap<>();
    ResolveRedSymbolsAction.resolveRedSymbols(
        (PsiJavaFile) pf,
        vf,
        editor,
        project,
        eventMetadata,
        newSymbolsFound -> {
          pendingFiles.remove(path);
          analyzedFiles.add(path);
          eventMetadata.put(EventLogger.KEY_TYPE, "daemon");
          eventMetadata.put(EventLogger.KEY_RESULT, newSymbolsFound ? "success" : "fail");
          LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_RED_SYMBOLS, eventMetadata);
        });
  }

  @Override
  public void daemonCancelEventOccurred(String reason) {}

  @Override
  public void selectionChanged(FileEditorManagerEvent event) {
    final VirtualFile oldFile = event.getOldFile();
    if (oldFile != null) {
      analyzedFiles.remove(oldFile.getPath());
    }
  }
}
