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

package com.facebook.samples.litho.changesetdebug;

public class DataModel {

  private String data;
  private int id;
  private boolean misSelected;

  public DataModel(String data, int id) {
    this.data = data;
    this.id = id;
  }

  public void setSelected(boolean isSelected) {
    misSelected = isSelected;
  }

  public boolean isSelected() {
    return misSelected;
  }

  String getData() {
    return data;
  }

  int getId() {
    return id;
  }

  @Override
  public String toString() {
    return data;
  }
}
