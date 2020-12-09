---
id: props-vs-state
title: Props vs. State
---

:::danger Content needs to be updated
Moved from old website without any change.
:::

Litho components have two types of data models: [Props](/docs/props) and [State](/docs/mainconcepts/coordinate-state-actions/hoisting-state). It's important to understand the difference between the two to know when you need to use each of them.

Props are for passing data down the tree from a component to its children. They are useful for defining the static part of a component's model, because props cannot be changed.

State is reserved mostly for handling updates that result from interactions with a component or updates that can only be intercepted by that component. It is managed by the component and it's not visible outside of it; a component's parent has no knowledge of its children's state.

Here's a quick overview of how to decide whether you should use props or state for some data on your component:

* Does it define a property that remains unchanged? If so, it should be a prop.
* Does it need to be initialized and passed down from the parent? If so, it should be a prop.
* Should it change its value when a user interacts with the components (for instance clicking it)? If so, it should be a state.
* Can you compute its value based on other existing props and state? If so, you shouldn't create a state for it.

Making your components stateful increasing the complexity of your application and it makes it much harder maintain and  to reason about the data flow than the top-down props approach. You should try and keep using state for your components to a minimum and strive to keep your data flow top-down. If you have multiple sibling components whose state is co-dependent, you should identify a parent component which can instead hold and manage this state for its children.

Let's take the example of a list of radio buttons, where you cannot have multiple items checked at the same time. The state of all the radio buttons depends on whether other items get clicked and checked. Instead of making all the radio buttons stateful, you should keep the state of what item is clicked in the parent and propagate that information top-down to the children through props.
