// @nolint
// PROCESSOR: ErrorOnlyProcessor
// EXPECTED:
// END

// FILE: AutoDepsGood.kt

import com.facebook.rendercore.annotations.AutoDeps
import com.facebook.rendercore.annotations.Deps
import com.facebook.rendercore.annotations.DepsFrom

fun interface Thing {
  fun go()
}

fun interface ScopedThing {
  fun String.go()
}

@AutoDeps
fun plainLambda(@Deps vararg deps: Any?, @DepsFrom func: () -> Unit): Int = deps.size

@AutoDeps
fun funInterface(@Deps vararg deps: Any?, @DepsFrom func: Thing): Int = deps.size

@AutoDeps
fun funInterfaceWithReceiver(@Deps vararg deps: Any?, @DepsFrom func: ScopedThing): Int = deps.size

@AutoDeps
fun twoSources(
    @Deps vararg deps: Any?,
    @DepsFrom first: () -> Unit,
    @DepsFrom second: Thing,
): Int = deps.size

@AutoDeps
fun String.extensionForm(@Deps vararg deps: Any?, @DepsFrom func: () -> Unit): Int = deps.size

@AutoDeps
fun arrayDeps(@Deps deps: Array<Any?> = emptyArray(), @DepsFrom func: () -> Unit): Int = deps.size

@AutoDeps
fun varargTakenBySomethingElse(
    vararg values: Int,
    @Deps deps: Array<Any?> = emptyArray(),
    @DepsFrom func: () -> Unit,
): Int = deps.size + values.size

@AutoDeps
class AnnotatedClass(@Deps vararg val deps: Any?, @DepsFrom val func: Thing)

class NotAnnotated(vararg val deps: Any?, val func: Thing)
