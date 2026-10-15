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

package com.facebook.litho.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * Applies the Litho compiler plugin, which Kotlin code that calls Litho hooks must be built with.
 *
 * Usage: `plugins { id("com.facebook.litho") version "<litho version>" }`. The plugin is built
 * against a single Kotlin version; see the Litho release notes for the one each release supports.
 */
class LithoGradlePlugin : KotlinCompilerPluginSupportPlugin {

  override fun apply(target: Project) = Unit

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

  override fun getCompilerPluginId(): String = "com.facebook.litho.compiler"

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
      groupId = "com.facebook.litho",
      artifactId = "litho-compiler-plugin",
      version = LITHO_VERSION,
  )

  override fun applyToCompilation(
      kotlinCompilation: KotlinCompilation<*>,
  ): Provider<List<SubpluginOption>> {
    kotlinCompilation.compileTaskProvider.configure { task ->
      task.compilerOptions.optIn.add(HOOK_OPT_IN)
    }
    return kotlinCompilation.target.project.provider { emptyList() }
  }

  private companion object {
    const val HOOK_OPT_IN = "com.facebook.litho.annotations.Hook"
  }
}
