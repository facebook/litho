/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.stetho;

import android.graphics.Rect;
import android.view.View;

import com.facebook.litho.DebugComponent;
import com.facebook.litho.LithoView;
import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.ChainedDescriptor;
import com.facebook.stetho.inspector.elements.ComputedStyleAccumulator;
import com.facebook.stetho.inspector.elements.Descriptor;
import com.facebook.stetho.inspector.elements.NodeType;
import com.facebook.stetho.inspector.elements.StyleAccumulator;
import com.facebook.stetho.inspector.elements.StyleRuleNameAccumulator;
import com.facebook.stetho.inspector.elements.android.HighlightableDescriptor;

/**
 * Exposes LithoView to the stetho elements inspector. It ensures that the view children
 * of a LithoView are not exposed (as they would if we used the ViewGroupDescriptor) as
 * they are an implementation detail of the framework (ComponentHost etc.).
 */
public final class LithoViewDescriptor
    extends Descriptor<LithoView>
    implements ChainedDescriptor<LithoView>, HighlightableDescriptor<LithoView> {

  private Descriptor<? super LithoView> mSuper;

  @Override
  public void setSuper(Descriptor<? super LithoView> superDescriptor) {
    if (superDescriptor == null) {
      throw new IllegalArgumentException("Super descriptor must not be null.");
    }

    if (superDescriptor != mSuper) {
      if (mSuper != null) {
        throw new IllegalStateException("Super descriptor has already been set.");
      }
      mSuper = superDescriptor;
    }
  }

  @Override
  public void hook(LithoView element) {
    verifyThreadAccess();
    mSuper.hook(element);
  }

  @Override
  public void unhook(LithoView element) {
    verifyThreadAccess();
    mSuper.unhook(element);
  }

  @Override
  public NodeType getNodeType(LithoView element) {
    return mSuper.getNodeType(element);
  }

  @Override
  public String getNodeName(LithoView element) {
    return mSuper.getNodeName(element);
  }

  @Override
  public String getLocalName(LithoView element) {
    return mSuper.getLocalName(element);
  }

  @Override
  public String getNodeValue(LithoView element) {
    return mSuper.getNodeValue(element);
  }

  @Override
  public void getAttributes(LithoView element, AttributeAccumulator attributes) {
    mSuper.getAttributes(element, attributes);
  }

  @Override
  public void setAttributesAsText(LithoView element, String text) {
    mSuper.setAttributesAsText(element, text);
  }

  @Override
  public void getStyleRuleNames(LithoView element, StyleRuleNameAccumulator accumulator) {
    mSuper.getStyleRuleNames(element, accumulator);
  }

  @Override
  public void getStyles(LithoView element, String ruleName, StyleAccumulator accumulator) {
    mSuper.getStyles(element, ruleName, accumulator);
  }

  @Override
  public void setStyle(LithoView element, String ruleName, String name, String value) {
    mSuper.setStyle(element, ruleName, name, value);
  }

  @Override
  public void getComputedStyles(LithoView element, ComputedStyleAccumulator accumulator) {
    mSuper.getComputedStyles(element, accumulator);
  }

  @Override
  public void getChildren(LithoView element, Accumulator<Object> children) {
    // LithoView is a view group but we explicitly do not want to call mSuper.getChildren()
    // here as we don't want to render children of LithoView and instead want to render
    // the component hierarchy. View children of LithoView are of type ComponentHost which is
    // an implementation detail of the Components framework.

    final DebugComponent component = DebugComponent.getRootInstance(element);
    if (component != null) {
      children.store(component);
    }
  }

  @Override
  public View getViewAndBoundsForHighlighting(LithoView element, Rect bounds) {
    return element;
  }

  @Override
  public Object getElementToHighlightAtPosition(LithoView element, int x, int y, Rect bounds) {
    final StethoHitTestAccumulator hitTestAccumulator = new StethoHitTestAccumulator(bounds, x, y);
    getChildren(element, hitTestAccumulator);

    final Object childElement = hitTestAccumulator.getElement();
    if (childElement != null) {
      return childElement;
    } else {
      return element;
    }
  }
}
