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
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import javax.annotation.Nullable;

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
   * Updates existing generated Component file.
   *
   * @param qualifiedSpecName fully qualified name of the Spec class to update Component for.
   * @param specModel {@link SpecModel} of the Spec class
   * @return true, if the Component file was updated. False otherwise.
   */
  private static boolean updateComponent(
      Project project, String qualifiedSpecName, SpecModel specModel) {
    PsiElement[] psiElements =
        new ComponentUpdater(project, specModel).tryCreate(qualifiedSpecName);
    return psiElements.length > 0;
  }

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
      return LithoPluginUtils.findComponentFile(qualifiedSpecName, project)
          .map(componentFile -> updateFileWithModel(componentFile, model))
          .map(psiClass -> new PsiElement[] {psiClass})
          .orElse(PsiElement.EMPTY_ARRAY);
    }

    private static PsiClass updateFileWithModel(PsiJavaFile componentFile, SpecModel model) {
      PsiDirectory targetDirectory = componentFile.getContainingDirectory();
      String packageName = componentFile.getPackageName();
      Project project = componentFile.getProject();
      componentFile.delete();

      TypeSpec typeSpec = model.generate(RunMode.normal());
      String content =
          JavaFile.builder(packageName, typeSpec).skipJavaLangImports(true).build().toString();
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
