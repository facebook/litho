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

package com.facebook.samples.litho.errors;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;

public class ListRow {
  public final String title;
  public final String subtitle;

  public ListRow(String title, String subtitle) {
    this.title = title;
    this.subtitle = subtitle;
  }

  public Component createComponent(ComponentContext c) {
    return ErrorBoundary.create(c).child(ListRowComponent.create(c).row(this).build()).build();
  }
}
