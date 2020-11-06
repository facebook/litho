This provides guidance on how to contribute various content to `fblitho.com`.

## Basic Structure

Most content is written in markdown or [MDX](https://mdxjs.com/). You name the file `something.md` or `something.mdx`, then have a header that looks like this:

```yml
---
id: contributing
title: How to Contribute
---

```

Customize these values for each document, blog post, etc.

> The filename of the `.md` file doesn't actually matter; what is important is the `id` being unique.

## Pages

Any `.js` file inside `src/pages` will be generated as a separate page. For more information, look at the Docusaurus 2 [creating pages guide](https://v2.docusaurus.io/docs/next/creating-pages).

Pages are written in React and use the `Layout` component for their header, footer and styling as well as accessing config values.

```jsx
import React from 'react';
import Layout from '@theme/Layout';

function Hello() {
  return (
    <Layout title="Hello">
      <div
        style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '50vh',
          fontSize: '20px',
        }}>
        <p>
          Edit <code>pages/hello.js</code> and save to reload.
        </p>
      </div>
    </Layout>
  );
}

export default Hello;
```

## Docs

For a full walkthrough of features, visit the [Docusaurus docs documentation](https://v2.docusaurus.io/docs/next/docs-introduction)

To modify docs, edit the appropriate markdown file in `./docs/`.

To add docs to the site....

1. Add your markdown file to the `./docs/` folder.
2. Update `./sidebars.js` file to add your new document to the navigation bar. Use the `id` you put in your doc markdown as the value inside the appropriate sidebar section.
3. [Run the site locally](./README.md) to test your changes. It will be at `http://127.0.0.1/docs/your-doc-id`
4. Push your changes to GitHub.

## Utilize `mdx`

If you want to mix markdown and `JSX`, you can use the `.mdx` file extension on any doc.

e.g.

```jsx
---
id: getting-started
title: Getting Started
hide_table_of_contents: true
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

<Tabs
  defaultValue="java"
  values={[
    {label: 'Java', value: 'java'},
    {label: 'Kotlin', value: 'kotlin'},
  ]}>
  <TabItem value="java">

## Java

Java example
  </TabItem>
  <TabItem value="kotlin">
## Kotlin

Kotlin example
  </TabItem>
</Tabs>
```

This allows you to combine the ease of markdown with the flexibility of React and `JSX`.

## Other Changes

- CSS: `./src/css/custom.css` (Colors and other root level or layout changes) or `./pages/styles.module.scss` (Page specific changes).
- Images: `./static/images/...`
