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
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.file.ComponentScope;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.facebook.litho.intellij.services.ComponentsCacheService;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.processor.PsiLayoutSpecModelFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class helping to create {@link LayoutSpecModel}s from the given file and update generated
 * Component files with the new model.
 */
public class ComponentGenerateUtils {
  private static final Logger LOG = Logger.getInstance(ComponentGenerateUtils.class);
  private static final String TAG = EventLogger.EVENT_GENERATE_COMPONENT + ".method";

  private static final PsiLayoutSpecModelFactory MODEL_FACTORY = new PsiLayoutSpecModelFactory();

  private ComponentGenerateUtils() {}

  /**
   * Updates generated Component file from the given Spec class or do nothing if provided class
   * doesn't contain {@link LayoutSpec}.
   *
   * @param layoutSpecCls class containing {@link LayoutSpec} class.
   */
  public static void updateLayoutComponent(PsiClass layoutSpecCls) {
    LithoLoggerProvider.getEventLogger().log(TAG + ".invoke");
    final String componentName =
        LithoPluginUtils.getLithoComponentNameFromSpec(layoutSpecCls.getQualifiedName());
    if (componentName == null) return;

    final Project project = layoutSpecCls.getProject();
    final Runnable job =
        () -> {
          final LayoutSpecModel model = createLayoutModel(layoutSpecCls);
          updateComponent(componentName, model, project);
        };
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      job.run();
    } else {
      DumbService.getInstance(project).smartInvokeLater(job);
    }
  }

  /** Updates generated Component file from the given Spec model. */
  public static void updateComponent(
      String componentQualifiedName, @Nullable SpecModel model, Project project) {
    if (model == null) return;

    final Optional<PsiClass> generatedClass =
        Optional.ofNullable(PsiSearchUtils.findOriginalClass(project, componentQualifiedName))
            .filter(cls -> !ComponentScope.contains(cls.getContainingFile()));
    final boolean isPresent = generatedClass.isPresent();
    if (isPresent) {
      final String newContent = createFileContentFromModel(componentQualifiedName, model);
      final Document document =
          PsiDocumentManager.getInstance(project)
              .getDocument(generatedClass.get().getContainingFile());
      if (document != null) {
        // Write access is allowed inside write-action only
        WriteAction.run(
            () -> {
              document.setText(newContent);
            });
        FileDocumentManager.getInstance().saveDocument(document);
        showSuccess(StringUtil.getShortName(componentQualifiedName), project);
      }
    } else {
      final PsiClass component =
          ComponentsCacheService.getInstance(project).update(componentQualifiedName, model);
      if (component != null) {
        showSuccess(component.getName(), project);
      }
    }
  }

  private static void showSuccess(String componentName, Project project) {
    LithoPluginUtils.showInfo(componentName + " was regenerated", project);
    LithoLoggerProvider.getEventLogger().log(TAG + ".success");
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

  public static PsiFile createFileFromModel(
      String clsQualifiedName, SpecModel model, Project project) {
    String fileContent = createFileContentFromModel(clsQualifiedName, model);
    return PsiFileFactory.getInstance(project)
        .createFileFromText(
            StringUtil.getShortName(clsQualifiedName) + ".java", StdFileTypes.JAVA, fileContent);
  }

  private static String createFileContentFromModel(String clsQualifiedName, SpecModel specModel) {
    TypeSpec typeSpec = specModel.generate(RunMode.normal());
    return JavaFile.builder(StringUtil.getPackageName(clsQualifiedName), typeSpec)
        .skipJavaLangImports(true)
        .build()
        .toString();
  }
}
