/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.samples.litho.staticscroll.horizontalscroll;

import android.util.Pair;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.HorizontalScroll;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@LayoutSpec
public class HorizontalScrollRootComponentSpec {

  @OnCreateInitialState
  static void createInitialState(
      ComponentContext c,
      StateValue<ImmutableList<Pair<String, Integer>>> items,
      StateValue<AtomicInteger> prependCounter,
      StateValue<AtomicInteger> appendCounter) {
    final List<Pair<String, Integer>> initialItems = new ArrayList<>();
    initialItems.add(new Pair<>("Coral", 0xFFFF7F50));
    initialItems.add(new Pair<>("Ivory", 0xFFFFFFF0));
    initialItems.add(new Pair<>("PeachPuff", 0xFFFFDAB9));
    initialItems.add(new Pair<>("LightPink", 0xFFFFB6C1));
    initialItems.add(new Pair<>("LavenderBlush", 0xFFFFF0F5));
    initialItems.add(new Pair<>("Gold", 0xFFFFD700));
    initialItems.add(new Pair<>("BlanchedAlmond", 0xFFFFEBCD));
    initialItems.add(new Pair<>("FloralWhite", 0xFFFFFAF0));
    initialItems.add(new Pair<>("Moccasin", 0xFFFFE4B5));
    initialItems.add(new Pair<>("LightYellow", 0xFFFFFFE0));
    items.set(new ImmutableList.Builder<Pair<String, Integer>>().addAll(initialItems).build());
    prependCounter.set(new AtomicInteger(0));
    appendCounter.set(new AtomicInteger(0));
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @State ImmutableList<Pair<String, Integer>> items) {
    return Column.create(c)
        .child(
            Row.create(c)
                .paddingDip(YogaEdge.VERTICAL, 10)
                .child(
                    Text.create(c)
                        .paddingDip(YogaEdge.RIGHT, 10)
                        .alignSelf(YogaAlign.CENTER)
                        .clickHandler(HorizontalScrollRootComponent.onClick(c, true))
                        .text("PREPEND")
                        .textSizeSp(20))
                .child(
                    Text.create(c)
                        .paddingDip(YogaEdge.LEFT, 10)
                        .alignSelf(YogaAlign.CENTER)
                        .clickHandler(HorizontalScrollRootComponent.onClick(c, false))
                        .text("APPEND")
                        .textSizeSp(20)))
        .child(HorizontalScroll.create(c).contentProps(createHorizonalScrollChildren(c, items)))
        .build();
  }

  private static Component createHorizonalScrollChildren(
      ComponentContext c, List<Pair<String, Integer>> items) {
    final Row.Builder rowBuilder = Row.create(c);
    for (Pair<String, Integer> colorItem : items) {
      rowBuilder.child(
          Row.create(c)
              .paddingDip(YogaEdge.ALL, 10)
              .backgroundColor(colorItem.second)
              .child(
                  Text.create(c)
                      .text(colorItem.first)
                      .textSizeSp(20)
                      .alignSelf(YogaAlign.CENTER)
                      .heightDip(100)));
    }
    return rowBuilder.build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(
      ComponentContext c,
      @State AtomicInteger prependCounter,
      @State AtomicInteger appendCounter,
      @State ImmutableList<Pair<String, Integer>> items,
      @Param boolean isPrepend) {
    final ArrayList<Pair<String, Integer>> updatedItems = new ArrayList<>(items);
    if (isPrepend) {
      int counter = prependCounter.getAndAdd(1);
      updatedItems.add(0, new Pair<>("Prepend#" + counter, 0xFF7CFC00));
    } else {
      int counter = appendCounter.getAndAdd(1);
      updatedItems.add(new Pair<>("Append#" + counter, 0xFF6495ED));
    }
    HorizontalScrollRootComponent.updateItems(
        c, new ImmutableList.Builder<Pair<String, Integer>>().addAll(updatedItems).build());
  }

  @OnUpdateState
  static void updateItems(
      StateValue<ImmutableList<Pair<String, Integer>>> items,
      @Param ImmutableList<Pair<String, Integer>> updatedItems) {
    items.set(updatedItems);
  }
}
