# Changelog

## Version 0.29.0

_2019-07-11_
 
 * New: Additional Sections debugging APIs:
   - Make `Change.getRenderInfos()` public.
   - Add `ChangesInfo.getAllChanges()`.
 * Fix: Don't crash on dangling mount content.
 
 For more details, see the [full diff](https://github.com/facebook/litho/compare/v0.28.0...v0.29.0).
 

## Version 0.28.0

_2019-07-05_
 
 * New: Plain code Codelabs with README instructions. Try them out in [codelabs](codelabs).
 * New: Add interface `ChangesetDebugConfiguration.ChangesetDebugListener` for listening for `ChangeSet` generation in Sections.
 * Fix: Cleanup some unused code.
 
 For more details, see the [full diff](https://github.com/facebook/litho/compare/v0.27.0...v0.28.0).


## Version 0.27.0

_2019-06-24_

 * **Breaking: Change in PerfEvents**:
   - Remove `FrameworkLogEvents.EVENT_CREATE_LAYOUT`, `FrameworkLogEvents.EVENT_CSS_LAYOUT `, and `FrameworkLogEvents.EVENT_COLLECT_RESULTS`: these are replaced by the sub-spans `"start_create_layout"`/`"end_create_layout"`, `"start_measure"`/`"end_measure"`, and `"start_collect_results"`/`"end_collect_results"` under the existing top-level `EVENT_CALCULATE_LAYOUT_STATE` event. The `PerfEvent.markerPoint()` API can be used to log these sub-spans. ([b859605](https://github.com/facebook/litho/commit/b859605258da6431b706d17c07df7bc8864396df))
   - Remove `FrameworkLogEvents.PREPARE_MOUNT` without replacement: this didn't provide much value. ([4917370](https://github.com/facebook/litho/commit/4917370d6a7405cddce01c13740f22e9ee736529))
   - Remove `FrameworkLogEvents.DRAW` without replacement: this was not free to maintain and didn't provide much value. ([9e548cb](https://github.com/facebook/litho/commit/9e548cbda5ad78d6f8d3a82ec42639439c118751))
 * **Breaking: The Default Range Ratio** for Sections/`RecyclerBinder` is changed from 4 screens worth of content in either direction to 2. This should improve resource usage with minimal effects on scroll performance. ([9b4fe95](https://github.com/facebook/litho/commit/9b4fe95b8cd48a15046b0d39c4f4756e50f35772))
 * **Breaking: `ComponentsSystrace.provide()`**: `ComponentsSystrace` now assumes an implementation will be provided before any other Litho operations occur. ([457a20f](https://github.com/facebook/litho/commit/457a20f660f14e7132e16668c21b4cf1ce766b70))
 * New: `ComponentsLogger` implementations can now return null for event types they don't care about. ([4075eb7](https://github.com/facebook/litho/commit/4075eb75c9a6f967111b451df6699d1b9b97671b))
 * New: Add `RecyclerCollectionEventsController.requestScrollBy()`. ([0146857](https://github.com/facebook/litho/commit/0146857653e29cb24491cbb8bee9307e55187365))
 * New: Add preliminary Robolectric v4 support. ([4c2f657](https://github.com/facebook/litho/commit/4c2f657f9e6edc45f78b9a8d82085025fa0fc86d), etc.)
 * New: More efficient code generation for state updates in Components and Sections. ([8c5c7e3](https://github.com/facebook/litho/commit/8c5c7e312fb7d1855caabc2fccbf5adc57e6e945), etc.)
 * Fix: Remove usage of API 19+ `Objects` class in cached value API. ([aabb24a](https://github.com/facebook/litho/commit/aabb24a5e67d30e5b93eb43f6c37aae91f455369))
 * Fix: Unset Components scope when creating a new `ComponentContext` in `ComponentTree`. ([05f11a7](https://github.com/facebook/litho/commit/05f11a74a4a06b4abcb1e302f56201609acccad2))
 * Fix: Fix perf logging for dirty mounts. ([3ad8bfb](https://github.com/facebook/litho/commit/3ad8bfba12a43e108602bcd8c63fd2404e6203cb))
 * Fix: Don't crash when `@OnCalculateCachedValue` takes no args. ([2a0f524](https://github.com/facebook/litho/commit/2a0f5240bc8c711d8e45db0e5426517f8bfbf49a))
 * Fix: Reduce number of systrace markers in `collectResults`: these were skewing the perceived size of `LayoutState.collectResults` in production and weren't actionable. ([3107467](https://github.com/facebook/litho/commit/3107467a4e3fd649821757699a5adba683f47edd))
 
 For more details, see the [full diff](https://github.com/facebook/litho/compare/v0.26.0...v0.27.0).
 
 
## Version 0.26.1

_2019-05-28_

* Fix: Picks [513cf91](https://github.com/facebook/litho/commit/513cf91b747bdf06c6bda78840a9195e900d4deb) to fix an issue with the Flipper integration.


## Version 0.26.0

_2019-05-13_

 * **Breaking: Fix the [lazy State update](https://fblitho.com/docs/state#lazy-state-updates) semantics.** ([de3d938](https://github.com/facebook/litho/commit/de3d938c7db32739468617d3b22e5a4568cfc6a5))
 * **Breaking:** Rename `LayoutHandler` to `LithoHandler` and add `DefaultLithoHandler`. ([h69cba5](https://github.com/facebook/litho/commit/h69cba5029c186a4fa81f91c15b44846bc9d79e5c), [0d0bb0b](https://github.com/facebook/litho/commit/0d0bb0b196e4035976cb31c307aee95fd9433a27))
 * **Breaking:** Update Yoga version to `1.14.0`. Fixes [#536](https://github.com/facebook/litho/pull/536). ([c16baf6](https://github.com/facebook/litho/commit/c16baf676f59df99de53cea50e5efa0cb9ddeb0e))
 * **Breaking:** Release sections' `ComponentTree`s when `RecyclerCollectionComponent` is detached. ([8893049](https://github.com/facebook/litho/commit/88930499b19c01953dead8d37e4fed7c1d161ca1))
 * **Breaking:** Only enable incremental mount if parent incremental mount is enabled. ([c88a660](https://github.com/facebook/litho/commit/c88a6605d8e77378e0ac89b267dcb260daddc6f4))
 * **Experimental: Make state updates faster by only recreating the subtrees which are being updated and reuse (read "clone") the untouched subtrees while calculating a new layout.** You can try it out by setting `ComponentsConfiguration.isReconciliationEnabled()` flag globally or using `ComponentTree.Builder.isReconciliationEnabled()` for specific trees.
 * New: Add a `RecyclerBinder.replaceAll(List<RenderInfo>)` method. ([#451](https://github.com/facebook/litho/pull/451))
 * New: Add documentation.
 * Fix: Eliminate Gradle deprecated methods. ([#526](https://github.com/facebook/litho/pull/526))
 * Fix: Cleanup tests and unused code.
 * Fix: Remove object pooling everywhere.
 * Fix: Make Robolectric tests work. ([a92018a](https://github.com/facebook/litho/commit/a92018a32d5241ace4d27f4120908595fb26b51a))
 
 For more details, see the [full diff](https://github.com/facebook/litho/compare/v0.25.0...v0.26.0).
 

## Version 0.25.0

_2019-03-14_

 * **Breaking:** Migrate support lib dependencies to AndroidX. ([de3097b](https://github.com/facebook/litho/commit/de3097b5c7e2bc9ede24ea8c5e8e863e67387039))
 * **Breaking:** Remove `DisplayListDrawable` and other `DisplayList`-related features. ([29f42fa](https://github.com/facebook/litho/commit/29f42fa04dfdff31fc82dd8f199ffc8c62ef5dbf))
 * **Breaking:** Remove `References` API. ([b1aa39a](https://github.com/facebook/litho/commit/b1aa39a460da25fbf29a80f727007a7e1f267440))
 * New: Remove object pooling for most internal objects.
 * New: Replace `powermock-reflect` with internal Whitebox implementation. ([ad899e4](https://github.com/facebook/litho/commit/ad899e4ae2c4217d751a75e82c0646d70b5837a3))
 * New: Enable Gradle incremental AnnotationProcessing. ([a864b5a](https://github.com/facebook/litho/commit/a864b5a642214d1d81c7422d3d4b2d0ada510625))
 * New: Allow `TextInput` to accept `null` as `inputBackground`. ([d1fd03b](https://github.com/facebook/litho/commit/d1fd03b4071794f55fc1617cf365f8a5f2a32558))
 * New: Add documentation.
 * Fix: Suppress focus temporarily while mounting. ([d93e2e0](https://github.com/facebook/litho/commit/d93e2e073a88b86b2674119bd1813a6c982622bf))
 * Fix: When clearing press state, also cancel pending input events. ([451e8b4](https://github.com/facebook/litho/commit/451e8b4a9c5b7744a6cf5b7702754fc0afe025a0))
 * Fix: Correctly calculate `VisibilityChangedEvent` in `MountState`. ([66d65fe](https://github.com/facebook/litho/commit/66d65feb39ed86450ea74891f9211fd6fda6ffc8))
 * Fix: Thread safety issue of `ViewportChanged` listeners of `ViewportManager`. ([9da9d90](https://github.com/facebook/litho/commit/9da9d903c22d0b10d823d509da18c08eb2b2f875))

 For more details, see the [full diff](https://github.com/facebook/litho/compare/v0.24.0...v0.25.0).


## Version 0.24.0

_2019-02-18_

 **Breaking: The default for state updates has changed from sync to async.** At Facebook this meant we ran a codemod script on the codebase and changed all state update methods to the explicit Sync variant (`"Sync"` is just appended to the end of the state update call), then changed the default. The reason for this is to not change existing behavior in case it breaks something, since there are many callsites. The script is committed and available at [scripts/codemod-state-update-methods.sh](scripts/codemod-state-update-methods.sh). We recommend using it if you have any concerns about state update calls automatically becoming async. As a reminder, when you add a `@OnUpdateState` method, it generates three methods: `updateState()`, `updateStateSync()`, and `updateStateAsync()`. Previously `updateState() == updateStateSync()`. Now, `updateState() == updateStateAsync()`.

 For more details, see the [full diff](https://github.com/facebook/litho/compare/v0.23.0...v0.24.0).


## Version 0.23.0

_2019-01-16_

 * **Breaking:** `KeyHandler`s now get registered in EndToEnd tests ([9836497](https://github.com/facebook/litho/commit/9836497c15986ebafbfc2b87b1b4a9730fc73080)). This is a an edge-case, but potentially behavior-changing.
 * New: **Add support for [Cached Values](https://fblitho.com/docs/cached-values)**.
 * New: `isEquivalentTo` now uses reflection for size reasons unless the target is a `MountSpec` or `SectionSpec`. ([2e27d99](https://github.com/facebook/litho/commit/2e27d99b0ae75e8356b5654fa412cb66ec965411))
 * Fix: Potential NPE in `RecyclerEventsController`. ([8e29036](https://github.com/facebook/litho/commit/8e29036de52e5108d5e9853abfcb5571b5643e86))

 For more details, see the [full diff](https://github.com/facebook/litho/compare/v0.22.0...v0.23.0).
