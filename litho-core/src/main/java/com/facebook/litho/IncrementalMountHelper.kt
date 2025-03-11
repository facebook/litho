/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho

import androidx.core.view.ViewCompat
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import androidx.viewpager2.widget.ViewPager2
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.ConcurrentModificationException

/**
 * Class that helps with incremental mounting. In particular, we need to account for ViewPagers as
 * we have no reliable hook for knowing when they are changing their content (and we should
 * therefore potentially be mounting).
 */
internal class IncrementalMountHelper(private val componentTree: ComponentTree) {

  private val viewPagerListeners: MutableList<ViewPagerListener> = ArrayList(2)
  private var isViewPager2Supported: Boolean = false
  private var viewPager2Listeners: MutableList<ViewPager2Listener>? = null

  fun onAttach(lithoView: LithoView) {
    if (!componentTree.isIncrementalMountEnabled) {
      return
    }
    if (lithoView.configuration?.enableIMHelperForViewPager2 == true) {
      isViewPager2Supported = true
      if (viewPager2Listeners == null) {
        viewPager2Listeners = ArrayList(2)
      }
    }

    // ViewPager does not give its child views any callbacks when it moves content onto the screen,
    // so we need to attach a listener to give us the information that we require.
    var viewParent = lithoView.parent
    while (viewParent != null) {
      if (viewParent is ViewPager) {
        val viewPager = viewParent
        val viewPagerListener = ViewPagerListener(componentTree, viewPager)

        // We want to add the listener immediately, since otherwise we might navigate to a
        // new tab in the ViewPager in this frame, and not mount the content. However, it is
        // possible that we are adding a listener here because its parent is being mounted due to
        // the ViewPager being scrolled (imagine a Recycler that is now on the screen and has to
        // mount a child view). In those cases adding the listener for the child will get a
        // ConcurrentModificationException, so we post it instead.
        try {
          viewPager.addOnPageChangeListener(viewPagerListener)
        } catch (e: ConcurrentModificationException) {
          ViewCompat.postOnAnimation(viewPager) {
            viewPager.addOnPageChangeListener(viewPagerListener)
          }
        }
        viewPagerListeners.add(viewPagerListener)
      } else if (isViewPager2Supported && (viewParent is ViewPager2)) {
        val viewPager2 = viewParent
        val viewPager2Listener = ViewPager2Listener(componentTree, viewPager2)

        try {
          viewPager2.registerOnPageChangeCallback(viewPager2Listener)
        } catch (e: ConcurrentModificationException) {
          viewPager2.postOnAnimation { viewPager2.registerOnPageChangeCallback(viewPager2Listener) }
        }
        viewPager2Listeners?.add(viewPager2Listener)
      }
      viewParent = viewParent.parent
    }
  }

  fun onDetach(lithoView: LithoView) {
    for (listener in viewPagerListeners) {
      listener.release()
    }
    viewPagerListeners.clear()
    if (isViewPager2Supported) {
      viewPager2Listeners?.let { listeners ->
        for (listener in listeners) {
          listener.release()
        }
        listeners.clear()
      }
    }
  }

  private class ViewPagerListener(componentTree: ComponentTree, viewPager: ViewPager) :
      SimpleOnPageChangeListener() {

    private val componentTree: WeakReference<ComponentTree> = WeakReference(componentTree)
    private val viewPager: WeakReference<ViewPager> = WeakReference(viewPager)

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
      componentTree.get()?.lithoView?.notifyVisibleBoundsChanged()
    }

    fun release() {
      componentTree.clear()
      val viewPager = viewPager.get() ?: return
      ViewCompat.postOnAnimation(viewPager) {
        viewPager.removeOnPageChangeListener(this@ViewPagerListener)
      }
    }
  }

  private class ViewPager2Listener(componentTree: ComponentTree, viewPager2: ViewPager2) :
      ViewPager2.OnPageChangeCallback() {

    private val componentTree: WeakReference<ComponentTree> = WeakReference(componentTree)
    private val viewPager2: WeakReference<ViewPager2> = WeakReference(viewPager2)

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
      componentTree.get()?.lithoView?.notifyVisibleBoundsChanged()
    }

    fun release() {
      componentTree.clear()
      val viewPager2 = viewPager2.get() ?: return
      viewPager2.postOnAnimation {
        viewPager2.unregisterOnPageChangeCallback(this@ViewPager2Listener)
      }
    }
  }
}
