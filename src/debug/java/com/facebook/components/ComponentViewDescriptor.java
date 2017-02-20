// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.graphics.Rect;
import android.view.View;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.ChainedDescriptor;
import com.facebook.stetho.inspector.elements.ComputedStyleAccumulator;
import com.facebook.stetho.inspector.elements.Descriptor;
import com.facebook.stetho.inspector.elements.NodeType;
import com.facebook.stetho.inspector.elements.StyleAccumulator;
import com.facebook.stetho.inspector.elements.android.HighlightableDescriptor;

/**
 * Exposes ComponentView to the stetho elements inspector. It ensures that the view children
 * of a ComponentView are not exposed (as they would if we used the ViewGroupDescriptor) as
 * they are an implementation detail of the framework (ComponentHost etc.).
 */
public final class ComponentViewDescriptor
    extends Descriptor<ComponentView>
    implements ChainedDescriptor<ComponentView>, HighlightableDescriptor<ComponentView> {

  private Descriptor<? super ComponentView> mSuper;

  @Override
  public void setSuper(Descriptor<? super ComponentView> superDescriptor) {
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
  public void hook(ComponentView element) {
    verifyThreadAccess();
    mSuper.hook(element);
  }

  @Override
  public void unhook(ComponentView element) {
    verifyThreadAccess();
    mSuper.unhook(element);
  }

  @Override
  public NodeType getNodeType(ComponentView element) {
    return mSuper.getNodeType(element);
  }

  @Override
  public String getNodeName(ComponentView element) {
    return mSuper.getNodeName(element);
  }

  @Override
  public String getLocalName(ComponentView element) {
    return mSuper.getLocalName(element);
  }

  @Override
  public String getNodeValue(ComponentView element) {
    return mSuper.getNodeValue(element);
  }

  @Override
  public void getAttributes(ComponentView element, AttributeAccumulator attributes) {
    mSuper.getAttributes(element, attributes);
  }

  @Override
  public void setAttributesAsText(ComponentView element, String text) {
    mSuper.setAttributesAsText(element, text);
  }

  @Override
  public void getStyles(ComponentView element, StyleAccumulator styles) {
    mSuper.getStyles(element, styles);
  }

  @Override
  public void getComputedStyles(ComponentView element, ComputedStyleAccumulator accumulator) {
    mSuper.getComputedStyles(element, accumulator);
  }

  @Override
  public void getChildren(ComponentView element, Accumulator<Object> children) {
    // ComponentView is a view group but we explicitly do not want to call mSuper.getChildren()
    // here as we don't want to render children of ComponentView and instead want to render
    // the component hierarchy. View children of ComponentView are of type ComponentHost which is
    // an implementation detail of the Components framework.

    final ComponentTree component = element.getComponent();
    final LayoutState layoutState = component == null ? null : component.getMainThreadLayoutState();
    final InternalNode root = layoutState == null ? null : layoutState.getLayoutRoot();
    if (root != null) {
      final ComponentContext context = component.getContext();
      final ComponentsStethoManagerImpl stethoManager =
          (ComponentsStethoManagerImpl) context.getStethoManager();
      if (stethoManager == null) {
        // Stetho has not been attached previously in this session. Create a stetho manager
        // and re-render the tree using that manager before exposing children nodes to stetho.
        context.setStethoManager(new ComponentsStethoManagerImpl());
        element.forceRelayout();
      } else {
        final StethoInternalNode stethoInternalNode = stethoManager.getStethoInternalNode(root);
        children.store(stethoInternalNode);
      }
    }
  }

  @Override
  public View getViewAndBoundsForHighlighting(ComponentView element, Rect bounds) {
    return element;
  }
}
