---
docid: group-sections
title: GroupSection Specs
layout: docs
permalink: /docs/group-sections
---

A *group section spec* is used to structure your data into a hierarchy of Sections, each responsible for rendering its own chunk of data.

Group section specs are classes annotated with `@GroupSectionSpec`.
Implementing a `GroupSectionSpec` is very simple: you only need to write one method annotated with @OnCreateChildren. This method returns a tree of Sections that will have root in this `GroupSectionSpec`. The children can be Section instances created from other `GroupSectionSpec` classes or from [DiffSectionSpec](/docs/diff-sections) classes.

Let's have a look at how you would declare a simple list that contains a header followed by a list of Strings. We'll use a [SingleComponentSection](/docs/sections-building-blocks#singlecomponentsection) for the header and a [DataDiffSection](/docs/sections-building-blocks#datadiffsection) for the list of Strings and we'll combine these in a hierarchy.

```java
@GroupSectionSpec
class FooSectionSpec {

  @OnCreateChildren
  static Children onCreateChildren(
      SectionContext c,
      @Prop String headerTitle,
      @Prop List<String> data) {
    return Children.create()
        .child(
            SingleComponentSection.create(c)
                .component(
                    Text.create(c)
                        .text(headerTitle)
                        .build()))
        .child(
            DataDiffSection.<String>create(c)
                .data(data)
                .renderEventHandler(FooSection.onRender(c)))
        .build();
  }

  @OnEvent(RenderEvent.class)
  static ComponentRenderInfo onRender(
      SectionContext c,
      @FromEvent String item) {
    return ComponentRenderInfo.create(c)
        .component(
            Text.create(c)
                .text(item)
                .build())
        .build();
    }
}
```

Imagine a surface that has multiple such subsections consisting of a header and list of Strings. An example could be a list of contacts grouped alphabetically, delimited by headers showing the first letter of the name. You can easily achieve that by creating a `GroupSectionSpec` that has a `FooSection` child for every letter.

```java
@GroupSectionSpec
class BarSectionSpec {

  @OnCreateChildren
  static Children onCreateChildren(
      SectionContext c,
      @Prop List<String> data) {

      final Children.Builder builder = Children.create();

      for(char firstLetter = 'A'; firstLetter <= 'Z'; firstLetter++) {
          builder.child(FooSection.create(c)
              .key(String.valueOf(firstLetter))
              .headerTitle(String.valueOf(firstLetter))
              .data(getItemsForLetter(firstLetter, data)));
      }

      return build.build();
  }
}
```

Below is a representation of the tree of Sections that has the root in `BarSection`. Each node in the tree is a Section, and the leaves are Components that can be rendered on screen.
Each one of the sections in the tree acts as a data source. Its responsibilities are to describe what data it needs and how that data should be rendered.


<img src="/static/images/group-section-spec.png" style="width: 800px;">
