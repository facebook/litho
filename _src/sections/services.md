---
id: services
title: 'Advanced: Granular Dependency Injection'
---

Values such as `@Prop` flow through Sections before being rendered on screen. The Sections API sits between your data source and the UI Component to efficiently calculate the changesets you need to render your components. For better performance we should only do work at the layer where it is required.

Thus, the data should ideally be fetched only in the Sections that use it. You need for those **data fetchers to be available throughout your Sections hierarchy** without having to cumbersomely pass them through each of the layers, and even through `GroupSection` that don't use it directly.

This is where Services come in.

## Usage
Services are what sections use to control your data source imperatively. It is through the service APIs where the [SectionLifecycle](pathname:///javadoc/com/facebook/litho/sections/SectionLifecycle.html) can be made known outside of the hierarchy. This allows you to know when you should start fetching your data.

Since a service is tied to a particular Section, this means it has access to all the `@Prop` and `@State` and interacts with them. This also means that the service is able to respond to events such as `@OnViewportChanged` and `@OnRefresh`, and call for a state update when new data arrives to allow the data to flow down the hierarchy.

### Creation
Services are persisted across state updates and for however long the sections remain in the hierarchy. The first and only instance of a `Service` should be created in the lifecycle `@OnCreateService`

```java
@GroupSectionSpec
public ServiceSectionSpec {
  ...
  @OnCreateService
  static DataLoader onCreateServices(
    SectionContext c,
    @Prop Configuration config) {
      return DataLoaderFactory.createLoader(config);
  }
}
```

### Lifecycle of Services

`@OnBindService` is a callback that allows you to define how your service should interact with its section. This bridge can be used to pass the new set of data to the section whenever a fetch is completed.

```java
@OnBindService
static void onBindService(
  SectionContext c,
  DataLoader service) {
    service.registerEventLoader(ServiceSection.dataLoaded(c));
}

@OnEvent(YourData.class)
static void dataLoaded(
  SectionContext c,
  @FromEvent Data data) {
    ServiceSection.updateData(c, data);
}
```

`@OnUnbindService` provides the callback for you to clean up and undo anything you have done in `@OnBindService`.

```java
@OnUnbindService
static void onUnbindService(
  SectionContext c,
  DataLoader service) {
    service.unregisterEventLoader();
}
```

Both functions are called every time the section tree is updated by events such as a state update. These function receive the service created by `@OnCreateService` as the second function parameter.

## Use of Services in context

As aforementioned, services are able to respond to Litho and Sections events such as `@OnRefresh`:

```java
 @OnRefresh
 static void onRefresh(
   SectionContext c,
   DataLoader service) {
     service.refreshData();
 }
```

or `@OnViewportChanged`:

```java
 @OnViewportChanged
 static void onViewportChanged(
   SectionContext c,
   int firstVisibleIndex,
   int lastVisibleIndex,
   int totalCount,
   int firstFullyVisibleIndex,
   int lastFullyVisibleIndex,
   DataLoader service) {
     service.makeTailFetch();
 }
```
