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

package com.facebook.litho.intellij.toolwindows;

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.facebook.litho.intellij.services.ComponentGenerateService;
import com.facebook.litho.specmodels.model.SpecModel;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.structureView.StructureView;
import com.intellij.ide.structureView.StructureViewFactory;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UI;
import com.intellij.util.ui.UIUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import org.jetbrains.annotations.Nullable;

/** Service creates structure view reflecting {@link ComponentTreeModel}. */
class ComponentStructureView implements Disposable {
  private static final JComponent STUB;

  static {
    final JTextArea text =
        new JTextArea(
            "Spec structure will be shown here for a valid ComponentSpec file (LayoutSpec, MountSpec, GroupSectionSpec)");
    text.setLineWrap(true);
    text.setEditable(false);
    text.setBackground(null);
    text.setWrapStyleWord(true);
    text.setFont(UIUtil.getLabelFont());
    text.setMargin(UI.insets(10));

    STUB = UI.PanelFactory.panel(text).createPanel();
  }

  private final AtomicBoolean isVisible = new AtomicBoolean(false);
  private final Project project;
  private ContentManager contentManager;
  private Content contentContainer;
  @VisibleForTesting @Nullable StructureView structureView;

  static ComponentStructureView getInstance(Project project) {
    return ServiceManager.getService(project, ComponentStructureView.class);
  }

  ComponentStructureView(Project project) {
    this.project = project;
    Disposer.register(project, this);
  }

  @Override
  public void dispose() {
    contentManager = null;
    contentContainer.dispose();
    contentContainer = null;
    dispose(structureView);
  }

  synchronized void setup(ToolWindow toolWindow) {
    contentManager = toolWindow.getContentManager();
    Disposer.register(contentManager, this);
    contentContainer = contentManager.getFactory().createContent(STUB, "", false);
    contentManager.addContent(contentContainer);
    isVisible.set(toolWindow.isVisible());
    updateViewLater(null);
    ComponentGenerateService.getInstance().subscribe(this::updateViewLater, this);
    final MessageBusConnection busConnection = project.getMessageBus().connect(this);
    busConnection.subscribe(
        FileEditorManagerListener.FILE_EDITOR_MANAGER,
        new FileEditorManagerListener() {
          @Override
          public void selectionChanged(FileEditorManagerEvent event) {
            updateViewLater(null);
          }
        });
    busConnection.subscribe(
        ToolWindowManagerListener.TOPIC,
        new ToolWindowManagerListener() {
          @Override
          public void stateChanged() {
            final boolean visible = toolWindow.isVisible();
            final boolean wasVisible = isVisible.getAndSet(visible);
            if (visible && !wasVisible) {
              updateViewLater(null);
            }
          }
        });
  }

  private void updateViewLater(@Nullable PsiClass updatedClass) {
    if (!isVisible.get()) return;

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      ReadAction.run(() -> updateView(updatedClass));
    } else {
      DumbService.getInstance(project).smartInvokeLater(() -> updateView(updatedClass));
    }
  }

  /** Updates view either if updatedClass is null, or if updatedClass is focused. */
  private synchronized void updateView(@Nullable PsiClass updatedClass) {
    // Sanity check in case view was disposed
    if (contentManager == null || contentContainer == null) return;

    final FileEditor selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor();
    final PsiFile selectedFile = getSelectedFile(selectedEditor, project);
    final PsiClass selectedClass =
        Optional.ofNullable(selectedFile)
            .flatMap(file -> LithoPluginUtils.getFirstClass(file, cls -> true))
            .orElse(null);

    if (updatedClass != null && updatedClass != selectedClass) {
      return;
    }

    // assumes Litho Spec is always the first top level class in the file
    String loggingType;
    if (LithoPluginUtils.isLayoutSpec(selectedClass)) {
      loggingType = "layout_spec";
    } else if (LithoPluginUtils.isMountSpec(selectedClass)) {
      loggingType = "mount_spec";
    } else if (selectedClass != null
        && LithoPluginUtils.hasLithoSectionSpecAnnotation(selectedClass)) {
      loggingType = "section_spec";
    } else {
      loggingType = "not_spec";
    }

    final StructureView oldStructure = structureView;
    final Map<String, String> data = new HashMap<>();
    data.put(EventLogger.KEY_TYPE, loggingType);
    // Overridden below
    data.put(EventLogger.KEY_RESULT, "fail");
    final JComponent newView =
        Optional.ofNullable(selectedClass)
            .map(ComponentGenerateService.getInstance()::getOrCreateSpecModel)
            .map(
                model -> {
                  structureView = createStructureView(model, selectedEditor, selectedFile, project);
                  data.put(EventLogger.KEY_RESULT, "success");
                  return structureView.getComponent();
                })
            .orElse(STUB);
    contentContainer.setComponent(newView);
    // View wasn't updating without this step
    contentManager.setSelectedContent(contentContainer);
    dispose(oldStructure);
    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_TOOLWINDOW, data);
  }

  private void dispose(@Nullable StructureView oldStructure) {
    if (oldStructure == null) return;

    oldStructure.dispose();
    if (this.structureView == oldStructure) {
      this.structureView = null;
    }
  }

  private static StructureView createStructureView(
      SpecModel model, FileEditor selectedEditor, PsiFile selectedFile, Project project) {
    final StructureViewModel viewModel = ComponentTreeModel.create(selectedFile, model);
    final StructureView view =
        StructureViewFactory.getInstance(project)
            .createStructureView(selectedEditor, viewModel, project, true);
    Disposer.register(view, viewModel);
    return view;
  }

  private static JComponent createView(JComponent top, JComponent bottom) {
    return FormBuilder.createFormBuilder()
        .addComponent(top, 1)
        .addComponentFillVertically(bottom, 0)
        .getPanel();
  }

  @Nullable
  private static PsiFile getSelectedFile(@Nullable FileEditor editor, Project project) {
    if (editor == null) return null;

    final VirtualFile vf = editor.getFile();
    if (vf == null) return null;

    return PsiManager.getInstance(project).findFile(vf);
  }
}
