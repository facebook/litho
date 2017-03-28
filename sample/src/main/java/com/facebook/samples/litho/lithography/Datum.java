// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.litho.lithography;

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;

/**
 * This is a interface for a piece of data that defines a component to be rendered in the feed.
 * Typically the datum would hold some intrinsic data (Strings or others) and use them to create
 * the component.
 */
public interface Datum {

  Component createComponent(ComponentContext c);
}
