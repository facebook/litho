module.exports = {
  title: 'Litho',
  tagline: 'A declarative UI framework for Android',
  url: 'https://your-docusaurus-test-site.com',
  baseUrl: '/',
  favicon: 'favicon.png',
  organizationName: 'facebook', // Usually your GitHub org/user name.
  projectName: 'docusaurus', // Usually your repo name.
  themeConfig: {
    defaultDarkMode: false,
    disableDarkMode: true,
    navbar: {
      title: 'Litho',
      logo: {
        alt: 'Litho Logo',
        src: 'logo.svg',
      },
      links: [
        {
          to: 'docs/',
          activeBasePath: 'docs',
          label: 'Docs',
          position: 'right',
        },
        {
          to: 'docs/javadoc',
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
        src: 'oss_logo.png',
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
              to: 'api/',
            },
          ]
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
          ]
        },
        {
          title: 'Social',
          items: [
            {
              label: 'Github',
              to: '#',
            },
            {
              label: 'Twitter',
              to: '#',
            },
          ]
        },
      ],
      logo: {
        alt: 'Facebook Open Source Logo',
        src: 'static/oss_logo.png',
        href: 'https://opensource.facebook.com',
      },
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
          editUrl:
            'https://github.com/facebook/litho/edit/master/website/',
        },
        theme: {
          customCss: require.resolve('./src/css/custom.scss'),
        },
      },
    ],
  ],
};
