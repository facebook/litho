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

const {
  fbContent,
  fbInternalOnly,
} = require('docusaurus-plugin-internaldocs-fb/internal');

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
    navbar: {
      title: siteTitle,
      logo: {
        alt: 'Litho Logo',
        src: 'images/logo.svg',
      },
      items: [
        {
          label: 'Docs',
          to: 'docs/components-basics',
          position: 'right',
        },
        {
          label: 'API Reference [ʙᴇᴛᴀ]',
          to: 'https://fblitho.com/reference',
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
        alt: 'Meta Open Source Logo',
        src: 'images/meta_opensource_logo_negative.svg',
        href: 'https://opensource.fb.com/',
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
              label: 'API Reference [ʙᴇᴛᴀ]',
              to: 'https://fblitho.com/reference',
            },
          ],
        },
        {
          title: 'Open Source',
          items: [
            {
              label: 'Where To Contribute',
              href: repoUrl,
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
      copyright: `Copyright © ${new Date().getFullYear()} Meta Platforms, Inc`,
    },
    colorMode: {
      // Current CSS doesn't have high contrast so it needs some work before being enabled.
      defaultMode: 'light',
      disableSwitch: true,
    },
    algolia: fbContent({
      internal: undefined,
      external: {
        appId: '5QBCYR8NMZ',
        apiKey: '770b32556a2f295e844076c56d04da49',
        indexName: 'fblitho',
      },
    }),
    prism: {
      theme: require('prism-react-renderer/themes/github'),
      darkTheme: require('prism-react-renderer/themes/dracula'),
      additionalLanguages: ['java', 'groovy', 'kotlin'],
    },
  },
  customFields: {
    fbRepoName: 'fbsource',
    ossRepoPath: 'fbandroid/libraries/components',
  },
  plugins: [
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
            to: '/docs/legacy-spec-api/dynamic-props',
            from: [
              '/docs/dynamic-props',
              '/docs/coordinate-state-actions/dynamic-props',
              '/docs/codegen/dynamic-props',
            ],
          },
          {
            to: '/docs/legacy-spec-api/sections/start',
            from: ['/docs/sections-intro', '/docs/sections/start'],
          },
          {
            to: '/docs/legacy-spec-api/sections/recycler-collection-component',
            from: ['/docs/recycler-collection-component', '/docs/hscrolls', '/docs/sections/recycler-collection-component'],
          },
          {
            to: '/docs/legacy-spec-api/sections/advanced/services',
            from: ['/docs/services', '/docs/sections/services'],
          },
          {
            to: '/docs/transitions/transition-basics',
            from: ['/docs/transition-basics', '/docs/animations/transition-basics'],
          },
          {
            to: '/docs/transitions/transition-types',
            from: ['/docs/transition-types', '/docs/animations/transition-types'],
          },
          {
            to: '/docs/transitions/transition-choreography',
            from: ['/docs/transition-choreography', '/docs/animations/transition-choreography'],
          },
          {
            to: '/docs/transitions/transition-all-layout',
            from: ['/docs/transition-all-layout', '/docs/animations/transition-all-layout'],
          },
          {
            to: '/docs/legacy-spec-api/transition-definitions',
            from: [
              '/docs/transition-definitions',
              '/docs/animations/transition-definitions',
              '/docs/codegen/transition-definitions',
            ],
          },
          {
            to: '/docs/transitions/transition-key-types',
            from: ['/docs/transition-key-types', '/docs/animations/transition-key-types'],
          },
          {
            to: '/docs/legacy-spec-api/coding-style',
            from: ['/docs/best-practices/coding-style', '/docs/codegen/coding-style'],
          },
          {
            to: '/docs/migrating-to-kcomponents/kotlin-api-cheatsheet',
            from: ['/docs/kotlin/kotlin-api-cheatsheet-primitives', '/docs/kotlin/kotlin-api-cheatsheet', '/docs/kotlin/cheatsheet'],
          },
          // kotlin/ → migrating-to-kcomponents/
          {
            to: '/docs/migrating-to-kcomponents/kotlin-api-basics',
            from: ['/docs/kotlin/basics'],
          },
          {
            to: '/docs/migrating-to-kcomponents/event-handling',
            from: ['/docs/kotlin/event-handling'],
          },
          {
            to: '/docs/migrating-to-kcomponents/kotlin-flexbox-containers',
            from: ['/docs/kotlin/flexbox-containers'],
          },
          {
            to: '/docs/migrating-to-kcomponents/hooks-for-spec-developers',
            from: ['/docs/kotlin/hooks-for-spec-developers'],
          },
          {
            to: '/docs/migrating-to-kcomponents/kotlin-transitions',
            from: ['/docs/kotlin/kotlin-transitions'],
          },
          {
            to: '/docs/migrating-to-kcomponents/migrating-from-mountspecs-to-primitives',
            from: ['/docs/kotlin/migrating-from-mountspecs-to-primitives'],
          },
          {
            to: '/docs/migrating-to-kcomponents/migrate-layout-spec',
            from: ['/docs/kotlin/migrating-layout-spec'],
          },
          {
            to: '/docs/migrating-to-kcomponents/kotlin-intro',
            from: ['/docs/kotlin/setup-and-intro'],
          },
          // kotlin/ → adopting-litho/
          {
            to: '/docs/adopting-litho/migration-strategies',
            from: ['/docs/kotlin/migration-strategies'],
          },
          // kotlin/ → legacy-spec-api/
          {
            to: '/docs/legacy-spec-api/custom-view-compat',
            from: ['/docs/kotlin/custom-view-compat', '/docs/adopting-litho/custom-view-compat'],
          },
          // kotlin/ → testing/
          {
            to: '/docs/testing/testing-getting-started',
            from: ['/docs/kotlin/testing-getting-started'],
          },
          {
            to: '/docs/testing/testing-actions',
            from: ['/docs/kotlin/testing-actions'],
          },
          {
            to: '/docs/testing/testing-assertions',
            from: ['/docs/kotlin/testing-assertions'],
          },
          // kotlin/lazycollections/ → lazycollections/
          {
            to: '/docs/lazycollections',
            from: ['/docs/kotlin/lazycollections/lazycollections'],
          },
          {
            to: '/docs/lazycollections/lazycollections-interactions',
            from: ['/docs/kotlin/lazycollections/lazycollections-interactions'],
          },
          {
            to: '/docs/lazycollections/lazycollections-layout',
            from: ['/docs/kotlin/lazycollections/lazycollections-layout'],
          },
          {
            to: '/docs/lazycollections/lazycollections-working-with-updates',
            from: ['/docs/kotlin/lazycollections/lazycollections-working-with-updates'],
          },
          {
            to: '/docs/migrating-to-kcomponents/lazycollections-sections-migration',
            from: ['/docs/kotlin/lazycollections/lazycollections-sections-migration'],
          },
          // primitivecomponents/ → primitives/
          {
            to: '/docs/primitives/overview',
            from: ['/docs/primitivecomponents/overview'],
          },
          {
            to: '/docs/primitives/primitive-component',
            from: ['/docs/primitivecomponents/primitive-component'],
          },
          {
            to: '/docs/primitives/primitive-controllers',
            from: ['/docs/primitivecomponents/primitive-controllers'],
          },
          {
            to: '/docs/primitives/primitive-measuring',
            from: ['/docs/primitivecomponents/primitive-measuring'],
          },
          {
            to: '/docs/primitives/primitive-preallocation',
            from: ['/docs/primitivecomponents/primitive-preallocation'],
          },
          {
            to: '/docs/primitives/primitive-tracing',
            from: ['/docs/primitivecomponents/primitive-tracing'],
          },
          {
            to: '/docs/primitives/primitive',
            from: ['/docs/primitivecomponents/primitive'],
          },
          // hooks-and-state/ → state-and-side-effects/
          {
            to: '/docs/state-and-side-effects/hooks-intro',
            from: ['/docs/hooks-and-state/hooks-intro'],
          },
          {
            to: '/docs/state-and-side-effects/use-state',
            from: ['/docs/hooks-and-state/use-state'],
          },
          {
            to: '/docs/state-and-side-effects/use-effect',
            from: ['/docs/hooks-and-state/use-effect'],
          },
          {
            to: '/docs/state-and-side-effects/use-ref',
            from: ['/docs/hooks-and-state/use-ref'],
          },
          {
            to: '/docs/state-and-side-effects/use-cached',
            from: ['/docs/hooks-and-state/use-cached'],
          },
          {
            to: '/docs/state-and-side-effects/use-callback',
            from: ['/docs/hooks-and-state/use-callback'],
          },
          {
            to: '/docs/state-and-side-effects/use-error-boundary',
            from: ['/docs/hooks-and-state/use-error-boundary'],
          },
          {
            to: '/docs/state-and-side-effects/use-flow',
            from: ['/docs/hooks-and-state/use-flow'],
          },
          {
            to: '/docs/state-and-side-effects/use-live-data',
            from: ['/docs/hooks-and-state/use-live-data'],
          },
          {
            to: '/docs/state-and-side-effects/keys-and-identity',
            from: ['/docs/coordinate-state-actions/keys-and-identity', '/docs/identity-and-visibility/keys-and-identity'],
          },
          // animations/ → transitions/
          {
            to: '/docs/transitions/dynamic-props-bindto',
            from: ['/docs/animations/dynamic-props-bindto'],
          },
          // sections/ → legacy-spec-api/sections/
          {
            to: '/docs/legacy-spec-api/sections/advanced/architecture',
            from: ['/docs/sections/architecture'],
          },
          {
            to: '/docs/legacy-spec-api/sections/advanced/communicating-with-the-ui',
            from: ['/docs/sections/communicating-with-the-ui'],
          },
          {
            to: '/docs/legacy-spec-api/sections/advanced/diff-sections',
            from: ['/docs/sections/diff-sections'],
          },
          {
            to: '/docs/legacy-spec-api/sections/advanced/view-support',
            from: ['/docs/sections/view-support'],
          },
          {
            to: '/docs/legacy-spec-api/sections/advanced/working-ranges',
            from: ['/docs/sections/working-ranges'],
          },
          {
            to: '/docs/legacy-spec-api/sections/best-practices',
            from: ['/docs/sections/best-practices'],
          },
          {
            to: '/docs/legacy-spec-api/sections/hscrolls',
            from: ['/docs/sections/hscrolls'],
          },
          // ide/ → tooling/android-studio/
          {
            to: '/docs/tooling/android-studio/android-studio-plugin',
            from: ['/docs/ide/android-studio-plugin'],
          },
          // debugging/ → tooling/debugging/
          {
            to: '/docs/legacy-spec-api/sections/debugging-sections',
            from: ['/docs/debugging/debugging-sections', '/docs/tooling/debugging/debugging-sections'],
          },
          {
            to: '/docs/tooling/debugging/debugging-tips',
            from: ['/docs/debugging/debugging-tips'],
          },
          // devtools/ → tooling/developer-tools/
          {
            to: '/docs/tooling/developer-tools/flipper-plugins',
            from: ['/docs/devtools/flipper-plugins'],
          },
          // mainconcepts/mainconcepts → mainconcepts
          {
            to: '/docs/mainconcepts',
            from: ['/docs/mainconcepts/mainconcepts'],
          },
          // coordinate-state-actions/ → various locations
          {
            to: '/docs/best-practices/hoisting-state',
            from: ['/docs/coordinate-state-actions/hoisting-state'],
          },
          {
            to: '/docs/best-practices/communicating-between-components',
            from: ['/docs/coordinate-state-actions/communicating-between-components'],
          },
          {
            to: '/docs/visibility/visibility-handling',
            from: ['/docs/coordinate-state-actions/visibility-handling'],
          },
          // codegen/ → legacy-spec-api/
          {
            to: '/docs/legacy-spec-api/overview',
            from: ['/docs/codegen/overview'],
          },
          {
            to: '/docs/legacy-spec-api/layout-specs',
            from: ['/docs/codegen/layout-specs'],
          },
          {
            to: '/docs/legacy-spec-api/mount-specs',
            from: ['/docs/codegen/mount-specs'],
          },
          {
            to: '/docs/legacy-spec-api/accessibility-overview',
            from: ['/docs/codegen/accessibility-overview'],
          },
        ],
      },
    ],
  ],
  presets: [
    [
      require.resolve('docusaurus-plugin-internaldocs-fb/docusaurus-preset'),
      {
        'docs': {
          path: '../docs',
          sidebarPath: require.resolve('./sidebars.js'),
          editUrl: fbContent({
            internal:
              'https://www.internalfb.com/intern/diffusion/FBS/browse/master/fbandroid/libraries/components/website/',
            external: 'https://github.com/facebook/litho/edit/master/website/',
          }),
        },
        'remark-code-snippets': {
          baseDir: '..',
        },
        'theme': {
          customCss: require.resolve('./src/css/custom.css'),
        },
        'enableEditor': fbContent({
          internal: 'top',
          external: true,
        }),
        'internSearch': true,
        'staticDocsProject': 'litho',
        'trackingFile': 'fbcode/staticdocs/WATCHED_FILES',
        'googleAnalytics': fbContent({
          internal: undefined,
          external: {
            trackingID: 'UA-44373548-28',
          },
        }),
      },
    ],
  ],
};
