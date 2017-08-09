package com.facebook.samples.litho.lithography;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.InspectableComponent;
import com.facebook.litho.testing.SubComponent;
import com.facebook.litho.testing.assertj.ComponentAssert;
import com.facebook.litho.testing.assertj.SubComponentExtractor;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;

import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(ComponentsTestRunner.class)
public class DecadeSeparatorSpecTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  private Component<DecadeSeparator> mComponent;

  @Before
  public void setUp() {
    mComponent = DecadeSeparator.create(mComponentsRule.getContext()).decade(new Decade(2010)).build();
  }

  @Test
  public void testSubComponentsWithManualExtraction() {
    final ComponentContext c = mComponentsRule.getContext();
    ComponentAssert.assertThat(c, mComponent).extractingSubComponents(c).hasSize(3);
  }

  @Test
  public void testSubComponentByClass() {
    final ComponentContext c = mComponentsRule.getContext();
    ComponentAssert.assertThat(c, mComponent).hasSubComponents(SubComponent.of(Text.class));
  }

  @Test
  public void testSubComponentByClassWithExtraction() {
    final ComponentContext c = mComponentsRule.getContext();
    ComponentAssert.assertThat(c, mComponent).extracting(SubComponentExtractor.subComponents(c))
      .areExactly(1, new Condition<InspectableComponent>() {
        @Override
        public boolean matches(InspectableComponent value) {
          return value.getComponentClass() == Text.class;
        }
      });
  }
}
