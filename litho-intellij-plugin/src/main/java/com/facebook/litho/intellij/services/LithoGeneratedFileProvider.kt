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
import com.facebook.litho.intellij.PsiSearchUtils
import com.facebook.litho.intellij.extensions.EventLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

/** Provider of generated Litho files information. */
class LithoGeneratedFileProvider private constructor() {

  private val componentFQNToSpec: MutableMap<String, PsiClass> = HashMap()

  /**
   * Guesses class fully qualified names for the given short name. This method should be called
   * before [createFileContent]
   */
  fun guessQualifiedNames(
      project: Project,
      searchScope: GlobalSearchScope,
      shortName: String,
      eventMetadata: MutableMap<String, String>,
  ): List<String> {
    val specName =
        LithoPluginUtils.getLithoComponentSpecNameFromComponent(shortName) ?: return emptyList()
    return PsiSearchUtils.getInstance()
        .findClassesByShortName(project, searchScope, specName)
        .filter { cls ->
          if (LithoPluginUtils.isLayoutSpec(cls)) {
            eventMetadata[EventLogger.KEY_CLASS] = "layout_spec"
            true
          } else if (LithoPluginUtils.isMountSpec(cls)) {
            eventMetadata[EventLogger.KEY_CLASS] = "mount_spec"
            true
          } else if (LithoPluginUtils.hasLithoSectionSpecAnnotation(cls)) {
            eventMetadata[EventLogger.KEY_CLASS] = "section_spec"
            true
          } else {
            false
          }
        }
        .mapNotNull { specCls ->
          val componentFQN =
              LithoPluginUtils.getLithoComponentNameFromSpec(specCls.qualifiedName)
                  ?: return@mapNotNull null
          componentFQNToSpec[componentFQN] = specCls
          componentFQN
        }
  }

  /**
   * Creates new file text content for the given fully qualified name.
   *
   * @param fqn should be one of the names provided by [guessQualifiedNames].
   * @return file text content.
   */
  fun createFileContent(fqn: String): String? {
    val specCls = componentFQNToSpec[fqn] ?: return null
    val content = createFileContent(specCls) ?: return null
    return content.second
  }

  fun clear() {
    componentFQNToSpec.clear()
  }

  private object Holder {
    val INSTANCE: LithoGeneratedFileProvider = LithoGeneratedFileProvider()
  }

  companion object {
    @JvmStatic
    fun INSTANCE(): LithoGeneratedFileProvider {
      return Holder.INSTANCE
    }

    @JvmStatic
    fun createFileContent(specCls: PsiClass): Pair<String, String>? {
      return ComponentGenerateService.getInstance().createLithoFileContent(specCls)
    }
  }
}
