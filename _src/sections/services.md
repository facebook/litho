---
id: services
title: Granular Dependency Injection
---

Values such as `@Prop` flow through Sections before being rendered on-screen. The Sections API sits between a data source and the UI components to efficiently calculate the changesets that are needed to render the components. For better performance, only do work at the layer where it's required.

The data should ideally be fetched only in the Sections that use it. The 'data fetchers' need to be available throughout the Section's hierarchy without having to needlessly pass through each of the layers or the `GroupSection` that don't use it directly.  This is where Services prove useful.

## Using Services

Services are used by Sections to control the data source imperatively. It is through the service APIs where the [SectionLifecycle](pathname:///javadoc/com/facebook/litho/sections/SectionLifecycle.html) can be made known outside of the hierarchy. This provides an indication of when to start fetching data.

Since a service is tied to a particular Section, this means it has access to the `@Prop` and `@State` and interacts with them. This also means that the service is able to respond to events such as `@OnViewportChanged` and `@OnRefresh` and call for a state update when new data arrives to allow the data to flow down the hierarchy.

### Creation

Services are persisted across state updates for however long the sections remain in the hierarchy. The first and only instance of a `Service` should be created in the lifecycle `@OnCreateService`, as follows:

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

`@OnBindService` is a callback that enables the definition of how a service should interact with its section. This bridge can be used to pass the new set of data to the section whenever a fetch is completed:

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

`@OnUnbindService` provides the callback for you to clean up and undo anything performed in `@OnBindService`:

```java
@OnUnbindService
static void onUnbindService(
  SectionContext c,
  DataLoader service) {
    service.unregisterEventLoader();
}
```

Both functions are called every time the section tree is updated by events such as a state update. These functions receive the service created by `@OnCreateService` as the second function parameter.

## Use of Services in Context

As mentioned, services are able to respond to Litho and Sections events such as `@OnRefresh`:

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
