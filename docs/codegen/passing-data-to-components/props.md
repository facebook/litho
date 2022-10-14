---
id: spec-props
title: Props in Specs
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
import VersionedCodeBlock from '@theme/VersionedCodeBlock';

:::caution
This page covers the old Java Spec API. If the Spec API is not being used, refer to the [Components](../../mainconcepts/components-basics.mdx) page.
:::

Litho uses a unidirectional data flow with immutable inputs. Following the name established by [React](https://reactjs.org/docs/components-and-props.html), inputs to a `Component` are known as *props*.

In the Spec API, props for a given `Component` are the union of all arguments annotated with `@Prop`. The value of the props can be accessed in all the methods that declare it as a `@Prop` parameter. The same prop can be defined and accessed in multiple lifecycle methods. The annotation processor will ensure you're using consistent prop types and consistent annotation parameters across all the spec methods.

In the Kotlin API, props are just `val` properties on a Component and can be accessed in the `render` function and its hooks.

## Prop immutability

The props of a Component are read-only. The Component's parent passes down values for the props when it creates the Component, and they cannot change throughout the lifecycle of the Component. If the props values must be updated, the parent has to create a new Component and pass down new values for the props.

:::important
Props should be immutable. Due to background layout, props may be accessed on multiple threads. The immutability of props ensures that no thread safety issues can occur during the component's lifecycle.
:::

## How to use Props

### Define Props on a Component

The way props are defined is shown in the following sample:

<Tabs
  groupId="props_tab_group"
  defaultValue="kotlin_props_tab"
  values={[
    {label: 'Kotlin API', value: 'kotlin_props_tab'},
    {label: 'Spec API', value: 'java_props_tab'},
  ]}>
  <TabItem value="kotlin_props_tab">

Props are just `val` properties of a Component.

```kotlin file=sample/src/main/java/com/facebook/samples/litho/kotlin/documentation/HelloComponent.kt start=start_simple_example end=end_simple_example
```

  </TabItem>
  <TabItem value="java_props_tab">

Props are defined using the `@Prop` annotation.

```java file=sample/src/main/java/com/facebook/samples/litho/onboarding/FirstComponentSpec.java start=start_example end=end_example
```

  </TabItem>
</Tabs>

The props parameters will hold the value passed down from the Component's parent when the Component was created (or their default values).

Props are defined and used the same way as in `LayoutSpec`s and `MountSpec`s.

### Set Props on a Component

The following code shows how to set Props on a Component.

<Tabs
  groupId="props_tab_group"
  defaultValue="kotlin_props_tab"
  values={[
    {label: 'Kotlin API', value: 'kotlin_props_tab'},
    {label: 'Spec API', value: 'java_props_tab'},
  ]}>
  <TabItem value="kotlin_props_tab">

The prop can be passed by its name in the KComponent.

```kotlin
KotlinApiComponent(name = "Linda")
```

  </TabItem>
  <TabItem value="java_props_tab">

The annotation processor creates a `Builder` class for the Component automatically, with setters, for each unique prop defined on the spec.
Values for these props can be passed down by calling the appropriate methods on the generated Component Builder:

```java
FirstComponent.create(c).name("Linda").build();
```

  </TabItem>
</Tabs>

## Optional Props and Prop Defaults

Litho provides a way to mark props as optional and define their default values:

<Tabs
  groupId="props_tab_group"
  defaultValue="kotlin_props_tab"
  values={[
    {label: 'Kotlin API', value: 'kotlin_props_tab'},
    {label: 'Spec API', value: 'java_props_tab'},
  ]}>
  <TabItem value="kotlin_props_tab">

In the Kotlin API, default values are always explicit because optional props are just normal constructor params with [default arguments](https://kotlinlang.org/docs/functions.html#default-arguments).

```kotlin file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropDefaultKComponent.kt start=start_example end=end_example
```

   </TabItem>
  <TabItem value="java_props_tab">

In the Spec API, a prop can be marked as optional by setting `optional = true` flag on its `@Prop` annotation, as seen in the example below.
It needs to be marked as such in all the methods that declare this prop, otherwise the annotation processor will throw an exception.
By default, if an optional prop's value is not provided when the component is created, a default value for its Java type is used (`null`, `0`, or `false`).

```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/OptionalPropComponentSpec.java start=start_example end=end_example
```

Instead of using the Java defaults, define an explicit default value for an optional prop. To create that default value, declare a `static final` field with the same name and type as the original prop and mark it with the [`@PropDefault`](pathname:///javadoc/com/facebook/litho/annotations/PropDefault.html) annotation, as shown in the following example:

```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropDefaultComponentSpec.java start=start_example end=end_example
```

  </TabItem>
</Tabs>

## Android Resources as Props

When creating layouts, it's common to use values from Android's resource system, such as dimensions, colours, strings, and so on. The Litho Spec API provides convenient ways to set prop values from Android resources using annotations, as shown in the following examples:

<Tabs
  groupId="props_tab_group"
  defaultValue="kotlin_props_tab"
  values={[
    {label: 'Kotlin API', value: 'kotlin_props_tab'},
    {label: 'Spec API', value: 'java_props_tab'},
  ]}>

<TabItem value="kotlin_props_tab">

Here, `PropWithoutResourceTypeKComponent` is expected to take `color` as an integer, `size` in pixels, and a `name` string.

```kotlin file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropWithoutResourceTypeKComponent.kt start=start_example end=end_example
```

If the props are to be set using resource values, it's recommended to do the following:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropResourceTypeParentKComponent.kt start=start_prop_without_resource_type_usage end=end_prop_without_resource_type_usage
```

But Litho provides a nicer way to provide prop values via resource ids.

In the Kotlin API, there is no way to generate multiple variants of the same setter for a prop. However, helper functions can be used to retrieve the value of a resource by its ID, for example `stringRes()`, `dimenRes()`, `colorRes()`, `colorAttr`, and so on.

```kotlin file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropResourceTypeParentKComponent.kt start=start_prop_with_resource_type_usage end=end_prop_with_resource_type_usage
```

Other supported functions are `drawableRes()`, `drawableAttr()`, `drawableColor()`, `colorAttr()` and `intRes()`. These can be found in the code under [`Resources`](https://github.com/facebook/litho/blob/master/litho-core-kotlin/src/main/kotlin/com/facebook/litho/Resources.kt)
  </TabItem>
  <TabItem value="java_props_tab">

In the following example, `PropWithoutResourceTypeComponentSpec` is expected to take `color` as an integer, `size` in pixels, and a `name` string.

```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropWithoutResourceTypeComponentSpec.java start=start_example end=end_example
```

If these props are to be set using resource values, it's recommended to do the following:

```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropResourceTypeParentComponentSpec.java start=start_prop_without_resource_type_usage end=end_prop_without_resource_type_usage
```

But Litho provides a nicer way to provide prop values via resource ids. Just add the `@Prop` parameter `resType`, which then creates multiple builder methods on the component for the single prop.

```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropWithResourceTypeComponentSpec.java start=start_example end=end_example
```

With these params applied, for each resource prop `PropWithResourceTypeComponentSpec`'s builder will have not only the main setter (like `name()`), but also its variants for resource ids with *Res*, *Attr*, *Dip*, *Sp* or *Px* suffixes, depending on prop's resource type (such as `nameRes()` or `sizePx()`).

```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropResourceTypeParentComponentSpec.java start=start_prop_with_resource_type_usage end=end_prop_with_resource_type_usage
```

Other supported resource types are `ResType.STRING_ARRAY`, `ResType.INT`, `ResType.INT_ARRAY`, `ResType.BOOL`, `ResType.COLOR`, `ResType.DIMEN_OFFSET`, `ResType.FLOAT`, and `ResType.DRAWABLE`.

`PropDefault` also supports values from Resources by setting a `resType` and `resId`.

The following example shows how to define a `PropDefault` with a resource value:

```java
@PropDefault(resType = ResType.DIMEN_SIZE, resId = R.dimen.vertical_spacing) static float spacingVertical;
```

  </TabItem>
</Tabs>

## Variable Arguments in Props

Sometimes, passing a list of items can be a bit painful as it requires the Developer to create a list structure, add all the items to it, then pass the list to the Component:

<Tabs
  groupId="props_tab_group"
  defaultValue="kotlin_props_tab"
  values={[
    {label: 'Kotlin API', value: 'kotlin_props_tab'},
    {label: 'Spec API', value: 'java_props_tab'},
  ]}>
   <TabItem value="kotlin_props_tab">

In the Kotlin API, use the [variable number of arguments (varargs) modifier](https://kotlinlang.org/docs/functions.html#variable-number-of-arguments-varargs) provided by the language itself to achieve this behaviour.

```kotlin file=sample/src/main/java/com/facebook/samples/litho/documentation/props/VariableArgumentPropKComponent.kt start=start_example end=end_example
```

It can then be used as follows:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/documentation/props/VariableArgumentPropParentKComponent.kt start=start_var_arg_usage end=end_var_arg_usage
```

   </TabItem>

  <TabItem value="java_props_tab">

In the Spec API, the `varArg` parameter in the `@Prop` annotation aims to make this a little easier, enabling the Prop to be set using multiple methods. A list of strings can be set by using a method named for the prop: `names(...)`.  Alternatively, set one or many individual strings using a method named for a value of a `varArg` parameter in the `@Prop` annotation: `name(...)`, as shown in the following example:

```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/VariableArgumentPropComponentSpec.java start=start_example end=end_example
```

It can then be used as follows:

```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/VariableArgumentPropParentComponentSpec.java start=start_var_arg_usage end=end_var_arg_usage
```

  </TabItem>
 </Tabs>

Variable Arguments also work with Android resources as props:

 <Tabs
   groupId="props_tab_group"
   defaultValue="kotlin_props_tab"
   values={[
     {label: 'Kotlin API', value: 'kotlin_props_tab'},
     {label: 'Spec API', value: 'java_props_tab'},
   ]}>
    <TabItem value="kotlin_props_tab">

  In Kotlin, variable arguments can be used with Android resources by using the helper functions to resolve the value by resource ID. The following example shows how to provide multiple strings as props, mixing `String` variables and Android string resources:

  ```kotlin file=sample/src/main/java/com/facebook/samples/litho/documentation/props/VariableArgumentPropParentKComponent.kt start=start_var_arg_res_type_usage end=end_var_arg_res_type_usage
  ```

  </TabItem>

  <TabItem value="java_props_tab">

  Given the following component:

  ```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/VariableArgumentWithResourceTypeSpec.java start=start_example end=end_example
  ```

  Multiple strings can be added by mixing `String` variables with Android string resources:

  ```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/VariableArgumentPropParentComponentSpec.java start=start_var_arg_res_type_usage end=end_var_arg_res_type_usage
  ```

 </TabItem>
 </Tabs>
