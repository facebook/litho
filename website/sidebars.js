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

const {fbContent, fbInternalOnly} = require('internaldocs-fb-helpers');
module.exports = {
  mainSidebar: {
    'Introducing Litho': [
      'intro',
      'motivation',
      'uses',
    ],
    'Quick Start': [
      'getting-started',
      'tutorial',
      'writing-components',
      'using-components',
    ],
    'Reference': [
      'layout-specs',
      'mount-specs',
      'props',
      'common-props',
      'dynamic-props',
      'state',
      'cached-values',
      'layout',
      'tree-props',
      'widgets',
    ],
    'Handling Events': [
      'events-overview',
      'events-touch-handling',
      'visibility-handling',
      'trigger-events',
    ],
    'Transition Animations': [
      'transition-basics',
      'transition-types',
      'transition-choreography',
      'transition-all-layout',
      'transition-definitions',
      'transition-key-types',
      'transition-faq',
    ],
    'Sections': [
      'sections-intro',
      'sections-tutorial',
      'group-sections',
      'diff-sections',
      'sections-building-blocks',
      'recycler-collection-component',
      'hscrolls',
      'communicating-with-the-ui',
      'sections-testing',
      'sections-view-support',
      'services',
      'sections-architecture',
      'sections-working-ranges',
    ],
    'Common use cases': [
      'updating-ui',
       'borders',
       'tooltips',
       'saving-state',
    ],
    'Compatibility': [
      'styles',
      'accessibility',
      'rtl',
    ],
    'Testing': [
      'testing-overview',
      'unit-testing',
      'subcomponent-testing',
      'prop-matching',
      'testing-treeprops',
      'injectprop-matching',
      'event-handler-testing',
      'espresso-testing',
      'tests-in-android-studio',
    ],
    'Advanced Guides': [
      'architecture-overview',
      'recycler-component',
      'custom-layout',
      'inc-mount',
      'reconciliation',
      'component-tree',
      'error-boundaries',
      'onattached-ondetached',
    ],
    'Architecture': [
      'codegen',
      'asynchronous-layout',
      'inc-mount-architecture',
      'view-flattening',
      'recycling',
    ],
    'Additional Resources': [
      'best-practices',
      'faq',
      'glossary',
    ],
    'Tools': [
      'flipper-plugins',
      'debugging',
      'dev-options',
    ],
    'Contributing': [
      'contributing',
      'community-showcase',
      'repo-structure',
    ],
    ...fbInternalOnly({
      'Internal': [
        'fb/internal-litho',
      ]}),
  },
};
