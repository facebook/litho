import React from 'react';
import clsx from 'clsx';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import useBaseUrl from '@docusaurus/useBaseUrl';
import styles from './styles.module.scss';

const features = [
  {
    title: <>Declarative</>,
    imageUrl: 'images/home-code.png',
    description: (
      <>
      Litho uses a declarative API to define UI components.
      You simply describe the layout for your UI based on a set of immutable inputs and the framework takes care of the rest.
      With code generation, Litho can perform optimisations for your UI under the hood, while keeping your code simple and easy to maintain.
      </>
    ),
  },
  {
    title: <>Asynchronous layout</>,
    imageUrl: 'images/home-async.png',
    description: (
      <>
      Litho can measure and layout your UI ahead of time without blocking the UI thread.
      By decoupling its layout system from the traditional Android View system, Litho can drop the UI thread constraint imposed by Android.
      </>
    ),
    dark: true,
  },
  {
    title: <>Flatter view hierarchies</>,
    imageUrl: 'images/home-flat-not-flat.png',
    description: (
      <>
      Litho uses <a href="https://yogalayout.com/docs">Yoga</a> for layout and automatically reduces the number of ViewGroups that your UI contains.
      This, in addition to Litho's text optimizations, allows for much smaller view hierarchies and improves both memory and scroll performance.
      </>
    ),
  },
  {
    title: <>Fine-grained recycling</>,
    imageUrl: 'images/home-incremental-mount.png',
    description: (
      <>
      With Litho, each UI item such as text, image, or video is recycled individually.
      As soon as an item goes off screen, it can be reused anywhere in the UI and pieced together with other items to create new UI elements.
      Such recycling reduces the need of having multiple view types and improves memory usage and scroll performance.
      </>
    ),
    dark: true,
  },
];

function Feature({imageUrl, title, description, dark}) {
  const imgUrl = useBaseUrl(imageUrl);
  return (
    <div className={clsx("container", dark && styles.darkFeature, styles.feature)}>
      <div className={styles.featureContent}>
        <img className={styles.featureImage} src={imgUrl} alt={title} />
        <div className={styles.featureText}>
          <h3>{title}</h3>
          <p>{description}</p>
        </div>
      </div>
    </div>
  );
}

function Home() {
  const context = useDocusaurusContext();
  const {siteConfig = {}} = context;
  return (
    <Layout
      description="Home page of Litho: A declaritive UI framework for Android">
      <header className={clsx('hero hero--primary', styles.heroBanner)}>
        <div className="container">
          <img className={styles.heroImage} src={useBaseUrl('logo.svg')} />
          <p className="hero__title">{siteConfig.title + ': ' + siteConfig.tagline}</p>
          <div className={styles.buttons}>
            <Link
              className={clsx(
                'button button--outline button--secondary button--lg',
                styles.getStarted,
              )}
              to={useBaseUrl('docs/')}>
              Get Started
            </Link>
          </div>
        </div>
      </header>
      <main>
        {features && features.length > 0 && (
          <section className={styles.features}>
              <div className="row">
                {features.map((props, idx) => (
                  <Feature key={idx} {...props} />
                ))}
              </div>
          </section>
        )}
      </main>
    </Layout>
  );
}

export default Home;
