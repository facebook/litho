---
id: props-vs-state
title: Props vs. State
---

Litho components have two types of data model: [Props](/codegen/passing-data-to-components/props.md) and [State](../mainconcepts/coordinate-state-actions/hoisting-state.md).

This page helps you to understand the difference between the two, so you'll know when to use each of them.

## Props

Props are for passing data down the tree from a component to its children. They are useful for defining the static part of a component's model, because props cannot be changed.

## State

State is reserved mostly for handling updates that result from interactions with a component or updates that can only be intercepted by that component. It is managed by the component and it's not visible outside of it; a component's parent has no knowledge of its children's state.

## Using State or Props

The following table contains a set of questions that might help you decide whether you should use Props or State for data on your component

| Question | Prop or State  |
| :-- | :-- |
| Does it define a property that remains unchanged? | If yes, then use a Prop. |
| Does it need to be initialized and passed down from the parent? | If yes, then use a Prop |
| Should it change its value when a user interacts with the components (for instance clicking it)? | If yes, then use a State |
| Can you compute its value based on other existing props and state? | If yes, then don't create a State |

Making your components stateful increases the complexity of your application, which makes it harder to maintain. It may also make it more difficult to understand than the top-down props approach.

:::tip
You should try to keep the use of state for your components to a minimum, and your data flow top-down. If you have multiple sibling components whose state is co-dependent, you should identify a parent component that can instead hold and manage this state for its children.
:::

## Example Scenario

Consider the scenario of a list of radio buttons where only one can be selected at a time.  In such a scenario, you'd keep a state specifying which item is selected in the parent and propagate that information top-down to the children through props.
