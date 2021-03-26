---
id: mount-extensions
title: Mount Extensions
---

Mount Extensions are a way of modularising Litho’s default behaviour, as it’s being changed from a monolithic framework to a modular framework that’s split into the rendering engine (RenderCore) and extensions for custom behaviour.
Mount Extensions are plug-in classes which can be enabled on a LithoView to add custom behaviour that RenderCore doesn’t implement. Mount Extensions can alter mount behaviour or can process information at mount time.

The RenderCore and Mount Extensions implementation of Litho is currently under the test and some features may not work as expected.

Litho provides features which are built into the the core of the framework, without the possibility of turning them off or customising behaviour. Such features include animations, incremental mount or dispatching visibility events. With Mount Extensions, the implementation of these features is extracted out of the core framework into separate extensions which work independently:
* IncrementalMountExtension
* TransitionsExtension
* VisibilityExtension

There are three main concepts related to the MountExtensions API:


## MountExtension

MountExtension: a class for customising mount behaviour. By default, all mount items are mounted if their host (the LithoView) is intersecting the viewport.

### Acquiring mount references

A Mount Extension can alter that behaviour by acquiring or releasing a mount reference for an item. Acquiring a mount reference means that the extension wants the item to be mounted. When the extension is no longer interested in keeping the item mounted, it can release the reference it previously acquired.
For example, the IncrementalMountExtension will only acquire a mount reference for items which are themselves visible in the viewport and will not acquire for items that are off-screen even if their host is visible.
Independently, the Animations Extension could acquire mount reference for items that are not visible on screen if their parent host is animating.
An extension can only release mount references for items it previously acquired and it has no information about whether other extensions have acquired an item.

Let’s take a look at the MountExtension API:


```java
public class MountExtension<Input> {

   public void registerToDelegate(MountDelegate mountDelegate) {
   }

    /**
   * Called for setting up input on the extension before mounting.
   *
   * @param input The new input the extension should use.
   */
  public void beforeMount(Input input, @Nullable Rect localVisibleRect) {}

  /** Called after mounting is finished. */
  public void afterMount() {}

  /** Called when the visible bounds of the Host change. */
  public void onVisibleBoundsChanged(@Nullable Rect localVisibleRect) {}

  /** Called after all the Host's children have been unmounted. */
  public void onUnmount() {}

  /** Called after all the Host's children have been unbound. */
  public void onUnbind() {}

  /** Called to request that this item should be mounted.
   * If isMounting is true, it will be immediately mounted if it's not mounted already.
   * Otherwise, it will be mounted on the next mount pass.
  */
  public void acquireMountRef(long nodeId, int index, boolean isMounting) {}

  /**
   * Called to release a mount referece and indicate that this extension
   * does not need the item to be mounted anymore.
   * If isMounting is true and the item's mount reference is no longer acquired by
   * any extension, it will be immediately unmounted.
   */
  public void releaseMountRef(long nodeId, int i, boolean isMounting) {
}
```


The Input is a type that represents the data for the Mount Extension to process. For example, in the case of VisibilityOutputsExtension, the Input implementation provides information about all the bounds and visibility event handlers for the Components.

## MountDelegate

A MountDelegate is a convenience class to manage all the extensions associated with a particular host. The MountDelegate has the list of all the enabled extensions which can alter mount behaviour. It aggregates the results of acquiring and releasing mount references by all the extensions and it notifies the MountDelegateTarget when an item needs to be mounted or unmounted.
The MountDelegate sums up the acquired mount references for all the items. It increases the total count when one of the extensions calls `acquireMountRef` and it decreases the total when `releaseMountRef` is called. When the total is positive, meaning at least one extension needs to mount an item, the MountDelegateTarget is notified and the item is mounted. If the total count reaches 0, the item no longer needs to be mounted by any extension so it will be unmounted.


## MountDelegateTarget

The MountDelegateTarget is a class that’s capable of creating, mounting and unmounting mount items. In Litho, the MountDelegateTarget implementation is the MountState. The MountDelegateTarget has a MountDelegate reference which it can query to check an item’s mount reference count and decide whether it should be mounted, unmounted or updated.
The MountDelegateTarget is also notified if an extension requires an item to be mounted or unmounted immediately. A MountExtension can influence what is mounted, but the MountDelegateTarget performs the mount operation.


```java
public interface MountDelegateTarget {

  /**
  * Can be called by a registered extension to request mount for a node.
  */
  void notifyMount(final int position);

 /**
  * Can be called by a registered extension to request unmount for a node.
  */
  void notifyUnmount(int position);


  /**
  * Is called by a host to request mount when a new layout tree is available.
  */
  void mount(RenderTree renderTree);

  void registerMountDelegateExtension(MountExtension mountExtension);

  // Check javadocs for the full API
}
```

## Handling new layout results

The host which is responsible for calling `MountState.mount()` (in our case, the LithoView) will call the appropriate callbacks on the list of enabled extensions.

For example, when a new layout for the LithoView’s ComponentTree is calculated, LithoView needs to mount the content for the new layout and it will perform the following sequence:


```java
private void mount(LayoutState layoutState, @Nullable Rect currentVisibleArea) {
  for (MountExtension mountExtension: enabledExtensions) {
    extension.beforeMount(layoutState, currentVisibleArea);
   }

  mountDelegateTarget.mount(layoutState.toRenderTree());

  for (MountExtension mountExtension: enabledExtensions) {
    extension.afterMount();
  }
}

```



## Handling position in viewport changes

Some Mount Extensions also need to process information when the host only changes visible bounds, even if no new layout result needs to be mounted. For example, the `VisibilityOutputsExtension` needs to listen to the host’s visible bounds changing on every scrolling frame and check if any items changed visibility status to dispatch events.
In that case, the host performs the following sequence:


```
for (MountExtension mountExtension: enabledExtensions) {
  mountExtension.onVisibleBoundsChanged(currentVisibleArea);
}
```

Since the layout result has not changed in this case, we don’t need to send a new input to process - it was already provided by calling `beforeMount`.


## Example: custom visibility event processing

At the moment, the extensions implementation and the usage of a MountDelegateTarget in Litho are still being tested and is unstable - there’s no API yet to provide a custom extension. Until the extension implementations and the integration between the LithoView and the MountDelegateTarget have been stabilised, we don’t recommend using this and we won’t provide an API to enable custom extensions on the LithoView.

The current implementation of MountState is a hybrid which uses the VisibilityOutputsExtension for visibility events processing during mount, but no MountDelegateTarget is being used. The extension’s callbacks are called manually by MountState - this is not the end state we want, but it’s an incremental step towards using extensions for all mount-time capabilities.
For this hybrid state, we can expose a test API to swap out the default implementation of the VisibilityOutputsExtension with a custom implementation. For example, if you want to receive visibility events even if your items are not visible on screen, but their host is visible, you can implement that behaviour in a custom visibility extension and pass that to the LithoView to use and override the default visibility behaviour.

Demo: https://github.com/facebook/litho/pull/714
