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
package com.facebook.litho.intellij.completion;

import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.processor.PsiLayoutSpecModelFactory;
import com.intellij.ide.actions.ElementCreator;
import com.intellij.ide.fileTemplates.JavaCreateFromTemplateHandler;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import javax.annotation.Nullable;
import org.jetbrains.annotations.Contract;

/**
 * Utility class helping to create {@link LayoutSpecModel}s from the given file and update generated
 * Component files with the new model.
 */
public class ComponentGenerateUtils {

  private static final PsiLayoutSpecModelFactory MODEL_FACTORY = new PsiLayoutSpecModelFactory();

  private ComponentGenerateUtils() {}

  /**
   * Updates existing generated Component file from the given Spec class or do nothing if provided
   * class doesn't contain {@link LayoutSpec}.
   *
   * @param layoutSpecCls class containing {@link LayoutSpec} class.
   * @return true, if Component file was updated. False otherwise.
   */
  public static boolean updateLayoutComponent(PsiClass layoutSpecCls) {
    SpecModel model = createLayoutModel(layoutSpecCls);
    if (model == null) {
      return false;
    }
    return updateComponent(layoutSpecCls.getProject(), layoutSpecCls.getQualifiedName(), model);
  }

  /**
   * Generates new {@link LayoutSpecModel} from the given {@link PsiClass}.
   *
   * @return new {@link LayoutSpecModel} or null if provided class is null or given class is not a
   *     {@link com.facebook.litho.annotations.LayoutSpec} class.
   */
  @Nullable
  public static LayoutSpecModel createLayoutModel(@Nullable PsiClass layoutSpecCls) {
    if (layoutSpecCls == null) {
      return null;
    }
    return MODEL_FACTORY.createWithPsi(layoutSpecCls.getProject(), layoutSpecCls, null);
  }

  /**
   * Updates existing generated Component.
   *
   * @param qualifiedSpecName fully qualified name of the Spec class to update Component for.
   * @param specModel {@link SpecModel} of the Spec class
   * @return true, if the Component file was updated. False otherwise.
   */
  private static boolean updateComponent(
      Project project, String qualifiedSpecName, SpecModel specModel) {
    return new ComponentUpdater(project, specModel).tryCreate(qualifiedSpecName).length > 0;
  }

  /** Example usage: new ComponentUpdater(project, specModel).tryCreate(qualifiedSpecName); */
  private static class ComponentUpdater extends ElementCreator {
    private final Project project;
    private final SpecModel model;

    ComponentUpdater(Project project, SpecModel model) {
      super(project, "Couldn't update generated Component file");
      this.project = project;
      this.model = model;
    }

    @Override
    protected PsiElement[] create(String qualifiedSpecName) {
      return LithoPluginUtils.findGeneratedFile(qualifiedSpecName, project)
          .map(componentFile -> updateFileWithModel(componentFile, model))
          .map(createdClass -> doPostponedOperationsAndUnblockDocument(createdClass, project))
          .map(createdClass -> new PsiElement[] {createdClass})
          .orElse(PsiElement.EMPTY_ARRAY);
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

    @Nullable
    private static PsiClass updateFileWithModel(PsiJavaFile componentFile, SpecModel model) {
      PsiDirectory targetDirectory = componentFile.getContainingDirectory();
      String packageName = componentFile.getPackageName();
      Project project = componentFile.getProject();
      componentFile.delete();

      TypeSpec typeSpec = model.generate(RunMode.normal());
      String content =
          JavaFile.builder(packageName, typeSpec).skipJavaLangImports(true).build().toString();

      if (content.equals(componentFile.getText())) {
        return null;
      }

      // Invokes PsiDirectory#add method, shouldn't be called on EventDispatch Thread
      return JavaCreateFromTemplateHandler.createClassOrInterface(
          project, targetDirectory, content, true, "java");
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
