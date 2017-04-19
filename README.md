# Litho [![CircleCI](https://circleci.com/gh/facebook/litho/tree/master.svg?style=svg)](https://circleci.com/gh/facebook/litho/tree/master)

<img src="docs/static/logo.png" width=150 align=right>

Litho is a declarative framework for building efficient UIs on Android.

* **Declarative:** Litho uses a declarative API to define UI components. You simply describe the layout for your UI based on a set of immutable inputs and the framework takes care of the rest.
* **Asynchronous layout:** Litho can measure and layout your UI ahead of time without blocking the UI thread.
* **View flattening:** Litho uses [Yoga](https://facebook.github.io/yoga/) for layout and automatically reduces the number of ViewGroups that your UI contains.
* **Fine-grained recycling:** Any component such as a text or image can be recycled and reused anywhere in the UI.

To get started, check out these links:

* [Learn how to use Litho in your project.](http://fblitho.com/docs/getting-started)
* [Get started with our tutorial.](http://fblitho.com/docs/tutorial)
* [Read more about Litho in our docs.](http://fblitho.com/docs/intro)

## Installation
Litho can be integrated either in Gradle or Buck projects. Read our [Getting Started](http://fblitho.com/docs/getting-started) guide for installation instructions.

## Quick start
### 1. Initialize `SoLoader` in your `Application` class.
```java
public class SampleApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    SoLoader.init(this, false);
  }
}
```
### 2. Create and display a component in your Activity
```java
@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext context = new ComponentContext(this);

    final Component component = Text.create(context)
        .text("Hello World")
        .textSizeDip(50)
        .build();

    setContentView(LithoView.create(context, component));
}
```
## Run sample
You can find more examples in our [sample app](https://github.com/facebook/litho/tree/master/sample).

To build and run (on an attached device/emulator) the sample app, execute

    $ buck fetch sample
    $ buck install -r sample

or, if you prefer Gradle,

    $ ./gradlew :sample:installDebug

## Contributing
For pull requests, please see our [CONTRIBUTING](CONTRIBUTING.md) guide.

See our [issues](https://github.com/facebook/litho/issues/) page for ideas on how to contribute or to let us know of any problems.

Please also read our [Coding Style](http://fblitho.com/docs/best-practices#coding-style) and [Code of Conduct](https://code.facebook.com/codeofconduct) before you contribute.

## License

Litho is BSD-licensed. We also provide an additional patent grant.
