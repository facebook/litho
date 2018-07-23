## Testing your Installation

You can test your install by adding a view created with Litho to an activity.

First, initialize `SoLoader`. Litho has a dependency on [SoLoader](https://github.com/facebook/SoLoader) to help load native libraries provided by the underlying layout engine, [Yoga](https://yogalayout.com/docs/). Your `Application` class is a good place to do this:

```kotlin
[MyApplication.kt]
class MyApplication: Application() {

  override fun onCreate() {
    super.onCreate()
    SoLoader.init(this, false)
  }
}
```

Then, add a predefined Litho `Text` widget to an activity that displays "Hello World!":

```kotlin
[MyActivity.kt]
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView

class MyActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val c = ComponentContext(this)

    val lithoView = LithoView.create(
        this /* context */,
        Text.create(c)
            .text("Hello, World!")
            .textSizeDip(50)
            .build())

    setContentView(lithoView)
  }
}
```

Now, when you run the app you should see "Hello World!" displayed on the screen.