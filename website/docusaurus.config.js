/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

module.exports = {
  title: 'Litho',
  tagline: 'A declarative UI framework for Android',
  url: 'https://fblitho.com',
  baseUrl: '/',
  favicon: 'images/favicon.png',
  organizationName: 'facebook', // Usually your GitHub org/user name.
  projectName: 'litho', // Usually your repo name.
  themeConfig: {
    navbar: {
      title: 'Litho',
      logo: {
        alt: 'Litho Logo',
        src: 'images/logo.svg',
      },
      items: [
        {
          to: 'docs/getting-started',
          label: 'Docs',
          position: 'right',
        },
        {
          to: 'pathname:///javadoc',
          label: 'API',
          position: 'right',
        },
        {
          to: 'docs/tutorial',
          label: 'Tutorial',
          position: 'right',
        },
        {
          href: 'https://github.com/facebook/litho',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    algolia: {
      apiKey: '6502239eccd45af18518695c2b743307',
      indexName: 'fblitho',
    },
    footer: {
      style: 'dark',
      logo: {
        alt: 'Facebook Open Source Logo',
        src: 'images/oss_logo.png',
        href: 'https://opensource.facebook.com',
      },
      links: [
        {
          title: 'Learn',
          items: [
            {
              label: 'Getting Started',
              to: 'docs/getting-started',
            },
            {
              label: 'API',
              to: 'javadoc',
            },
          ],
        },
        {
          title: 'Open Source',
          items: [
            {
              label: 'How To Contribute',
              to: 'docs/contributing',
            },
            {
              label: 'Open Source Projects',
              to: 'https://opensource.facebook.com',
            },
          ],
        },
        {
          title: 'Social',
          items: [
            {
              label: 'Github',
              to: 'https://github.com/facebook/litho',
            },
            {
              label: 'Twitter',
              to: 'https://twitter.com/fblitho',
            },
          ],
        },
      ],
      logo: {
        alt: 'Facebook Open Source Logo',
        src: 'images/oss_logo.png',
        href: 'https://opensource.facebook.com',
      },
      // Please do not remove the credits, help to publicize Docusaurus :)
      copyright: `Copyright \u00A9 ${new Date().getFullYear()} Facebook, Inc. Built with Docusaurus.`,
    },
    prism: {
      theme: require('prism-react-renderer/themes/github'),
      darkTheme: require('prism-react-renderer/themes/dracula'),
      additionalLanguages: ['groovy', 'kotlin'],
    },
  },
  plugins: ['docusaurus-plugin-sass', require.resolve('docusaurus-plugin-internaldocs-fb')],
  presets: [
    [
      '@docusaurus/preset-classic',
      {
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          editUrl: 'https://github.com/facebook/litho/edit/master/website/',
        },
        theme: {
          customCss: require.resolve('./src/css/custom.scss'),
        },
      },
    ],
  ],
};
