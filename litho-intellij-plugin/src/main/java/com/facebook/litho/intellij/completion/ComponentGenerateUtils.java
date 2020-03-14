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

package com.facebook.litho.intellij.completion;

import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.processor.PsiLayoutSpecModelFactory;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.actions.ElementCreator;
import com.intellij.ide.fileTemplates.JavaCreateFromTemplateHandler;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.util.Deque;
import java.util.LinkedList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class helping to create {@link LayoutSpecModel}s from the given file and update generated
 * Component files with the new model.
 */
public class ComponentGenerateUtils {
  private static final String TAG = EventLogger.EVENT_GENERATE_COMPONENT + ".method";

  private static final PsiLayoutSpecModelFactory MODEL_FACTORY = new PsiLayoutSpecModelFactory();

  private ComponentGenerateUtils() {}

  /**
   * Updates existing generated Component file from the given Spec class or do nothing if provided
   * class doesn't contain {@link LayoutSpec}.
   *
   * @param layoutSpecCls class containing {@link LayoutSpec} class.
   */
  public static void updateLayoutComponent(PsiClass layoutSpecCls) {
    LithoLoggerProvider.getEventLogger().log(TAG + ".invoke");
    String specName = layoutSpecCls.getQualifiedName();
    if (specName == null) {
      return;
    }
    SpecModel model = createLayoutModel(layoutSpecCls);
    if (model == null) {
      return;
    }
    String dirPath = getDirectoryPath(layoutSpecCls.getContainingFile().getContainingDirectory());
    if (dirPath == null) {
      return;
    }
    final Project project = layoutSpecCls.getProject();
    final int created =
        new ComponentUpdater(specName, model, dirPath, project).tryCreate(specName).length;
    final String componentName = model.getComponentName();
    // TODO: T56876413 improve notifications
    if (created > 0) {
      LithoPluginUtils.showInfo(componentName + " component was regenerated", project);
      LithoLoggerProvider.getEventLogger().log(TAG + ".success");
    } else {
      LithoPluginUtils.showWarning(componentName + " component was not found", project);
    }
  }

  @VisibleForTesting
  @Contract("null->null")
  @Nullable
  static String getDirectoryPath(@Nullable PsiDirectory directory) {
    if (directory == null) {
      return null;
    }
    VirtualFile currentDir = directory.getVirtualFile();
    VirtualFile root = directory.getProject().getBaseDir();
    Deque<String> path = new LinkedList<>();
    while (!currentDir.equals(root)) {
      path.addFirst(currentDir.getName());
      currentDir = currentDir.getParent();
      if (currentDir == null) {
        return null;
      }
    }
    return StringUtil.join(path, "/");
  }

  /**
   * Generates new {@link LayoutSpecModel} from the given {@link PsiClass}.
   *
   * @return new {@link LayoutSpecModel} or null if provided class is not a {@link
   *     com.facebook.litho.annotations.LayoutSpec} class.
   */
  @Nullable
  public static LayoutSpecModel createLayoutModel(PsiClass layoutSpecCls) {
    return MODEL_FACTORY.createWithPsi(layoutSpecCls.getProject(), layoutSpecCls, null);
  }

  /**
   * Access is allowed from event dispatch thread only. Example usage: new
   * ComponentUpdater(..).tryCreate(..);
   */
  private static class ComponentUpdater extends ElementCreator {
    private final Project project;
    private final SpecModel model;
    private final String specDirectoryPath;
    private final String specQualifiedName;

    ComponentUpdater(
        String specQualifiedName, SpecModel specModel, String specDirectoryPath, Project project) {
      super(project, "Couldn't update generated Component file");
      this.project = project;
      this.model = specModel;
      this.specQualifiedName = specQualifiedName;
      this.specDirectoryPath = specDirectoryPath;
    }

    @Override
    protected PsiElement[] create(String qualifiedSpecName) {
      String specPackageName = StringUtil.getPackageName(specQualifiedName);
      return ComponentBuildInfoProvider.getInstance()
          .provideGeneratedComponentDirs(specDirectoryPath, specQualifiedName, project)
          .findFirst()
          .map(
              targetDirectory ->
                  createFileWithModel(targetDirectory, specPackageName, project, model))
          .map(createdClass -> doPostponedOperationsAndUnblockDocument(createdClass, project))
          .map(createdClass -> new PsiElement[] {createdClass})
          .orElse(PsiElement.EMPTY_ARRAY);
    }

    private static PsiClass createFileWithModel(
        PsiDirectory targetDirectory, String packageName, Project project, SpecModel model) {
      TypeSpec typeSpec = model.generate(RunMode.normal());
      String content =
          JavaFile.builder(packageName, typeSpec).skipJavaLangImports(true).build().toString();

      String extension = StdFileTypes.JAVA.getDefaultExtension();
      PsiFile oldFile = targetDirectory.findFile(model.getComponentName() + "." + extension);
      if (oldFile != null) {
        oldFile.delete();
      }
      // Invokes PsiDirectory#add method, shouldn't be called on EventDispatch Thread
      return JavaCreateFromTemplateHandler.createClassOrInterface(
          project, targetDirectory, content, true, extension);
    }

    /**
     * Applies pending changes made through the PSI to the document.
     *
     * @param createdClass the PSI class for which the Document is requested.
     * @param project the project for which the document manager is requested.
     * @return createdClass.
     */
    @Contract("_, _ -> param1")
    private static PsiClass doPostponedOperationsAndUnblockDocument(
        PsiClass createdClass, Project project) {
      PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
      Document document = psiDocumentManager.getDocument(createdClass.getContainingFile());
      if (document != null) {
        psiDocumentManager.doPostponedOperationsAndUnblockDocument(document);
      }
      return createdClass;
    }

    @Override
    public boolean startInWriteAction() {
      return true;
    }

    @Override
    protected String getActionName(String newName) {
      return "GenerateComponentAction";
    }
  }
}
