```java
import android.app.Activity;
import android.os.Bundle;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentInfo;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentView;
import com.facebook.litho.widget.Text;

public class SampleActivity extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentView componentView = new ComponentView(this);
    final ComponentContext context = new ComponentContext(this);

    final Component text = Text.create(context)
        .text("Hello, World!")
        .build();
    final ComponentTree componentTree = ComponentTree.create(context, text)
        .build();

    componentView.setComponent(componentTree);

    setContentView(componentView);
  }
}
```
