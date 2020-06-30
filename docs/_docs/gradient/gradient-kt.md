Litho provides the `ComparableGradientDrawable` class which we can use to set a gradient background.
`ComparableGradientDrawable` implements [ComparableDrawable](/javadoc/com/facebook/litho/drawable/ComparableDrawable.html){:target="\_blank"} that makes subsequent mounting more efficient.

```kotlin
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import com.facebook.litho.drawable.ComparableGradientDrawable

MyComponent.create(c)
  .background(
    ComparableGradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        listOf(
            Color.GREEN,
            Color.BLUE
        ).toIntArray()
    )
  )
```
