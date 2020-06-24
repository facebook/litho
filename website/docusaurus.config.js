module.exports = {
  title: 'Litho',
  tagline: 'A declarative UI framework for Android',
  url: 'https://fblitho.com',
  baseUrl: '/',
  favicon: 'images/favicon.png',
  organizationName: 'facebook', // Usually your GitHub org/user name.
  projectName: 'litho', // Usually your repo name.
  themeConfig: {
    defaultDarkMode: false,
    disableDarkMode: true,
    navbar: {
      title: 'Litho',
      logo: {
        alt: 'Litho Logo',
        src: 'images/logo.svg',
      },
      links: [
        {
          to: 'docs/',
          activeBasePath: 'docs',
          label: 'Docs',
          position: 'right',
        },
        {
          href: '/javadoc/',
          label: 'API',
          position: 'right',
        },
        {
          to: 'docs/tutorial',
          activeBasePath: 'docs/tutorial',
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
              to: 'docs/',
            },
            {
              label: 'API',
              href: '/javadoc/',
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
      copyright: `Copyright © ${new Date().getFullYear()} Facebook, Inc. Built with Docusaurus.`,
    },
    prism: {
      theme: require('prism-react-renderer/themes/github'),
      additionalLanguages: ['groovy', 'kotlin'],
    },
  },
  plugins: ['docusaurus-plugin-sass'],
  presets: [
    [
      '@docusaurus/preset-classic',
      {
        docs: {
          // It is recommended to set document id as docs home page (`docs/` path).
          homePageId: 'getting-started',
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
