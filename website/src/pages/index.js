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
        Litho uses a declarative API to define UI components. You simply
        describe the layout for your UI based on a set of immutable inputs and
        the framework takes care of the rest. With code generation, Litho can
        perform optimisations for your UI under the hood, while keeping your
        code simple and easy to maintain.
      </>
    ),
    dark: false,
  },
  {
    title: <>Asynchronous layout</>,
    imageUrl: 'images/home-async.png',
    description: (
      <>
        Litho can measure and layout your UI ahead of time without blocking the
        UI thread. By decoupling its layout system from the traditional Android
        View system, Litho can drop the UI thread constraint imposed by Android.
      </>
    ),
    dark: true,
  },
  {
    title: <>Flatter view hierarchies</>,
    imageUrl: 'images/home-flat-not-flat.png',
    description: (
      <>
        Litho uses <a href="https://yogalayout.com/docs">Yoga</a> for layout and
        automatically reduces the number of ViewGroups that your UI contains.
        This, in addition to Litho's text optimizations, allows for much smaller
        view hierarchies and improves both memory and scroll performance.
      </>
    ),
    dark: false,
  },
  {
    title: <>Fine-grained recycling</>,
    imageUrl: 'images/home-incremental-mount.png',
    description: (
      <>
        With Litho, each UI item such as text, image, or video is recycled
        individually. As soon as an item goes off screen, it can be reused
        anywhere in the UI and pieced together with other items to create new UI
        elements. Such recycling reduces the need of having multiple view types
        and improves memory usage and scroll performance.
      </>
    ),
    dark: true,
  },
];

function Feature({imageUrl, title, description, dark}) {
  const imgUrl = useBaseUrl(imageUrl);
  return (
    <section
      className={clsx(
        dark && styles.darkFeature,
        !dark && styles.lightFeature,
      )}>
      <div className={styles.featureContent}>
        <img className={styles.featureImage} src={imgUrl} alt={title} />
        <div className={styles.featureText}>
          <h3 className={styles.featureTitle}>{title}</h3>
          <p className={styles.featureBody}>{description}</p>
        </div>
      </div>
    </section>
  );
}

function VideoContainer() {
  return (
    <div className="container text--center margin-bottom--xl margin-top--lg">
      <div className="row">
        <div className="col">
          <h2>Check it out in the intro video</h2>
          <div className={styles.ytVideo}>
            <iframe
              width="560"
              height="315"
              src="https://www.youtube.com/embed/RFI-fuiMRK4"
              title="Explain Like I'm 5: Litho"
              frameBorder="0"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
              allowFullScreen
            />
          </div>
        </div>
      </div>
    </div>
  );
}

function Home() {
  const context = useDocusaurusContext();
  const {siteConfig = {}} = context;
  return (
    <Layout description="Home page of Litho: A declarative UI framework for Android">
      <div className={styles.heroBanner}>
        <div className={styles.heroInner}>
          <img
            className={styles.heroImage}
            src={useBaseUrl('images/logo.svg')}
          />
          <div className={styles.heroTitle}>
            {'Litho: ' + siteConfig.tagline}
          </div>
          <div className={styles.buttons}>
            <Link
              className={clsx('button button--outline', styles.button)}
              to={useBaseUrl('docs/mainconcepts/components-basics')}>
              GET STARTED
            </Link>
            <Link
              className={clsx('button button--outline', styles.button)}
              to={useBaseUrl('docs/intro')}>
              LEARN MORE
            </Link>
            <Link
              className={clsx('button button--outline', styles.button)}
              to={useBaseUrl('docs/tutorial/overview')}>
              TUTORIAL
            </Link>
          </div>
        </div>
      </div>
      <main>
      <div>
       <div className={styles.banner}>
         Support Ukraine 🇺🇦{' '}
         <Link to="https://opensource.facebook.com/support-ukraine">
           Help Provide Humanitarian Aid to Ukraine
         </Link>
         .
       </div>
      </div>
        <VideoContainer />
        {features &&
          features.length > 0 &&
          features.map((props, idx) => <Feature key={idx} {...props} />)}
      </main>
    </Layout>
  );
}

export default Home;
