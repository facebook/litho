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

package com.facebook.samples.litho.dynamicprops;

import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.DynamicValue;
import com.facebook.litho.LithoView;
import com.facebook.samples.litho.NavigatableDemoActivity;
import com.facebook.samples.litho.R;
import java.util.Calendar;

public class DynamicPropsActivity extends NavigatableDemoActivity
    implements SeekBar.OnSeekBarChangeListener {
  private static final long DAY = 24 * 60 * 60 * 1000;

  private DynamicValue<Long> mTimeValue;
  private DynamicValue<Float> mAlphaValue;

  private SeekBar mSeekBar;
  private TextView mTimeLabel;
  private TextView mAlphaLabel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState == null) {
      mTimeValue = new DynamicValue<>(0L);
      mAlphaValue = new DynamicValue<>(0F);
    }

    setContentView(R.layout.activity_dynamic_props);

    mTimeLabel = findViewById(R.id.time);
    mAlphaLabel = findViewById(R.id.alpha);

    final LithoView lithoView = findViewById(R.id.lithoView);
    final ComponentContext c = new ComponentContext(this);
    final Component component =
        ClockComponent.create(c).time(mTimeValue).alpha(mAlphaValue).build();
    lithoView.setComponentTree(ComponentTree.create(c, component).build());

    mSeekBar = findViewById(R.id.seekBar);
    mSeekBar.setMax((int) DAY);
    mSeekBar.setOnSeekBarChangeListener(this);

    final SeekBar alphaSeekBar = findViewById(R.id.alphaSeekBar);
    alphaSeekBar.setMax(100);
    alphaSeekBar.setProgress(100);
    alphaSeekBar.setOnSeekBarChangeListener(this);

    reset(null);
    setAlpha(1F);
  }

  private void setTime(long time) {
    mTimeValue.set(time % ClockView.TWELVE_HOURS);
    mTimeLabel.setText(ClockView.getTimeString(time % DAY, false));
    mSeekBar.setProgress((int) time);
  }

  private void setAlpha(float alpha) {
    mAlphaLabel.setText(Float.toString(alpha));
    mAlphaValue.set(alpha);
  }

  public void reset(View v) {
    final Calendar calendar = Calendar.getInstance();

    final long now = calendar.getTimeInMillis();

    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    final long midnight = calendar.getTimeInMillis();

    final int sinceMidnight = (int) (now - midnight);
    setTime(sinceMidnight);
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    if (!fromUser) {
      return;
    }

    final int id = seekBar.getId();
    if (id == R.id.seekBar) {
      setTime(progress);
    } else if (id == R.id.alphaSeekBar) {
      setAlpha(progress / 100f);
    }
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {}

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {}
}
