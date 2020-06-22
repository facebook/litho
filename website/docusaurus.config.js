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
      links: [
        {
          to: 'docs/',
          activeBasePath: 'docs',
          label: 'Docs',
          position: 'right',
        },
        {
          to: '#',
          activeBasePath: '#',
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
          href: 'https://github.com/facebook/docusaurus',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      logo: {
        alt: 'Facebook Open Source Logo',
        src: 'img/oss_logo.png',
        href: 'https://opensource.facebook.com',
      },
      links: [
        {
          label: 'Open Source Projects',
          to: '#',
        },
        {
          label: 'Github',
          to: '#',
        },
        {
          label: 'Twitter',
          to: '#',
        },
      ],
    },
    prism: {
      additionalLanguages: ['groovy', 'kotlin'],
    },
  },
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
          customCss: require.resolve('./src/css/custom.css'),
        },
      },
    ],
  ],
};
