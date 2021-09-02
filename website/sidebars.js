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
 *
 * @format
 */

const {fbContent, fbInternalOnly} = require('internaldocs-fb-helpers');
module.exports = {
  mainSidebar: {
    'What is Litho?': ['intro/motivation', 'intro/built-with-litho'],
    'Tutorial': [
      'tutorial/overview',
      'tutorial/project-setup',
      'tutorial/first-components',
      'tutorial/adding-state',
      'tutorial/handling-events',
      'tutorial/building-lists',
    ],
    'Main Concepts': [
      {
        'UI Composition': [
          'mainconcepts/uicomposition/layout-specs',
          'mainconcepts/uicomposition/mount-specs',
          'mainconcepts/uicomposition/flexbox-yoga',
        ],
        'Coordinating State and Actions': [
          'mainconcepts/coordinate-state-actions/state-overview',
          'mainconcepts/coordinate-state-actions/hoisting-state',
          'mainconcepts/coordinate-state-actions/events',
          'mainconcepts/coordinate-state-actions/trigger-events',
          'mainconcepts/coordinate-state-actions/treeprops',
          'mainconcepts/coordinate-state-actions/componenttree',
          'mainconcepts/coordinate-state-actions/dynamic-props',
        ],
        'Handling User Interactions': [],
      },
    ],
    'Building lists': [
      'sections/start',
      'sections/recycler-collection-component',
      'sections/best-practices',
      'sections/hscrolls',
      'sections/api-overview',
      'sections/working-ranges',
      'sections/services',
      'sections/view-support',
      'sections/diff-sections',
      'sections/architecture',
    ],
    ...fbInternalOnly({
      'Kotlin API': [
        'fb/kotlin/kotlin-intro',
        'fb/kotlin/kotlin-api-basics',
        {
          'Hooks and State': [
            'fb/kotlin/hooks-intro',
            'fb/kotlin/use-state',
            'fb/kotlin/use-effect',
            'fb/kotlin/use-ref',
            'fb/kotlin/use-transition',
          ],
        },
        'fb/kotlin/kotlin-flexbox-containers',
        'fb/kotlin/event-handling',
        'fb/kotlin/kotlin-api-cheatsheet',
      ],
    }),
    'Widgets': [
      'widgets/builtin-widgets',
      ...fbInternalOnly(['fb/widgets/design-components']),
    ],
    'Developer Tools': [
      'devtools/android-studio-plugin',
      'devtools/flipper-plugins',
    ],
    'Testing': [
      'testing/testing-overview',
      {
        'Unit Tests': [
          'testing/unit-testing',
          ...fbInternalOnly(['testing/fb/unit-testing-at-facebook']),
          'testing/subcomponent-testing',
          'testing/prop-matching',
          'testing/testing-treeprops',
          'testing/injectprop-matching',
          'testing/event-handler-testing',
          'testing/sections-testing',
        ],
      },
      {
        'UI Tests': [
          'testing/espresso-testing',
          ...fbInternalOnly(['testing/fb/buddy-tests-at-facebook']),
        ],
      },
      ...fbInternalOnly([
        {
          'Benchmark Tests': [
            'testing/fb/litho-benchmark-tests',
            {
              'MobileLab Tests': [
                'testing/fb/mobilelab-benchmark-tests/mobilelab-tests',
                'testing/fb/mobilelab-benchmark-tests/getting-started',
                'testing/fb/mobilelab-benchmark-tests/memory-benchmarks',
                'testing/fb/mobilelab-benchmark-tests/mobilelab-integration',
                'testing/fb/mobilelab-benchmark-tests/profiling-benchmarks',
              ],
            },
          ],
        },
      ]),
      'testing/tests-in-android-studio',
    ],
    'Animations': [
      'animations/transition-basics',
      'animations/transition-types',
      'animations/transition-all-layout',
      'animations/transition-choreography',
      'animations/transition-definitions',
      'animations/transition-key-types',
    ],
    'Accessibility': ['accessibility/accessibility-overview'],
    'Performance': [
      'performance/analysing-performance',
      'performance/spotting-performance-regressions',
    ],
    'Best Practices': [
      'best-practices/immutability',
      'best-practices/props-vs-state',
      'best-practices/coding-style',
    ],
    'Deep Dive': [
      {
        Reconciliation: [
          'deep-dive/reconciliation',
          'deep-dive/reconciliation/enabling-reconciliation',
          ...fbInternalOnly([
            'deep-dive/reconciliation/fb/when-to-use-reconciliation',
          ]),
        ],
      },
      'deep-dive/incremental-mount',
    ],
    'Contributing Documentation': [
      'contributing-documentation',
    ],
    ...fbInternalOnly({
      '[Internal]': [
        'fb/internal-litho',
        'fb/video-lessons',
        {
          Architecture: [
            'fb/architecture-sections-in-a-fragment-or-activity',
            'fb/architecture-thread-safety',
            'fb/architecture-litho-tricks',
          ],
        },
        'fb/dependency-injection',
        {
          'Analysing Performance': [
            'fb/analysing-performance-qpl',
            'fb/analysing-performance-spotting-performance-issues',
            'fb/analysing-performance-ttrc',
          ],
        },
        {
          'Error Handling': [
            'fb/error-boundaries',
            'fb/error-handling',
            'fb/error-handling-setting-a-default-error-event-handler',
          ],
        },
        'fb/experimentation',
        {
          'Open Source': [
            'fb/open-source',
            'fb/open-source-using-the-open-source-repo',
            'fb/open-source-releasing-litho',
          ],
        },
        'fb/sample-app',
      ],
    }),
    ...fbInternalOnly({
      '[Old Not Reused Content]': [
        {
          'Introducing Litho': ['intro', 'uses'],
          'Quick Start': [
            'getting-started',
            'tutorial',
            'writing-components',
            'using-components',
          ],
          'Reference': ['props', 'common-props', 'cached-values'],
          'Handling Events': ['events-touch-handling', 'visibility-handling'],
          'Sections': ['sections-tutorial', 'communicating-with-the-ui'],
          'Common use cases': [
            'updating-ui',
            'borders',
            'tooltips',
            'saving-state',
          ],
          'Compatibility': ['styles', 'rtl'],
          'Advanced Guides': [
            'architecture-overview',
            'recycler-component',
            'custom-layout',
            'onattached-ondetached',
          ],
          'Architecture': [
            'codegen',
            'asynchronous-layout',
            'view-flattening',
            'recycling',
          ],
          'Experimental': ['mount-extensions'],
          'Additional Resources': ['faq', 'glossary'],
          'Tools': ['debugging'],
          'Contributing': [
            'contributing',
            'community-showcase',
            'repo-structure',
          ],
        },
      ],
    }),
  },
};
