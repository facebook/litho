/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.samples.litho.animations.sharedelements;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.transition.ChangeTransform;
import androidx.transition.TransitionSet;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;

public class SharedElementsFragmentActivity extends AppCompatActivity {

  private static final int CONTENT_VIEW_ID = ViewCompat.generateViewId();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final FrameLayout frame = new FrameLayout(this);
    frame.setId(CONTENT_VIEW_ID);
    setContentView(
        frame,
        new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

    if (savedInstanceState == null) {
      final Fragment fragment = new FirstFragment();
      getSupportFragmentManager().beginTransaction().add(CONTENT_VIEW_ID, fragment).commit();
    }
  }

  public static class FirstFragment extends Fragment {

    @Override
    public @Nullable View onCreateView(
        LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {

      Activity activity = getActivity();
      if (activity == null) {
        return null;
      }

      final ComponentContext c = new ComponentContext(activity);

      final View view = new View(activity);
      ViewCompat.setBackground(view, new ColorDrawable(Color.RED));
      ViewCompat.setTransitionName(view, "view");
      view.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              nextFragment(v, Color.RED);
            }
          });

      final LinearLayout layout = new LinearLayout(activity);
      layout.setLayoutParams(
          new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
      layout.setOrientation(LinearLayout.VERTICAL);

      TextView viewTitle = new TextView(activity);
      viewTitle.setText("View");
      layout.addView(viewTitle);
      layout.addView(view, 200, 200);

      layout.addView(new View(activity), 200, 200);

      TextView lithoViewTitle = new TextView(activity);
      lithoViewTitle.setText("LithoView (animating root component)");
      layout.addView(lithoViewTitle);
      Component boxInLithoViewComponent =
          BoxInLithoViewComponent.create(c).firstFragment(this).build();
      LithoView boxInLithoView = LithoView.create(activity, boxInLithoViewComponent);
      boxInLithoView.setLayoutParams(
          new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      layout.addView(boxInLithoView);

      layout.addView(new View(activity), 200, 200);
      TextView componentHostTitle = new TextView(activity);
      componentHostTitle.setText("ComponentHost (in a LithoView)");
      layout.addView(componentHostTitle);
      Component boxInComponentHostComponent =
          BoxInComponentHostComponent.create(c).firstFragment(this).build();
      LithoView boxInComponentHost = LithoView.create(activity, boxInComponentHostComponent);
      boxInComponentHost.setClipChildren(false);
      boxInComponentHost.setLayoutParams(
          new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      layout.addView(boxInComponentHost);

      layout.setClipChildren(false);

      return layout;
    }

    public void nextFragment(View view, int color) {
      final String transitionName = ViewCompat.getTransitionName(view);
      if (transitionName == null) {
        throw new IllegalArgumentException();
      }
      final Fragment fragment = new SecondFragment();
      final Bundle args = new Bundle();
      args.putString(SecondFragment.ARG_TRANSITION_NAME, transitionName);
      args.putInt(SecondFragment.ARG_COLOR, color);
      fragment.setArguments(args);

      final TransitionSet transitionSet =
          new TransitionSet()
              .addTransition(new ChangeTransform())
              .addTransition(new ComponentHostChangeBoundsTransition());
      fragment.setSharedElementEnterTransition(transitionSet);

      FragmentActivity activity = getActivity();
      if (activity == null) {
        return;
      }

      activity
          .getSupportFragmentManager()
          .beginTransaction()
          .addSharedElement(view, transitionName)
          .replace(CONTENT_VIEW_ID, fragment)
          .addToBackStack(null)
          .commit();
    }
  }

  public static class SecondFragment extends Fragment {

    public static final String ARG_COLOR = "COLOR";
    public static final String ARG_TRANSITION_NAME = "TRANSITION_NAME";

    @Override
    public @Nullable View onCreateView(
        LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {

      postponeEnterTransition();

      Bundle arguments = requireArguments();

      final @Nullable String transitionName = arguments.getString(ARG_TRANSITION_NAME);
      if (transitionName == null) {
        throw new IllegalArgumentException();
      }
      final @ColorInt int color = arguments.getInt(ARG_COLOR, Color.RED);

      FragmentActivity activity = getActivity();
      if (activity == null) {
        return null;
      }

      final ComponentContext c = new ComponentContext(activity);
      final Component component =
          Column.create(c)
              .paddingDip(YogaEdge.TOP, 100)
              .child(
                  Column.create(c)
                      .alignSelf(YogaAlign.CENTER)
                      .backgroundColor(color)
                      .widthDip(150)
                      .heightDip(150)
                      .transitionName(transitionName))
              .build();

      final LithoView lithoView = LithoView.create(activity, component);
      LithoView.OnDirtyMountListener dirtyMountListener =
          new LithoView.OnDirtyMountListener() {
            @Override
            public void onDirtyMount(LithoView view) {
              startPostponedEnterTransition();
            }
          };
      lithoView.setOnDirtyMountListener(dirtyMountListener);

      return lithoView;
    }
  }
}
