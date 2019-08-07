/*
 * Copyright 2004-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.jni;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A thread which invokes the "destruct" routine for objects after they have been garbage collected.
 *
 * <p>An object which needs to be destructed should create a static subclass of {@link Destructor}.
 * Once the referent object is garbage collected, the DestructorThread will callback to the {@link
 * Destructor#destruct()} method.
 *
 * <p>The underlying thread in DestructorThread starts when the first Destructor is constructed and
 * then runs indefinitely.
 */
public class DestructorThread {

  /**
   * N.B The Destructor <b>SHOULD NOT</b> refer back to its referent object either explicitly or
   * implicitly (for example, as a non-static inner class). This will create a reference cycle where
   * the referent object will never be garbage collected.
   */
  public abstract static class Destructor extends PhantomReference<Object> {

    private Destructor next;
    private Destructor previous;

    public Destructor(Object referent) {
      super(referent, sReferenceQueue);
      sDestructorStack.push(this);
    }

    private Destructor() {
      super(null, sReferenceQueue);
    }

    /** Callback which is invoked when the original object has been garbage collected. */
    protected abstract void destruct();
  }

  /** A list to keep all active Destructors in memory confined to the Destructor thread. */
  private static DestructorList sDestructorList;
  /** A thread safe stack where new Destructors are placed before being add to sDestructorList. */
  private static DestructorStack sDestructorStack;

  private static ReferenceQueue sReferenceQueue;
  private static Thread sThread;

  static {
    sDestructorStack = new DestructorStack();
    sReferenceQueue = new ReferenceQueue();
    sDestructorList = new DestructorList();
    sThread =
        new Thread("HybridData DestructorThread") {
          @Override
          public void run() {
            while (true) {
              try {
                Destructor current = (Destructor) sReferenceQueue.remove();
                current.destruct();

                // If current is in the sDestructorStack,
                // transfer all the Destructors in the stack to the list.
                if (current.previous == null) {
                  sDestructorStack.transferAllToList();
                }

                DestructorList.drop(current);
              } catch (InterruptedException e) {
                // Continue. This thread should never be terminated.
              }
            }
          }
        };

    sThread.start();
  }

  private static class Terminus extends Destructor {
    @Override
    protected void destruct() {
      throw new IllegalStateException("Cannot destroy Terminus Destructor.");
    }
  }

  /** This is a thread safe, lock-free Treiber-like Stack of Destructors. */
  private static class DestructorStack {
    private AtomicReference<Destructor> mHead = new AtomicReference<>();

    public void push(Destructor newHead) {
      Destructor oldHead;
      do {
        oldHead = mHead.get();
        newHead.next = oldHead;
      } while (!mHead.compareAndSet(oldHead, newHead));
    }

    public void transferAllToList() {
      Destructor current = mHead.getAndSet(null);
      while (current != null) {
        Destructor next = current.next;
        sDestructorList.enqueue(current);
        current = next;
      }
    }
  }

  /** A doubly-linked list of Destructors. */
  private static class DestructorList {
    private Destructor mHead;

    public DestructorList() {
      mHead = new Terminus();
      mHead.next = new Terminus();
      mHead.next.previous = mHead;
    }

    public void enqueue(Destructor current) {
      current.next = mHead.next;
      mHead.next = current;

      current.next.previous = current;
      current.previous = mHead;
    }

    private static void drop(Destructor current) {
      current.next.previous = current.previous;
      current.previous.next = current.next;
    }
  }
}
