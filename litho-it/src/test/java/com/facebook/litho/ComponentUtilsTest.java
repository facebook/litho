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

import com.facebook.litho.annotations.Comparable;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.Arrays;
import java.util.Collection;
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
  public void hasEquivalentFieldsArrayIntPropTest() {
    mC1.propArrayInt = new int[] {2, 5, 6};
    mC2.propArrayInt = new int[] {2, 5, 6};
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.propArrayInt = new int[] {2, 3};
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  @Test
  public void hasEquivalentFieldsArrayCharPropTest() {
    mC1.propArrayChar = new char[] {'a', 'c', '5'};
    mC2.propArrayChar = new char[] {'a', 'c', '5'};
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.propArrayChar = new char[] {'a', 'c'};
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
  public void hasEquivalentFieldsBytePropTest() {
    mC1.propByte = 1;
    mC2.propByte = 1;
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.propByte = 2;
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
  public void hasEquivalentFieldsIntPropTest() {
    mC1.propInt = 3;
    mC2.propInt = 3;
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.propInt = 2;
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  @Test
  public void hasEquivalentFieldsLongPropTest() {
    mC1.propLong = 3;
    mC2.propLong = 3;
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.propLong = 2;
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  @Test
  public void hasEquivalentFieldsBooleanPropTest() {
    mC1.propBoolean = true;
    mC2.propBoolean = true;
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.propBoolean = false;
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  @Test
  public void hasEquivalentFieldsIntBoxedPropTest() {
    mC1.propIntBoxed = new Integer(3);
    mC2.propIntBoxed = new Integer(3);
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.propIntBoxed = new Integer(2);
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();

    mC2.propIntBoxed = null;
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();
  }

  @Test
  public void hasEquivalentFieldsStringPropTest() {
    mC1.propString = "string";
    mC2.propString = "string";
    assertThat(hasEquivalentFields(mC1, mC2)).isTrue();

    mC2.propString = "bla";
    assertThat(hasEquivalentFields(mC1, mC2)).isFalse();

    mC2.propString = null;
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

  private static class ComponentTest extends Component {
    @Comparable(type = Comparable.ARRAY)
    int[] propArrayInt;

    @Comparable(type = Comparable.ARRAY)
    char[] propArrayChar;

    @Comparable(type = Comparable.DOUBLE)
    double propDouble;

    @Comparable(type = Comparable.FLOAT)
    float propFloat;

    @Comparable(type = Comparable.PRIMITIVE)
    char propChar;

    @Comparable(type = Comparable.PRIMITIVE)
    byte propByte;

    @Comparable(type = Comparable.PRIMITIVE)
    short propShort;

    @Comparable(type = Comparable.PRIMITIVE)
    int propInt;

    @Comparable(type = Comparable.PRIMITIVE)
    long propLong;

    @Comparable(type = Comparable.PRIMITIVE)
    boolean propBoolean;

    @Comparable(type = Comparable.OTHER)
    Integer propIntBoxed;

    @Comparable(type = Comparable.OTHER)
    String propString;

    @Comparable(type = Comparable.COLLECTION_COMPLEVEL_0)
    Collection<String> propCollection;

    @Comparable(type = Comparable.COLLECTION_COMPLEVEL_1)
    Collection<Component> propCollectionWithComponents;

    @Comparable(type = Comparable.COMPONENT)
    Component propComponent;

    @Comparable(type = Comparable.EVENT_HANDLER)
    EventHandler propEventHandler;

    @Comparable(type = Comparable.OTHER)
    Object treePropObject;

    @Comparable(type = Comparable.STATE_CONTAINER)
    StateTest stateContainer = new StateTest();

    protected ComponentTest() {
      super("test");
    }
  }

  private static class StateTest extends StateContainer {
    @Comparable(type = Comparable.PRIMITIVE)
    boolean state1;

    @Comparable(type = Comparable.FLOAT)
    float state2;

    StateTest(boolean state1, float state2) {
      this.state1 = state1;
      this.state2 = state2;
    }

    StateTest() {}

    @Override
    public void applyStateUpdate(StateUpdate stateUpdate) {}
  }
}
