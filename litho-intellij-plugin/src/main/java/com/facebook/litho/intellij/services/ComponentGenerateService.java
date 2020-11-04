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

import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.file.ComponentScope;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.processor.PsiLayoutSpecModelFactory;
import com.facebook.litho.specmodels.processor.PsiMountSpecModelFactory;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class helping to create {@link SpecModel}s from the given file and update generated
 * Component files with the new model.
 */
public class ComponentGenerateService {
  private static final Logger LOG = Logger.getInstance(ComponentGenerateService.class);
  private static final PsiLayoutSpecModelFactory LAYOUT_SPEC_MODEL_FACTORY =
      new PsiLayoutSpecModelFactory();
  private static final PsiMountSpecModelFactory MOUNT_SPEC_MODEL_FACTORY =
      new PsiMountSpecModelFactory();
  private final Set<SpecUpdateNotifier> listeners = Collections.synchronizedSet(new HashSet<>());
  private final List<String> underAnalysis = Collections.synchronizedList(new LinkedList<>());
  private final Map<String, SpecModel> specFqnToModelMap =
      Collections.synchronizedMap(createLRUMap(50));

  public interface SpecUpdateNotifier {
    void onSpecModelUpdated(PsiClass specCls);
  }

  public static ComponentGenerateService getInstance() {
    return ServiceManager.getService(ComponentGenerateService.class);
  }

  private ComponentGenerateService() {}

  /** Subscribes listener to Spec model updates. Removes subscription once parent is disposed. */
  public void subscribe(SpecUpdateNotifier listener, Disposable parent) {
    listeners.add(listener);
    Disposer.register(parent, () -> listeners.remove(listener));
  }

