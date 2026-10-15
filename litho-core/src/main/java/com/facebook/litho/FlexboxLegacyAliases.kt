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

@file:Suppress("DEPRECATION")

package com.facebook.litho

/**
 * Migration scaffolding: a typealias for the legacy SpecGen [Row] class under a non-colliding name.
 *
 * When migrating Kotlin call sites from the legacy `ResourcesScope.Row { ... }` DSL to the new
 * `com.facebook.litho.layout.flexbox.Row(...) { ... }` function, any file that also constructs the
 * legacy SpecGen `Row` via `Row.create(c).build()` hits a name collision: the new function and the
 * legacy class share the simple name `Row`. Rewriting `Row.create(c)` to `RowBuilder.create(c)`
 * (and switching the import to `com.facebook.litho.RowBuilder`) frees the `Row` name in the file
 * for the new function.
 *
 * Java callers are unaffected — typealiases are Kotlin-only and Java code keeps using
 * `com.facebook.litho.Row` directly. The Java migration is handled separately in Phase 3 of the
 * Row/Column migration plan.
 */
@Deprecated(
    "Migration scaffold. Use com.facebook.litho.layout.flexbox.Row(...) for new construction. " +
        "This alias only exists so files that also import the new Row function can keep " +
        "calling Row.create(c).build() under the disambiguated name RowBuilder.create(c).build().",
)
typealias RowBuilder = Row

/**
 * Migration scaffolding: a typealias for the legacy SpecGen [Column] class under a non-colliding
 * name. See [RowBuilder] for rationale.
 */
@Deprecated(
    "Migration scaffold. Use com.facebook.litho.layout.flexbox.Column(...) for new construction. " +
        "This alias only exists so files that also import the new Column function can keep " +
        "calling Column.create(c).build() under the disambiguated name ColumnBuilder.create(c).build().",
)
typealias ColumnBuilder = Column
