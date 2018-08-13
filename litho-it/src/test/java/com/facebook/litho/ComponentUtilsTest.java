/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static com.facebook.litho.ComponentUtils.hasEquivalentFields;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.reference.Reference;
import com.facebook.litho.reference.ReferenceLifecycle;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class ComponentUtilsTest {

  private ComponentTest mC1;
  private ComponentTest mC2;

  @Before
  public void setUp() {
    mC1 = new ComponentTest();
    mC2 = new ComponentTest();
  }

  @Test
  public void levelOfComponentsInCollectionTest() {
    Field[] fields = CollectionObject.class.getDeclaredFields();

    assertThat(ComponentUtils.levelOfComponentsInCollection(fields[0].getGenericType()))
        .isEqualTo(1);
    assertThat(ComponentUtils.levelOfComponentsInCollection(fields[1].getGenericType()))
        .isEqualTo(2);
    assertThat(ComponentUtils.levelOfComponentsInCollection(fields[2].getGenericType()))
        .isEqualTo(0);
  }

  @Test
  public void hasEquivalentFieldsArrayPropTest() {
    mC1.propArray = new int[] {2, 5, 6};
    mC2.propArray = new int[] {2, 5, 6};
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.propArray = new int[] {2, 3};
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  @Test
  public void hasEquivalentFieldsStateContainersTest() {
    mC1.stateContainer = new StateTest(true, 3f);
    mC2.stateContainer = new StateTest(true, 3f);
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.stateContainer = new StateTest(true, 2f);
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  @Test
  public void hasEquivalentFieldsDoublePropTest() {
    mC1.propDouble = 2.0;
    mC2.propDouble = 2.0;
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.propDouble = 3.0;
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  @Test
  public void hasEquivalentFieldsFloatPropTest() {
    mC1.propFloat = 2f;
    mC2.propFloat = 2f;
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.propFloat = 3f;
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  @Test
  public void hasEquivalentFieldsCharPropTest() {
    mC1.propChar = 'c';
    mC2.propChar = 'c';
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.propChar = 'z';
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  @Test
  public void hasEquivalentFieldsShortPropTest() {
    mC1.propShort = 3;
    mC2.propShort = 3;
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.propShort = 2;
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  @Test
  public void hasEquivalentFieldsReferencePropTest() {
    mC1.propReference = new TestReference("aa");
    mC2.propReference = new TestReference("aa");
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.propReference = new TestReference("ab");
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  @Test
  public void hasEquivalentFieldsCollectionPropTest() {
    mC1.propCollection = Arrays.asList("1", "2", "3");
    mC2.propCollection = Arrays.asList("1", "2", "3");
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.propCollection = Arrays.asList("2", "3");
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  @Test
  public void hasEquivalentFieldsCollectionWithComponentsPropTest() {
    ComponentTest innerComponent11 = new ComponentTest();
    innerComponent11.propDouble = 2.0;
    ComponentTest innerComponent12 = new ComponentTest();
    innerComponent12.propDouble = 2.0;
    ComponentTest innerComponent21 = new ComponentTest();
    innerComponent21.propDouble = 2.0;
    ComponentTest innerComponent22 = new ComponentTest();
    innerComponent22.propDouble = 2.0;

    mC1.propCollectionWithComponents = Arrays.asList(innerComponent11, innerComponent12);
    mC2.propCollectionWithComponents = Arrays.asList(innerComponent21, innerComponent22);
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    innerComponent22.propDouble = 3.0;
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  @Test
  public void hasEquivalentFieldsComponentPropTest() {
    ComponentTest innerComponent1 = new ComponentTest();
    innerComponent1.propDouble = 2.0;
    ComponentTest innerComponent2 = new ComponentTest();
    innerComponent2.propDouble = 2.0;

    mC1.propComponent = innerComponent1;
    mC2.propComponent = innerComponent2;
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    innerComponent2.propDouble = 3.0;
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  @Test
  public void hasEquivalentFieldsEventHandlerPropTest() {
    // The first item of the params is skipped as explained in the EventHandler class.
    mC1.propEventHandler = new EventHandler(null, 3, new Object[] {"", "1"});
    mC2.propEventHandler = new EventHandler(null, 3, new Object[] {"", "1"});
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.propEventHandler = new EventHandler(null, 3, new Object[] {"", "2"});
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  @Test
  public void hasEquivalentFieldsTreePropTest() {
    mC1.treePropObject = new String("1");
    mC2.treePropObject = new String("1");
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.treePropObject = new String("2");
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  private static class CollectionObject {
    List<Component> arg0;
    List<Set<Component>> arg1;
    Set<List<List<Integer>>> arg2;
  }

  private static class ComponentTest extends Component {
    @Prop int[] propArray;
    @Prop double propDouble;
    @Prop float propFloat;
    @Prop char propChar;
    @Prop short propShort;
    @Prop Reference propReference;
    @Prop Collection<String> propCollection;
    @Prop Collection<Component> propCollectionWithComponents;
    @Prop Component propComponent;
    @Prop EventHandler propEventHandler;

    @TreeProp Object treePropObject;

    StateTest stateContainer = new StateTest();

    protected ComponentTest() {
      super("test");
    }

    @Override
    public boolean isEquivalentTo(Component other) {
      return hasEquivalentFields(this, other);
    }
  }

  private static class StateTest implements ComponentLifecycle.StateContainer {
    @State boolean state1;

    @State float state2;

    StateTest(boolean state1, float state2) {
      this.state1 = state1;
      this.state2 = state2;
    }

    StateTest() {}
  }

  private static class TestReference extends Reference {
    private String mVal;

    TestReference(String val) {
      super(
          new ReferenceLifecycle<String>() {
            @Override
            protected String onAcquire(Context context, Reference<String> reference) {
              return "";
            }
          });
      mVal = val;
    }

    @Override
    public boolean equals(Object o) {
      TestReference other = (TestReference) o;
      return this == o || mVal.equals(other.mVal);
    }

    @Override
    public String getSimpleName() {
      return "TestReference";
    }
  }
}
