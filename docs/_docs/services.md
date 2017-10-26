---
docid: services
title: Services
layout: docs
permalink: /docs/services
---
## Service

### Data Flow
Data flows through sections before being rendered on screen. Sections sit between your data source and UI to efficiently calculate the changesets you need to render your components.

For better performance, we should only do work that's required. Hence, the data should ideally be fetched whenever your sections need it. This is where services come in.

### Introduction
Services are what sections use to control your data source imperatively. It is through the service APIs where the [SectionLifecycle](https://fblitho.com/javadoc/com/facebook/litho/sections/SectionLifecycle.html) can be made known outside of the hierarchy. This allows you to know when you should start fetching your data.

Since a service is tied to a particular section, this means it has access to all the `Prop` and `State` and interacts with them. This also means that the service is able to respond to events such as `@OnViewportChanged` and `@OnRefresh`, and call for a state update when new data arrives to allow the data to flow down the hierarchy.

### @OnCreateService
Services are persisted across state updates and for however long the sections remain in the hierarchy. The first and only instance of a `Service` should be created in the lifecycle `@OnCreateService`

```java
@GroupSectionSpec
public ServiceSectionSpec {
  ...
  @OnCreateService
  static DataLoader onCreateServices(
    SectionContext c,
    @Prop Configuration config,
    ...) {
      /**
       * onCreateServices() is called only once when the Section is first created.
       * In this function you should create and return your service.
       **/
      return new DataLoaderFactory.createLoader(config);
  }
}
```

### @OnBindService and @OnUnbindService

`@OnBindService` is a callback that allows you to define how your service should interact with its section. This bridge can be used to pass the new set of data to the section whenever a fetch is completed.

`@OnUnbindService` provides the callback for you to clean up and undo anything you have done in `@OnBindService`.

```java
@GroupSectionSpec
public ServiceSectionSpec {
  ...
  @OnBindService
  static void onBindService(
    final SectionContext c,
    final DataLoader service,
    ...) {
      /**
       * onBindService() is called (along with onUnbindService()) every time
       * the section tree is updated (usually because of a state update).
       * This function is passed the service created by onCreateService as the second function parameter.
       * In this function you should bind any EventHandler that will make state changes to your service.
       **/
      service.registerEventLoader(ServiceSection.dataLoaded(c));
  }

  @OnUnbindService
  static void onUnbindService(
    final SectionContext c,
    final DataLoader service,
    ...) {
      /**
       * onUnBindService() is called (along with onBindService()) every time
       * the section tree is updated (usually because of a state update).
       * This function is passed the service created by onCreateService as the second function parameter.
       * This should be the inverse of onBindService(). Anything set or bound in onBindService() should be undone here.
       **/
      service.unregisterEventLoader();
  }

  @OnEvent(YourData.class)
  static void dataLoaded(
    final SectionContext c,
    @FromEvent final Data data) {
      // Update your state with the new data
      ServiceSection.updateData(c, data);
  }

  @UpdateState
  static void updateData(
    final StateValue<Data> connectionData,
    @Param Data data) {
      connectionData.set(data);
  }
}
```

### Data Fetching
As aforementioned, services are able to respond to events such as `@OnViewportChanged` and `@OnRefresh`

```java
 @OnRefresh
 static void onRefresh(
   SectionContext c,
   DataLoader service,
   ...) {
     service.refreshData();
 }

 @OnViewportChanged
 static void onViewportChanged(
   SectionContext c,
   int firstVisibleIndex,
   int lastVisibleIndex,
   int firstFullyVisibleIndex,
   int lastFullyVisibleIndex,
   int totalCount,
   DataLoader service,
   ...) {
     service.makeTailFetch();
 }
```