  /**
   * Updates generated Component file from the given Spec class and shows success notification. Or
   * do nothing if provided class doesn't contain {@link LayoutSpec} or {@link MountSpec}.
   *
   * @param specCls class containing {@link LayoutSpec} or {@link MountSpec} class.
   */
  public void updateComponentAsync(PsiClass specCls) {
    final Project project = specCls.getProject();
    final Runnable job =
        () -> {
          final PsiClass component = updateComponentSync(specCls);
          if (component != null) {
            showSuccess(component.getName(), project);
          }
        };
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      job.run();
    } else {
      DumbService.getInstance(project).smartInvokeLater(job);
    }
  }

  /** @return false iff class is under analysis. Otherwise starts analysis. */
  public boolean tryUpdateComponent(PsiClass specCls) {
    LOG.debug("Under update " + underAnalysis);
    if (underAnalysis.contains(specCls.getQualifiedName())) {
      return false;
    }

    updateComponentSync(specCls);
    return true;
  }

  @Nullable
  public PsiClass updateComponentSync(PsiClass specCls) {
    final String qName = specCls.getQualifiedName();
    underAnalysis.add(qName);
    PsiClass psiClass;
    try {
      psiClass = updateComponent(specCls);
    } finally {
      underAnalysis.remove(qName);
    }
    LOG.debug("Update finished " + qName + (psiClass != null));
    return psiClass;
  }

  @Nullable
  public SpecModel getSpecModel(PsiClass specClass) {
    return specFqnToModelMap.get(specClass.getQualifiedName());
  }

  @Nullable
  private PsiClass updateComponent(PsiClass specCls) {
    final String componentQN =
        LithoPluginUtils.getLithoComponentNameFromSpec(specCls.getQualifiedName());
    if (componentQN == null) return null;

    final SpecModel model = createModel(specCls);
    if (model == null) return null;

    // New model might be malformed to generate component, but it's accurate to the Spec
    specFqnToModelMap.put(specCls.getQualifiedName(), model);
    Set<SpecUpdateNotifier> copy;
    synchronized (listeners) {
      copy = new HashSet<>(listeners);
    }
    copy.forEach(listener -> listener.onSpecModelUpdated(specCls));

    return updateComponent(componentQN, model, specCls.getProject());
  }

  /** Updates generated Component file from the given Spec model. */
  @Nullable
  private static PsiClass updateComponent(
      String componentQualifiedName, SpecModel model, Project project) {
    final Optional<PsiClass> generatedClass =
        Optional.ofNullable(PsiSearchUtils.findOriginalClass(project, componentQualifiedName))
            .filter(cls -> !ComponentScope.contains(cls.getContainingFile()));
    final String newContent = createFileContentFromModel(componentQualifiedName, model);
    if (generatedClass.isPresent()) {
      return updateExistingComponent(newContent, generatedClass.get(), project);
    } else {
      return updateInMemoryComponent(newContent, componentQualifiedName, project);
    }
  }

  @Nullable
  private static PsiClass updateExistingComponent(
      String newContent, PsiClass generatedClass, Project project) {
    // Null is not expected scenario
    final Document document =
        PsiDocumentManager.getInstance(project).getDocument(generatedClass.getContainingFile());
    if (newContent.equals(document.getText())) {
      return generatedClass;
    }

    // Write access is allowed inside write-action only and on EDT
    if (ApplicationManager.getApplication().isDispatchThread()) {
      updateDocument(newContent, document);
    } else {
      ApplicationManager.getApplication().invokeLater(() -> updateDocument(newContent, document));
    }
    // Currently, we don't need reference to the pre-existing component's PsiClass.
    return null;
  }

  private static void updateDocument(String newContent, Document document) {
    WriteAction.run(() -> document.setText(newContent));
    FileDocumentManager.getInstance().saveDocument(document);
  }

  @Nullable
  private static PsiClass updateInMemoryComponent(
      String newContent, String componentQualifiedName, Project project) {
    final String componentShortName = StringUtil.getShortName(componentQualifiedName);
    if (componentShortName.isEmpty()) return null;

    final ComponentsCacheService cacheService = ComponentsCacheService.getInstance(project);
    final PsiClass oldComponent = cacheService.getComponent(componentQualifiedName);
    if (oldComponent != null && newContent.equals(oldComponent.getContainingFile().getText())) {
      return oldComponent;
    }

    final PsiFile file =
        PsiFileFactory.getInstance(project)
            .createFileFromText(componentShortName + ".java", StdFileTypes.JAVA, newContent);
    ComponentScope.include(file);
    final PsiClass inMemory =
        LithoPluginUtils.getFirstClass(file, cls -> componentShortName.equals(cls.getName()))
            .orElse(null);
    if (inMemory == null) return null;

    cacheService.update(componentQualifiedName, inMemory);
    return inMemory;
  }

  private static void showSuccess(String componentName, Project project) {
    LithoPluginUtils.showInfo(componentName + " was regenerated", project);
  }

  /**
   * Generates new {@link SpecModel} from the given {@link PsiClass}.
   *
   * @return new {@link SpecModel} or null if provided class is not a {@link
   *     com.facebook.litho.annotations.LayoutSpec} or {@link
   *     com.facebook.litho.annotations.MountSpec} class.
   */
  @Nullable
  private static SpecModel createModel(PsiClass specCls) {
    final LayoutSpecModel layoutSpecModel =
        LAYOUT_SPEC_MODEL_FACTORY.createWithPsi(specCls.getProject(), specCls, null);
    if (layoutSpecModel != null) {
      return layoutSpecModel;
    }
    return MOUNT_SPEC_MODEL_FACTORY.createWithPsi(specCls.getProject(), specCls, null);
  }

  private static String createFileContentFromModel(String clsQualifiedName, SpecModel specModel) {
    TypeSpec typeSpec = specModel.generate(RunMode.normal());
    return JavaFile.builder(StringUtil.getPackageName(clsQualifiedName), typeSpec)
        .skipJavaLangImports(true)
        .build()
        .toString();
  }

  private static <K, V> Map<K, V> createLRUMap(final int maxEntries) {
    return new LinkedHashMap<K, V>(maxEntries * 10 / 7, 0.7f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxEntries;
      }
    };
  }
}
