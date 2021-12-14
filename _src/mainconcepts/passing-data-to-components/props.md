---
id: props
title: Props
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
import VersionedCodeBlock from '@theme/VersionedCodeBlock';

Litho uses a unidirectional data flow with immutable inputs. Following the name established by [React](https://reactjs.org/docs/components-and-props.html), the inputs that a `Component` takes are known as *props*.

In the Spec API, props for a given `Component` are the union of all arguments annotated with `@Prop`. You can access the value of the props in all the methods that declare it as a `@Prop` parameter. The same prop can be defined and accessed in multiple lifecycle methods. The annotation processor will ensure you're using consistent prop types and consistent annotation parameters across all the spec methods.

In the Kotlin API, props are just `val` properties on Component and can be accessed in the `render` function and its hooks.

## Props Immutability

The props of a Component are read-only. The Component's parent passes down values for the props when it creates the Component and they cannot change throughout the lifecycle of the Component. If the props values must be updated, the parent has to create a new Component and pass down new values for the props.

:::important
The props objects should be made immutable. Due to background layout, props may be accessed on multiple threads. Props immutability ensures that no thread safety issues can happen during the component's lifecycle.
:::

## How to use Props

### Define Props on Component

Let's understand this using an example below.

<Tabs
  groupId="props_tab_group"
  defaultValue="kotlin_props_tab"
  values={[
    {label: 'Kotlin API', value: 'kotlin_props_tab'},
    {label: 'Spec API', value: 'java_props_tab'},
  ]}>
  <TabItem value="kotlin_props_tab">

Props are just `val` properties on component

```kotlin file=sample/src/main/java/com/facebook/samples/litho/kotlin/documentation/KotlinApiComponent.kt start=start_example end=end_example
```
  </TabItem>
  <TabItem value="java_props_tab">

Props are defined using the `@Prop` annotation

```java file=sample/src/main/java/com/facebook/samples/litho/java/onboarding/FirstComponentSpec.java start=start_example end=end_example
```
  </TabItem>
</Tabs>

The props parameters will hold the value passed down from the Component's parent when the Component was created (or their default values).

Props are defined and used the same way in `LayoutSpec`s and `MountSpec`s.

### Set Props on Component

<Tabs
  groupId="props_tab_group"
  defaultValue="kotlin_props_tab"
  values={[
    {label: 'Kotlin API', value: 'kotlin_props_tab'},
    {label: 'Spec API', value: 'java_props_tab'},
  ]}>
  <TabItem value="kotlin_props_tab">

You can simply pass the prop by its name in the KComponent.


```kotlin
KotlinApiComponent(name = "Linda")
```
  </TabItem>
  <TabItem value="java_props_tab">

The annotation processor creates a `Builder` class for your Component automatically with setters for each unique prop defined on the spec.
You pass down values for these props by calling the appropriate methods on the generated Component Builder.

```java
FirstComponent.create(c).name("Linda").build()
```
  </TabItem>
</Tabs>

## Optional Props and Prop Defaults

Litho provides a way to mark props as optional and define their default values.

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

In the Spec API, you can mark a prop as optional by setting `optional = true` flag on its `@Prop` annotation as seen in the example below.
It needs to be marked as such in all the methods that declare this prop, otherwise the annotation processor will throw an exception.
By default, if an optional prop's value is not provided when component is created, then a default value for its Java type would be used, i.e `null`, `0`, `false`.

```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/OptionalPropComponentSpec.java start=start_example end=end_example
```

You will often want to define explicit default value for an optional prop instead of simply using Java's defaults. And to do that you should declare a constant (i.e. `static final`) field with the same name and type as the original prop and mark it with [`@PropDefault`](pathname:///javadoc/com/facebook/litho/annotations/PropDefault.html) annotation. Like in the following example:


```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropDefaultComponentSpec.java start=start_example end=end_example
```
  </TabItem>
</Tabs>


## Android Resources as Props

When creating layouts, it is very common to use values from Android's resource system such as dimensions, colors, strings, etc. Litho Spec API provides convenient ways to set prop values from Android resources using annotations.

Let's consider a simple example:

<Tabs
  groupId="props_tab_group"
  defaultValue="kotlin_props_tab"
  values={[
    {label: 'Kotlin API', value: 'kotlin_props_tab'},
    {label: 'Spec API', value: 'java_props_tab'},
  ]}>

<TabItem value="kotlin_props_tab">

In the following example, `PropWithoutResourceTypeKComponent` is expected to take `color` as an integer, `size` in pixels, and a `name` string.

```kotlin file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropWithoutResourceTypeKComponent.kt start=start_example end=end_example
```

And if you decide to set these props using resource values you'll have to do the following:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropResourceTypeParentKComponent.kt start=start_prop_without_resource_type_usage end=end_prop_without_resource_type_usage
```

But Litho provides a nicer way to provide prop values via resource ids.

In the Kotlin API, there is no way to generate multiple variants of the same setter for a prop, but you can use helper functions to retrieve an actual value of the resource by its ID, for example `stringRes()`, `dimenRes()`, `colorRes()`, `colorAttr`, etc.

```kotlin file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropResourceTypeParentKComponent.kt start=start_prop_with_resource_type_usage end=end_prop_with_resource_type_usage
```

Other supported functions are `drawableRes()`, `drawableAttr()`, `drawableColor()`, `colorAttr()` and `intRes()`. These can be found in the code under [`Resources`](https://github.com/facebook/litho/blob/master/litho-core-kotlin/src/main/kotlin/com/facebook/litho/Resources.kt)
  </TabItem>
  <TabItem value="java_props_tab">

In the following example, `PropWithoutResourceTypeComponentSpec` is expected to take `color` as an integer, `size` in pixels, and a `name` string.

```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropWithoutResourceTypeComponentSpec.java start=start_example end=end_example
```

And if you decide to set these props using resource values you'll have to do the following:

```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropResourceTypeParentComponentSpec.java start=start_prop_without_resource_type_usage end=end_prop_without_resource_type_usage
```

But Litho provides a nicer way to provide prop values via resource ids. You can add `@Prop` parameter `resType` which then creates multiple builder methods on the component for the single prop.

```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropWithResourceTypeComponentSpec.java start=start_example end=end_example
```

With these params applied, for each resource prop `PropWithResourceTypeComponentSpec`'s builder will have not only the main setter (like `name()`), but also its variants for resource ids with *Res*, *Attr*, *Dip*, *Sp* or *Px* suffixes, depending on prop's resource type (like `nameRes()` or `sizePx()`).

```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/PropResourceTypeParentComponentSpec.java start=start_prop_with_resource_type_usage end=end_prop_with_resource_type_usage
```

Other supported resource types are `ResType.STRING_ARRAY`, `ResType.INT`, `ResType.INT_ARRAY`, `ResType.BOOL`, `ResType.COLOR`, `ResType.DIMEN_OFFSET`, `ResType.FLOAT`, and `ResType.DRAWABLE`.

`PropDefault` also support values from Resources via setting a `resType` and `resId`. Let's define
a `PropDefault` with a resource value:

```java
@PropDefault(resType = ResType.DIMEN_SIZE, resId = R.dimen.vertical_spacing) static float spacingVertical;
```
  </TabItem>
</Tabs>

## Variable Arguments in Props

Sometimes, you want to support passing a list of items. This can unfortunately
be a bit painful since it requires the developer to make a list, add all the
items to it, and pass list to the component.

<Tabs
  groupId="props_tab_group"
  defaultValue="kotlin_props_tab"
  values={[
    {label: 'Kotlin API', value: 'kotlin_props_tab'},
    {label: 'Spec API', value: 'java_props_tab'},
  ]}>
   <TabItem value="kotlin_props_tab">

In the Kotlin API, you can use [Variable number of arguments](https://kotlinlang.org/docs/functions.html#variable-number-of-arguments-varargs) provided by the language itself to achieve this behaviour.

```kotlin file=sample/src/main/java/com/facebook/samples/litho/documentation/props/VariableArgumentPropKComponent.kt start=start_example end=end_example
```

It can then be used as follows:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/documentation/props/VariableArgumentPropParentKComponent.kt start=start_var_arg_usage end=end_var_arg_usage
```
   </TabItem>

  <TabItem value="java_props_tab">

In the Spec API, The `varArg` parameter in `@Prop` annotation aims to make this a little easier. This allows to set the prop in multiple ways.
You can set a list of strings by using a method named for the actual prop — `names(...)`, or you can just set one or many individual strings by using a method named for a value of a `varArg` parameter in `@Prop` annotation, i.e.  `name(...)` in following example.

```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/VariableArgumentPropComponentSpec.java start=start_example end=end_example
```

It can then be used as follows:

```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/VariableArgumentPropParentComponentSpec.java start=start_var_arg_usage end=end_var_arg_usage
```
  </TabItem>
 </Tabs>

Variable Arguments also works with Android resources as props.

 <Tabs
   groupId="props_tab_group"
   defaultValue="kotlin_props_tab"
   values={[
     {label: 'Kotlin API', value: 'kotlin_props_tab'},
     {label: 'Spec API', value: 'java_props_tab'},
   ]}>
    <TabItem value="kotlin_props_tab">

  In Kotlin variable arguments can be used with Android resources naturally by simply using helper functions to resolve actual value by resource id. In the following example you can see how to provide multiple strings as props, mixing `String` variables and Android string resources:

  ```kotlin file=sample/src/main/java/com/facebook/samples/litho/documentation/props/VariableArgumentPropParentKComponent.kt start=start_var_arg_res_type_usage end=end_var_arg_res_type_usage
  ```
  </TabItem>

  <TabItem value="java_props_tab">

  For instance, given a Component like this:


  ```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/VariableArgumentWithResourceTypeSpec.java start=start_example end=end_example
  ```

  You can add multiple strings mixing `String` variables and Android string resources:

  ```java file=sample/src/main/java/com/facebook/samples/litho/documentation/props/VariableArgumentPropParentComponentSpec.java start=start_var_arg_res_type_usage end=end_var_arg_res_type_usage
  ```
  </TabItem>
  </Tabs>
