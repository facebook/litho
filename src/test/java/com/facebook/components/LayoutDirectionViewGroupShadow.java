// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.view.ViewGroup;

import org.robolectric.annotation.Implements;

/**
 * Robolectric shadow view does not support layout direction so we must implement our custom shadow.
 * We must have ViewGroup and View shadows as Robolectric forces us to have the whole hierarchy.
 */
@Implements(ViewGroup.class)
public class LayoutDirectionViewGroupShadow extends LayoutDirectionViewShadow {

}
