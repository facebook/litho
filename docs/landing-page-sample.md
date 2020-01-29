```java
import android.app.Activity;
import android.os.Bundle;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.widget.Text;

public class SampleActivity extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final LithoView lithoView = new LithoView(this);
    final ComponentContext c = new ComponentContext(this);

    final Component text = Text.create(c)
        .text("Hello, World!")
        .build();
    final ComponentTree componentTree = ComponentTree.create(c, text)
        .build();

    lithoView.setComponent(componentTree);

    setContentView(lithoView);
  }
}
```
