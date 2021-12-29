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

package com.facebook.samples.litho.java.communicating;

import android.os.Bundle;
import android.widget.Toast;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.samples.litho.NavigatableDemoActivity;

public class CommunicatingFromChildToParent extends NavigatableDemoActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // start_define_observer
    final ComponentContext c = new ComponentContext(this);
    setContentView(
        LithoView.create(
            c,
            ParentComponentReceivesEventFromChild.create(c)
                .observer(
                    new ComponentEventObserver() {
                      @Override
                      public void onComponentClicked() {
                        Toast.makeText(
                                c.getAndroidContext(),
                                "Activity received event from child",
                                Toast.LENGTH_SHORT)
                            .show();
                      }
                    })
                .build()));
  }

  public interface ComponentEventObserver {
    void onComponentClicked();
  }

  // end_define_observer
}
