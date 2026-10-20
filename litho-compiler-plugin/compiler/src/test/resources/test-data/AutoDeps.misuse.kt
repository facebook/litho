// @nolint
// PROCESSOR: ErrorOnlyProcessor
// EXPECTED:
// e: file:///AutoDepsMisuse.kt:10:5 @AutoDeps declarations must have at least one parameter annotated @DepsFrom, whose captured values become the dependencies.
// e: file:///AutoDepsMisuse.kt:10:5 @AutoDeps declarations must have a parameter annotated @Deps to receive the inferred dependency array.
// e: file:///AutoDepsMisuse.kt:13:5 @AutoDeps declarations must have at least one parameter annotated @DepsFrom, whose captured values become the dependencies.
// e: file:///AutoDepsMisuse.kt:16:5 @AutoDeps declarations must have a parameter annotated @Deps to receive the inferred dependency array.
// e: file:///AutoDepsMisuse.kt:19:28 A non-vararg @Deps parameter must have a default value. Omitting the argument at the call site is what tells the plugin to infer, and a parameter that cannot be omitted can never be inferred.
// e: file:///AutoDepsMisuse.kt:22:39 A @Deps parameter must be declared 'vararg deps: Any?' or 'deps: Array<Any?>'. The inferred values are Any?.
// e: file:///AutoDepsMisuse.kt:25:62 A @DepsFrom parameter must be a function type or a fun interface -- anything else has no closure to read the dependencies from.
// e: file:///AutoDepsMisuse.kt:28:27 @AutoDeps declarations must have at least one parameter annotated @DepsFrom, whose captured values become the dependencies.
// e: file:///AutoDepsMisuse.kt:31:77 A @DepsFrom parameter must be a function type or a fun interface -- anything else has no closure to read the dependencies from.
// e: file:///AutoDepsMisuse.kt:34:50 A non-vararg @Deps parameter must have a default value. Omitting the argument at the call site is what tells the plugin to infer, and a parameter that cannot be omitted can never be inferred.
// e: file:///AutoDepsMisuse.kt:34:50 @AutoDeps declarations must have exactly one @Deps parameter. The inferred array is written into the first one, so this one would never be filled in.
// e: file:///AutoDepsMisuse.kt:39:3 @AutoDeps declarations must have at least one parameter annotated @DepsFrom, whose captured values become the dependencies.
// e: file:///AutoDepsMisuse.kt:39:3 @AutoDeps declarations must have a parameter annotated @Deps to receive the inferred dependency array.
// e: file:///AutoDepsMisuse.kt:42:39 @Deps and @DepsFrom only take effect on a declaration annotated @AutoDeps. Without it nothing is inferred and the call site silently takes this parameter's default.
// e: file:///AutoDepsMisuse.kt:42:61 @Deps and @DepsFrom only take effect on a declaration annotated @AutoDeps. Without it nothing is inferred and the call site silently takes this parameter's default.
// e: file:///AutoDepsMisuse.kt:44:43 @Deps and @DepsFrom only take effect on a declaration annotated @AutoDeps. Without it nothing is inferred and the call site silently takes this parameter's default.
// e: file:///AutoDepsMisuse.kt:44:91 @Deps and @DepsFrom only take effect on a declaration annotated @AutoDeps. Without it nothing is inferred and the call site silently takes this parameter's default.
// e: file:///AutoDepsMisuse.kt:47:59 A @DepsFrom parameter must not have a default value. A default lambda is not written at the call site, so the plugin cannot read what it captures, and those values would silently be missing from the dependency list.
// e: file:///AutoDepsMisuse.kt:50:11 @AutoDeps declarations must have at least one parameter annotated @DepsFrom, whose captured values become the dependencies.
// e: file:///AutoDepsMisuse.kt:50:11 @AutoDeps declarations must have a parameter annotated @Deps to receive the inferred dependency array.
// e: file:///AutoDepsMisuse.kt:53:8 @AutoDeps declarations must have at least one parameter annotated @DepsFrom, whose captured values become the dependencies.
// e: file:///AutoDepsMisuse.kt:53:8 @AutoDeps declarations must have a parameter annotated @Deps to receive the inferred dependency array.
// END

// FILE: AutoDepsMisuse.kt

import com.facebook.rendercore.annotations.AutoDeps
import com.facebook.rendercore.annotations.Deps
import com.facebook.rendercore.annotations.DepsFrom

fun interface Thing {
  fun go()
}

@AutoDeps
fun missingBoth(count: Int): Int = count

@AutoDeps
fun missingDepsFrom(@Deps vararg deps: Any?): Int = deps.size

@AutoDeps
fun missingDeps(@DepsFrom func: () -> Unit): Int = 0

@AutoDeps
fun depsNotOmittable(@Deps deps: Array<Any?>, @DepsFrom func: () -> Unit): Int = deps.size

@AutoDeps
fun depsWrongElementType(@Deps vararg deps: String, @DepsFrom func: () -> Unit): Int = deps.size

@AutoDeps
fun depsFromNotFunctional(@Deps vararg deps: Any?, @DepsFrom value: Int): Int = value

@AutoDeps
class MissingDepsFromClass(@Deps vararg val deps: Any?)

@AutoDeps
class DepsFromNotFunctionalClass(@Deps vararg val deps: Any?, @DepsFrom val value: Int)

@AutoDeps
fun duplicateDeps(@Deps vararg deps: Any?, @Deps other: Array<Any?>, @DepsFrom func: () -> Unit): Int =
    deps.size + other.size

@AutoDeps
class SecondaryConstructorOnly {
  constructor(ignored: Int)
}

fun orphanedParticipants(@Deps vararg deps: Any?, @DepsFrom func: () -> Unit): Int = deps.size

class OrphanedParticipantsClass(@Deps val deps: Array<Any?> = emptyArray(), @DepsFrom val func: () -> Unit)

@AutoDeps
fun depsFromHasDefault(@Deps vararg deps: Any?, @DepsFrom func: () -> Unit = {}): Int = deps.size

@AutoDeps
interface InterfaceHasNoConstructor

@AutoDeps
object ObjectHasNoParameters
