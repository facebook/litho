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

package com.facebook.litho.intellij.services

import com.facebook.litho.intellij.LithoPluginUtils
import com.facebook.litho.specmodels.internal.RunMode
import com.facebook.litho.specmodels.model.SpecModel
import com.facebook.litho.specmodels.processor.PsiDiffSectionSpecModelFactory
import com.facebook.litho.specmodels.processor.PsiGroupSectionSpecModelFactory
import com.facebook.litho.specmodels.processor.PsiLayoutSpecModelFactory
import com.facebook.litho.specmodels.processor.PsiMountSpecModelFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.squareup.javapoet.JavaFile
import java.util.Collections
import java.util.LinkedHashMap

/**
 * Utility class helping to create [SpecModel]s from the given file and update generated Component
 * files with the new model.
 */
class ComponentGenerateService private constructor() {

  private val listeners: MutableSet<SpecUpdateNotifier> = Collections.synchronizedSet(HashSet())
  private val specFqnToModelMap: MutableMap<String, SpecModel> =
      Collections.synchronizedMap(createLRUMap(50))

  fun interface SpecUpdateNotifier {
    fun onSpecModelUpdated(specCls: PsiClass)
  }

  /** Subscribes listener to Spec model updates. Removes subscription once parent is disposed. */
  fun subscribe(listener: SpecUpdateNotifier, parent: Disposable) {
    listeners.add(listener)
    Disposer.register(parent) { listeners.remove(listener) }
  }

  /**
   * Creates new Component file text content.
   *
   * @param specCls corresponding to generated Component.
   * @return a Pair of Component FQN (first) and file text content (second). Null if specCls is not
   *   valid.
   */
  fun createLithoFileContent(specCls: PsiClass): Pair<String, String>? {
    val componentQN =
        LithoPluginUtils.getLithoComponentNameFromSpec(specCls.qualifiedName) ?: return null

    val model = getOrCreateSpecModel(specCls) ?: return null

    val newContent = createFileContentFromModel(componentQN, model)
    return Pair.create(componentQN, newContent)
  }

  fun getOrCreateSpecModel(specClass: PsiClass): SpecModel? {
    return getOrCreateSpecModel(specClass, true)
  }

  fun getOrCreateSpecModel(specClass: PsiClass, useCachedValue: Boolean): SpecModel? {
    val model: SpecModel
    val specFQN = specClass.qualifiedName

    synchronized(specFqnToModelMap) {
      if (useCachedValue && specFqnToModelMap.containsKey(specFQN)) {
        return specFqnToModelMap[specFQN]
      }

      model = createModel(specClass) ?: return null

      // New model might be malformed to generate component, but it's accurate to the Spec.
      // Skip caching when specFQN is null: the original Java code stored under a null key, which
      // would cause unrelated specs without qualified names to share the same cached model.
      if (specFQN != null) {
        specFqnToModelMap[specFQN] = model
      }
    }

    val copy: Set<SpecUpdateNotifier>
    synchronized(listeners) { copy = HashSet(listeners) }
    copy.forEach { listener -> listener.onSpecModelUpdated(specClass) }

    return model
  }

  companion object {
    private val LAYOUT_SPEC_MODEL_FACTORY = PsiLayoutSpecModelFactory()
    private val MOUNT_SPEC_MODEL_FACTORY = PsiMountSpecModelFactory()
    private val GROUP_SECTION_SPEC_MODEL_FACTORY = PsiGroupSectionSpecModelFactory()
    private val DIFF_SECTION_SPEC_MODEL_FACTORY = PsiDiffSectionSpecModelFactory()

    @JvmStatic
    fun getInstance(): ComponentGenerateService {
      return service<ComponentGenerateService>()
    }

    /**
     * Generates new [SpecModel] from the given [PsiClass].
     *
     * @return new [SpecModel] or null if provided class is not a
     *   [com.facebook.litho.annotations.LayoutSpec] or [com.facebook.litho.annotations.MountSpec]
     *   or [com.facebook.litho.sections.annotations.GroupSectionSpec] or
     *   [com.facebook.litho.sections.annotations.DiffSectionSpec] class.
     */
    private fun createModel(specCls: PsiClass): SpecModel? {
      val layoutSpecModel = LAYOUT_SPEC_MODEL_FACTORY.createWithPsi(specCls.project, specCls)
      if (layoutSpecModel != null) {
        return layoutSpecModel
      }
      val mountSpecModel = MOUNT_SPEC_MODEL_FACTORY.createWithPsi(specCls.project, specCls)
      if (mountSpecModel != null) {
        return mountSpecModel
      }
      val groupSectionSpecModel =
          GROUP_SECTION_SPEC_MODEL_FACTORY.createWithPsi(specCls.project, specCls)
      if (groupSectionSpecModel != null) {
        return groupSectionSpecModel
      }
      return DIFF_SECTION_SPEC_MODEL_FACTORY.createWithPsi(specCls.project, specCls)
    }

    private fun createFileContentFromModel(clsQualifiedName: String, specModel: SpecModel): String {
      val typeSpec = specModel.generate(RunMode.normal())
      return JavaFile.builder(StringUtil.getPackageName(clsQualifiedName), typeSpec)
          .skipJavaLangImports(true)
          .build()
          .toString()
    }

    private fun <K, V> createLRUMap(maxEntries: Int): MutableMap<K, V> {
      return object : LinkedHashMap<K, V>(maxEntries * 10 / 7, 0.7f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean {
          return size > maxEntries
        }
      }
    }
  }
}
