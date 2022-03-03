/**
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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
 *
 * @format
 */

const {fbContent, fbInternalOnly} = require('internaldocs-fb-helpers');

const repoUrl = 'https://github.com/facebook/litho';
const siteTitle = fbContent({internal: 'Litho @FB', external: 'Litho'});

module.exports = {
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'throw',
  trailingSlash: true,
  title: siteTitle,
  tagline: 'A declarative UI framework for Android',
  url: fbContent({
    internal: 'https://litho.thefacebook.com',
    external: 'https://fblitho.com',
  }),
  baseUrl: '/',
  favicon: 'images/favicon.png',
  organizationName: 'facebook',
  projectName: 'litho',
  themeConfig: {
    announcementBar: {
          id: 'support_ukraine',
          content:
            'Support Ukraine 🇺🇦 <a target="_blank" rel="noopener noreferrer" href="https://opensource.facebook.com/support-ukraine">Help Provide Humanitarian Aid to Ukraine</a>',
          textColor: '#091E42',
          isCloseable: false,
        },
    navbar: {
      title: siteTitle,
      logo: {
        alt: 'Litho Logo',
        src: 'images/logo.svg',
      },
      items: [
        {
          label: 'Docs',
          to: 'docs/intro/motivation',
          position: 'right',
        },
        {
          label: 'API',
          to: 'pathname:///javadoc',
          position: 'right',
        },
        {
          label: 'Tutorial',
          to: 'docs/tutorial/overview',
          position: 'right',
        },
        {
          label: 'GitHub',
          href: repoUrl,
          position: 'right',
        },
      ],
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
              to: 'pathname:///javadoc',
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
              href: 'https://opensource.facebook.com',
            },
          ],
        },
        {
          title: 'Legal',
          items: [
            {
              label: 'Privacy',
              href: 'https://opensource.facebook.com/legal/privacy',
            },
            {
              label: 'Terms',
              href: 'https://opensource.facebook.com/legal/terms',
            },
          ],
        },
        {
          title: 'Social',
          items: [
            {
              label: 'Github',
              href: repoUrl,
            },
            {
              label: 'Twitter',
              href: 'https://twitter.com/fblitho',
            },
          ],
        },
      ],
      // Please do not remove the credits, help to publicize Docusaurus :)
      copyright: `Copyright © ${new Date().getFullYear()} Facebook, Inc.`,
    },
    colorMode: {
      // Current CSS doesn't have high contrast so it needs some work before being enabled.
      defaultMode: 'light',
      disableSwitch: true,
    },
    algolia: fbContent({
      internal: undefined,
      external: {
        apiKey: '6502239eccd45af18518695c2b743307',
        indexName: 'fblitho',
      }
    }),
    googleAnalytics: fbContent({
      internal: undefined,
      external: {
        trackingID: 'UA-44373548-28',
      }
    }),
    prism: {
      theme: require('prism-react-renderer/themes/github'),
      darkTheme: require('prism-react-renderer/themes/dracula'),
      additionalLanguages: ['java', 'groovy', 'kotlin'],
    },
  },
  customFields: {
    fbRepoName: 'fbsource',
    ossRepoPath: 'fbandroid/libraries/components'
  },
  plugins: [
    'docusaurus-plugin-sass',
    [
      '@docusaurus/plugin-client-redirects',
      {
        fromExtensions: ['html', 'htm'],
        redirects: [
          {
            to: '/',
            from: ['/docs'],
          },
          {
            to: '/docs/animations/dynamic-props',
            from: [
              '/docs/dynamic-props',
              '/docs/mainconcepts/coordinate-state-actions/dynamic-props',
            ],
          },
          {
            to: '/docs/sections/start',
            from: ['/docs/sections-intro'],
          },
          {
            to: '/docs/sections/recycler-collection-component',
            from: [
              '/docs/recycler-collection-component',
              '/docs/hscrolls',
            ],
          },
          {
            to: '/docs/sections/services',
            from: ['/docs/services'],
          },
          {
            to: '/docs/animations/transition-basics',
            from: ['/docs/transition-basics'],
          },
          {
            to: '/docs/animations/transition-types',
            from: ['/docs/transition-types'],
          },
          {
            to: '/docs/animations/transition-choreography',
            from: ['/docs/transition-choreography'],
          },
          {
            to: '/docs/animations/transition-all-layout',
            from: ['/docs/transition-all-layout'],
          },
          {
            to: '/docs/animations/transition-definitions',
            from: ['/docs/transition-definitions'],
          },
          {
            to: '/docs/animations/transition-key-types',
            from: ['/docs/transition-key-types'],
          },
        ],
      },
    ]
  ],
  presets: [
    [
      require.resolve('docusaurus-plugin-internaldocs-fb/docusaurus-preset'),
      {
        docs: {
          path: '../docs',
          sidebarPath: require.resolve('./sidebars.js'),
          editUrl: fbContent({
            internal: 'https://www.internalfb.com/intern/diffusion/FBS/browse/master/fbandroid/libraries/components/website/',
            external: 'https://github.com/facebook/litho/edit/master/website/',
          })
        },
        'remark-code-snippets': {
          baseDir: '..'
        },
        theme: {
          customCss: require.resolve('./src/css/custom.scss'),
        },
        enableEditor: fbContent({
          internal: 'top',
          external: true,
        }),
        staticDocsProject: 'litho',
        trackingFile: 'xplat/staticdocs/WATCHED_FILES',
      },
    ],
  ],
};
