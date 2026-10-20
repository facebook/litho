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

package com.facebook.litho.common

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * The type lists the capture analysis consults, parsed from `litho-autodeps.conf`.
 *
 * Two of the analysis levels need to know about types the plugin cannot reason about on its own,
 * and neither list can be expressed as annotations on the types themselves because the most
 * important entries -- `android.content.Context` and friends -- are types Litho does not own.
 * - [ignore]: ambient values rather than reactive inputs. Dropped from the inferred set entirely.
 *   This under-captures on purpose, which is why it lives in a reviewable file.
 * - [expand]: no meaningful equivalence. Triggers expansion to the value's own inputs.
 * - [stable]: identity equality is acceptable here; keep as a dependency and stay quiet about it.
 *
 * An entry matches the named type and all of its subtypes. Precedence is [ignore], then [expand],
 * then [stable].
 */
class AutoDepsConfig(
    val ignore: List<Entry>,
    val expand: List<Entry>,
    val stable: List<Entry>,
) {

  /**
   * One directive line: the type it names and where it came from.
   *
   * The provenance is what makes an unresolvable entry actionable. It also decides whether the
   * entry is worth complaining about at all: the shipped default is deliberately broad and names
   * types -- `android.content.Context`, Litho's own -- that not every module compiling with the
   * plugin can see, so a default entry that does not resolve is expected. An entry an author
   * supplied for their own module is a specific claim, and one that never applies is a bug.
   */
  class Entry(val type: ClassId, val where: String, val isDefault: Boolean)

  /** Merges [other] over this config; entries accumulate, so a later file can only add. */
  operator fun plus(other: AutoDepsConfig): AutoDepsConfig = AutoDepsConfig(
      ignore = ignore + other.ignore,
      expand = expand + other.expand,
      stable = stable + other.stable,
  )

  companion object {
    private const val DEFAULT_RESOURCE = "/litho-autodeps.conf"
    private val WHITESPACE = Regex("\\s+")

    /**
     * The default lists, shipped as a jar resource so every target that gets the plugin gets them.
     *
     * There is no per-target override: the plugin takes no compiler options, and [plus] is how a
     * caller holding its own [parse] result layers one on.
     */
    fun load(): AutoDepsConfig {
      // Absent means the jar was built wrong, and an empty config is not a safe reading of that:
      // every `ignore` entry would stop applying, so `ComponentContext` and friends would become
      // dependencies that never compare equal and every annotated call site would stop caching.
      // Loud here beats that, everywhere, silently.
      val stream =
          checkNotNull(AutoDepsConfig::class.java.getResourceAsStream(DEFAULT_RESOURCE)) {
            "$DEFAULT_RESOURCE is missing from the Litho compiler plugin jar."
          }
      return stream.bufferedReader().use {
        parse(it.readText(), DEFAULT_RESOURCE, isDefault = true)
      }
    }

    /**
     * Parses the line-oriented format: `#` comments, blank lines ignored, one `<directive> <type>`
     * per line.
     *
     * Throws [IllegalArgumentException] naming [origin] and the line number on a malformed line --
     * a silently skipped entry is an entry that never applies and that nobody notices.
     */
    fun parse(text: String, origin: String, isDefault: Boolean): AutoDepsConfig {
      val ignore = mutableListOf<Entry>()
      val expand = mutableListOf<Entry>()
      val stable = mutableListOf<Entry>()
      text.lineSequence().forEachIndexed { index, rawLine ->
        val line = rawLine.substringBefore('#').trim()
        if (line.isEmpty()) return@forEachIndexed
        val where = "$origin:${index + 1}"
        val fields = line.split(WHITESPACE)
        require(fields.size == 2) {
          "$where: expected '<ignore | expand | stable> <fully.qualified.TypeName>', got '$line'"
        }
        val (directive, name) = fields
        val classId =
            requireNotNull(name.toClassId()) {
              "$where: '$name' is not a fully qualified type name -- no segment starts with an " +
                  "uppercase letter, so the class name cannot be told from the package"
            }
        val entry = Entry(classId, where, isDefault)
        when (directive) {
          "ignore" -> ignore += entry
          "expand" -> expand += entry
          "stable" -> stable += entry
          else ->
              throw IllegalArgumentException(
                  "$where: unknown directive '$directive', expected 'ignore', 'expand' or 'stable'",
              )
        }
      }
      return AutoDepsConfig(ignore, expand, stable)
    }

    /**
     * Splits a dotted name into its package and class parts, treating the first segment that starts
     * with an uppercase letter as the beginning of the class name. Everything after it is a nested
     * class. That convention is the only information a flat dotted string carries.
     */
    private fun String.toClassId(): ClassId? {
      val segments = split('.')
      val classStart = segments.indexOfFirst { it.firstOrNull()?.isUpperCase() == true }
      if (classStart < 0) return null
      return ClassId(
          FqName.fromSegments(segments.subList(0, classStart)),
          FqName.fromSegments(segments.subList(classStart, segments.size)),
          false,
      )
    }
  }
}
