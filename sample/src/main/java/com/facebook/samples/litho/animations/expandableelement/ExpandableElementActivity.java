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

package com.facebook.samples.litho.animations.expandableelement;

import android.os.Bundle;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.samples.litho.NavigatableDemoActivity;
import java.util.Arrays;

public class ExpandableElementActivity extends NavigatableDemoActivity {

  public static final Message[] MESSAGES =
      new Message[] {
        new Message(
            true,
            "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque "
                + "laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi "
                + "architecto beatae vitae dicta sunt explicabo",
            true,
            "DEC 25 AT 9:55"),
        new Message(
            false,
            "Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit",
            true,
            "DEC 25 AT 9:58"),
        new Message(
            false,
            "sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt.",
            true,
            "DEC 25 AT 9:59"),
        new Message(true, "Neque porro quisquam est", true, "DEC 25 AT 9:59"),
        new Message(false, "qui dolorem ipsum quia dolor sit amet", true, "DEC 25 AT 10:01"),
        new Message(true, "consectetur, adipisci velit", true, "DEC 25 AT 10:02"),
        new Message(
            true,
            "sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam "
                + "quaerat voluptatem",
            true,
            "DEC 25 AT 10:07"),
        new Message(
            true,
            "Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit "
                + "laboriosam",
            true,
            "DEC 25 AT 10:11"),
        new Message(false, "nisi ut aliquid ex ea commodi consequatur?", true, "DEC 25 AT 10:16"),
        new Message(
            true,
            "Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil"
                + " molestiae consequatur",
            true,
            "DEC 25 AT 10:21"),
        new Message(
            false,
            "vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?",
            false,
            "DEC 25 AT 10:25"),
        new Message(
            false,
            "At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis "
                + "praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias "
                + "excepturi sint occaecati cupiditate non provident, similique sunt in culpa qui "
                + "officia deserunt mollitia animi, id est laborum et dolorum fuga.",
            false,
            "DEC 25 AT 10:29"),
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final LithoView lithoView =
        LithoView.create(
            this,
            ExpandableElementRootComponent.create(new ComponentContext(this))
                .initialMessages(Arrays.asList(MESSAGES))
                .build());
    setContentView(lithoView);
  }
}
