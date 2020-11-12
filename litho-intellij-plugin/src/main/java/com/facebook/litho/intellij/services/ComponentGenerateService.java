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

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.redsymbols.FileGenerateUtils;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.processor.PsiLayoutSpecModelFactory;
import com.facebook.litho.specmodels.processor.PsiMountSpecModelFactory;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class helping to create {@link SpecModel}s from the given file and update generated
 * Component files with the new model.
 */
public class ComponentGenerateService {
  private static final PsiLayoutSpecModelFactory LAYOUT_SPEC_MODEL_FACTORY =
      new PsiLayoutSpecModelFactory();
  private static final PsiMountSpecModelFactory MOUNT_SPEC_MODEL_FACTORY =
      new PsiMountSpecModelFactory();
  private final Set<SpecUpdateNotifier> listeners = Collections.synchronizedSet(new HashSet<>());
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

  @Nullable
  public PsiClass updateComponentSync(PsiClass specCls) {
    final Pair<String, String> newComponent = createLithoFileContent(specCls);
    if (newComponent == null) return null;

    return FileGenerateUtils.updateClass(
        newComponent.first, newComponent.second, specCls.getProject());
  }

  @Nullable
  public SpecModel getSpecModel(PsiClass specClass) {
    return specFqnToModelMap.get(specClass.getQualifiedName());
  }

  /**
   * Creates new Component file text content.
   *
   * @param specCls corresponding to generated Component.
   * @return a Pair of Component FQN (first) and file text content (second). Null if specCls is not
   *     valid.
   */
  @Nullable
  Pair<String, String> createLithoFileContent(PsiClass specCls) {
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

    final String newContent = createFileContentFromModel(componentQN, model);
    return Pair.create(componentQN, newContent);
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
