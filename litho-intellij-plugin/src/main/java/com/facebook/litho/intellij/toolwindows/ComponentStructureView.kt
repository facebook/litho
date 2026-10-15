/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.intellij.toolwindows

import com.facebook.litho.intellij.LithoPluginUtils
import com.facebook.litho.intellij.extensions.EventLogger
import com.facebook.litho.intellij.logging.LithoLoggerProvider
import com.facebook.litho.intellij.services.ComponentGenerateService
import com.facebook.litho.specmodels.model.SpecModel
import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.structureView.StructureView
import com.intellij.ide.structureView.StructureViewFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UI
import com.intellij.util.ui.UIUtil
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.JTextArea

/** Service creates structure view reflecting [ComponentTreeModel]. */
internal class ComponentStructureView(private val project: Project) : Disposable {

  private val isVisible = AtomicBoolean(false)
  private var contentManager: ContentManager? = null
  private var contentContainer: Content? = null
  @VisibleForTesting var structureView: StructureView? = null

  init {
    Disposer.register(project, this)
  }

  override fun dispose() {
    contentManager = null
    contentContainer?.dispose()
    contentContainer = null
    dispose(structureView)
  }

  @Synchronized
  fun setup(toolWindow: ToolWindow) {
    val contentManager = toolWindow.contentManager
    this.contentManager = contentManager
    Disposer.register(contentManager, this)
    val contentContainer = contentManager.factory.createContent(STUB, "", false)
    this.contentContainer = contentContainer
    contentManager.addContent(contentContainer)
    isVisible.set(toolWindow.isVisible)
    updateViewLater(null)
    ComponentGenerateService.getInstance()
        .subscribe({ updatedClass -> updateViewLater(updatedClass) }, this)
    val busConnection = project.messageBus.connect(this)
    busConnection.subscribe(
        FileEditorManagerListener.FILE_EDITOR_MANAGER,
        object : FileEditorManagerListener {
          override fun selectionChanged(event: FileEditorManagerEvent) {
            updateViewLater(null)
          }
        },
    )
    busConnection.subscribe(
        ToolWindowManagerListener.TOPIC,
        object : ToolWindowManagerListener {
          override fun stateChanged() {
            val visible = toolWindow.isVisible
            val wasVisible = isVisible.getAndSet(visible)
            if (visible && !wasVisible) {
              updateViewLater(null)
            }
          }
        },
    )
  }

  private fun updateViewLater(updatedClass: PsiClass?) {
    if (!isVisible.get()) {
      return
    }

    if (ApplicationManager.getApplication().isUnitTestMode) {
      ReadAction.run<RuntimeException> { updateView(updatedClass) }
    } else {
      DumbService.getInstance(project).smartInvokeLater { updateView(updatedClass) }
    }
  }

  /** Updates view either if updatedClass is null, or if updatedClass is focused. */
  @Synchronized
  private fun updateView(updatedClass: PsiClass?) {
    // Sanity check in case view was disposed
    val contentManager = contentManager ?: return
    val contentContainer = contentContainer ?: return

    val selectedEditor = FileEditorManager.getInstance(project).selectedEditor
    val selectedFile = getSelectedFile(selectedEditor, project)
    val selectedClass =
        if (selectedFile != null) {
          LithoPluginUtils.getFirstClass(selectedFile) { true }
        } else {
          null
        }

    if (updatedClass != null && updatedClass !== selectedClass) {
      return
    }

    // assumes Litho Spec is always the first top level class in the file
    val loggingType =
        when {
          LithoPluginUtils.isLayoutSpec(selectedClass) -> "layout_spec"
          LithoPluginUtils.isMountSpec(selectedClass) -> "mount_spec"
          selectedClass != null &&
              LithoPluginUtils.hasLithoSectionSpecAnnotation(selectedClass) -> {
            "section_spec"
          }
          else -> "not_spec"
        }

    val oldStructure = structureView
    val data = HashMap<String, String>()
    data[EventLogger.KEY_TYPE] = loggingType
    // Overridden below
    data[EventLogger.KEY_RESULT] = "fail"
    val specModel =
        if (selectedClass != null) {
          ComponentGenerateService.getInstance().getOrCreateSpecModel(selectedClass)
        } else {
          null
        }
    val newView: JComponent =
        if (specModel != null && selectedEditor != null && selectedFile != null) {
          val newStructureView =
              createStructureView(specModel, selectedEditor, selectedFile, project)
          structureView = newStructureView
          data[EventLogger.KEY_RESULT] = "success"
          newStructureView.component
        } else {
          STUB
        }
    contentContainer.component = newView
    // View wasn't updating without this step
    contentManager.setSelectedContent(contentContainer)
    dispose(oldStructure)
    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_TOOLWINDOW, data)
  }

  private fun dispose(oldStructure: StructureView?) {
    if (oldStructure == null) {
      return
    }

    oldStructure.dispose()
    if (this.structureView === oldStructure) {
      this.structureView = null
    }
  }

  companion object {
    private val STUB: JComponent

    init {
      val text = JTextArea(
          "Spec structure will be shown here for a valid ComponentSpec file (LayoutSpec," +
              " MountSpec, GroupSectionSpec, DiffSectionSpec)",
      )
      text.lineWrap = true
      text.isEditable = false
      text.background = null
      text.wrapStyleWord = true
      text.font = UIUtil.getLabelFont()
      text.margin = JBUI.insets(10)

      STUB = UI.PanelFactory.panel(text).createPanel()
    }

    @JvmStatic
    fun getInstance(project: Project): ComponentStructureView {
      return project.getService(ComponentStructureView::class.java)
    }

    private fun createStructureView(
        model: SpecModel,
        selectedEditor: FileEditor,
        selectedFile: PsiFile,
        project: Project,
    ): StructureView {
      val viewModel = ComponentTreeModel.create(selectedFile, model)
      val view =
          StructureViewFactory.getInstance(project)
              .createStructureView(selectedEditor, viewModel, project, true)
      Disposer.register(view, viewModel)
      return view
    }

    @Suppress("unused")
    private fun createView(top: JComponent, bottom: JComponent): JComponent {
      return FormBuilder.createFormBuilder()
          .addComponent(top, 1)
          .addComponentFillVertically(bottom, 0)
          .panel
    }

    private fun getSelectedFile(editor: FileEditor?, project: Project): PsiFile? {
      if (editor == null) {
        return null
      }

      val vf = editor.file ?: return null

      return PsiManager.getInstance(project).findFile(vf)
    }
  }
}
