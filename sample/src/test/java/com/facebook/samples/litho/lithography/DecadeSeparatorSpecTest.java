package com.facebook.samples.litho.lithography;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.SubComponent;
import com.facebook.litho.testing.assertj.ComponentAssert;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class DecadeSeparatorSpecTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  @Test
  public void testSubComponentsWithManualExtraction() {
    final ComponentContext c = mComponentsRule.getContext();
    final Component<DecadeSeparator> component =
        DecadeSeparator.create(c).decade(new Decade(2010)).build();

    ComponentAssert.assertThat(c, component).extractingSubComponents(c).hasSize(3);

    ComponentAssert.assertThat(c, component).hasSubComponents(SubComponent.of(Text.class));
  }
}
