// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.litho;

/**
 * An interfaced used by ComponentContext to notify the holder of a Litho hierarchy that as a result
 * of error handling the root of the tree should change to a Litho component representing the error.
 */
public interface ErrorComponentReceiver {
  void onErrorComponent(Component component);
}
