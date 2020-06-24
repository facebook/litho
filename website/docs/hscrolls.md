---
id: hscrolls
title: Horizontal scrolls and height
---
import useBaseUrl from '@docusaurus/useBaseUrl';

Vertical scrolling lists usually fill the width of the screen so it's easy to measure them with a fixed size.
In this guide we'll talk about ways of measuring the height of a horizontal scrolling list (we'll call it h-scroll), which is not as trivial and different height settings have different performance implications.

To see how to create a horizontally scrolling list using a `RecyclerCollectionComponent`, check out [this](recycler-collection-component#null__horizontal-lists) guide.

### Setting the height of a horizontal `RecyclerCollectionComponent`
The code samples below are extracted from the [h-scroll height codelab](https://github.com/facebook/litho/tree/master/codelabs/hscroll-height).

You can set the height of horizontally scrolling `RecyclerCollectionComponent` in three ways.

**1) The most performant way: A fixed height is set on the H-Scroll component.**

In this case, the client knows the height of the h-scroll when it creates it. The height cannot be changed once the h-scroll gets measured.

Children of this h-scroll are measured with at most the height of the h-scroll; taller children will be clipped and smaller children will be positioned at the start of the h-scroll.

In Litho this is the most efficient way to set the height of an h-scroll and it's advisable to use this option whenever possible.

To do this, just set the height through the `height` prop on your `RecyclerCollectionComponent`:
```java
   @LayoutSpec
   object FixedHeightHscrollComponentSpec {

       @OnCreateLayout
       fun onCreateLayout(c: ComponentContext, @Prop colors: List<Int>): Component {
           return RecyclerCollectionComponent.create(c)
             .recyclerConfiguration(ListRecyclerConfiguration.create().orientation(OrientationHelper.HORIZONTAL).build())
             .section(DataDiffSection.create<Int>(SectionContext(c))
               .data(colors)
               .renderEventHandler(FixedHeightHscrollComponent.onRender(c))
               .build())
             .heightDip(150f)
             .build()
       }

       @OnEvent(RenderEvent::class)
       fun onRender(c: ComponentContext, @FromEvent model: Int): RenderInfo {
           return ComponentRenderInfo.create()
             .component(SolidColor.create(c).color(model).heightDip(100f).widthDip(100f))
             .build()
       }
   }
```
![fixedheight](/static/images/fixed-height-hscroll.png)

Notice the gray background is the actual bounds of the h-scroll, and the children have smaller heights.

**2) Height is not known when component is created: Let the h-scroll set its height to the height of the first item.**

In cases where the height of the h-scroll is not known at the time it is created, the height will be determined by measuring the first child of the h-scroll and setting that as the height of the h-scroll. This measurement happens once only, when the h-scroll is first measured, and the height cannot be changed after that. All other children heights will be measured with at most the height of the h-scroll and position at the start of the h-scroll.
To enable this, instead of passing a `height` prop on the `RecyclerCollectionComponent`, tell it through the `canMeasureRecycler` prop it should measure itself.

```java
@LayoutSpec
object MeasureFirstItemForHeightHscrollComponentSpec {

    @OnCreateLayout
    fun onCreateLayout(c: ComponentContext, @Prop colors: List<Int>): Component {

        return RecyclerCollectionComponent.create(c)
          .recyclerConfiguration(ListRecyclerConfiguration.create().orientation(OrientationHelper.HORIZONTAL).build())
          .section(DataDiffSection.create<Int>(SectionContext(c))
            .data(colors)
            .renderEventHandler(MeasureFirstItemForHeightHscrollComponent.onRender(c))
            .build())
          .canMeasureRecycler(true)
          .build()
    }

    @OnEvent(RenderEvent::class)
    fun onRender(c: ComponentContext, @FromEvent model: Int, @FromEvent index: Int): RenderInfo {
      if (index == 0) {
        return ComponentRenderInfo.create()
          .component(SolidColor.create(c).color(model).heightDip(100f).widthDip(100f))
          .build()
      }

      if (index == 1) {
         return ComponentRenderInfo.create()
           component(SolidColor.create(c).color(model).heightDip(200f).widthDip(100f))
           .build()
      }

      return ComponentRenderInfo.create()
        .component(SolidColor.create(c).color(model).heightDip(50f).widthDip(100f))
        .build()
    }
}
```
![canmeasure](/static/images/canmeasure.png)

In this case, the first child has a height of 100dip; the second child has a height of 200dip but it's cropped to fit the size of the h-scroll as determined by the first child. Once measured like this, the height cannot be changed.
The gray background represents the actual bounds of the h-scroll.
> Note that if you don't set a non-zero height on the `RecyclerCollectionComponent` and `canMeasureRecycler` is not enabled, your RecyclerCollectionComponent will end up with a height of 0.

**3) The underperformant way: Let the h-scroll dynamically change its height to fit the tallest item**

H-Scrolls can be configured to support items of different heights or remeasuring the height if the height of the children could change after the initial measurement. In this case, the initial height of the h-scroll is determined by the height of the tallest child.
Initial height: The initial height of the h-scroll is determined by the height of the tallest child.
Expanding more than the height of the h-scroll: If a child wants to expand to become taller than the current height of the h-scroll, the h-scroll will be remeasured with the new height of the child. Other items will not be remeasured.
Collapsing the highest child: If the child with the biggest height collapses, then the h-scroll will again determine what its height should be by remeasuring all the items.

> Enabling this option should be done only if absolutely needed and should especially be avoided for lists with infinite scrolling.

Measuring all the children to determine the tallest comes with a high performance cost, especially for infinite loading h-scrolls when the height needs to be remeasured every time new items are inserted.
If you must do this, you can pass your own [RecyclerConfiguration](/javadoc/com/facebook/litho/sections/widget/RecyclerConfiguration.html) to the `RecyclerCollectionComponent` and enable `hasDynamicItemHeight` on the [RecyclerBinderConfigurationer](/javadoc/com/facebook/litho/sections/widget/RecyclerBinderConfiguration.html) that is used to create the `RecyclerConfiguration`.

```java
RecyclerCollectionComponent.create(c)
          .recyclerConfiguration(ListRecyclerConfiguration.create()
              .recyclerBinderConfiguration(RecyclerBinderConfiguration.create().hasDynamicItemHeight(true).build())
              .orientation(OrientationHelper.HORIZONTAL).build())
          .section(DataDiffSection.create<Int>(SectionContext(c))
            .data(colors)
            .renderEventHandler(DynamicHeightHscrollComponent.onRender(c))
            .build())
          .canMeasureRecycler(true)
          .build()
```

In the video below you can see that the h-scroll will remeasure to always adjust height to accommodate the tallest item, but it won't collapse to fit a smaller maximum height.

<video loop="true" autoplay="true" controls="true" class="video" width="600px" height="100%">
  <source type="video/mp4" src={useBaseUrl("/videos/dynamicheight.mov")}></source>
  <p>Your browser does not support the video element.</p>
</video>
