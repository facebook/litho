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
module.exports = {
  mainSidebar: {
    // TODO: update content
    'What is Litho?': ['intro/motivation', 'intro/built-with-litho'],
    'Tutorial': [
      'tutorial/overview',
      // TODO: add kotlin dep
      'tutorial/project-setup',
      'tutorial/first-components',
      'tutorial/adding-state',
      'tutorial/handling-events',
      'tutorial/building-lists',
    ],
    'Main Concepts': [
      'mainconcepts/components-basics',
      'mainconcepts/props',
      {
        'Hooks and State': [
          'mainconcepts/hooks-intro',
          'mainconcepts/use-state',
          'mainconcepts/use-effect',
        ],
      },
      'mainconcepts/uicomposition/flexbox-yoga',
      {
        // TODO: remove/replace 'Coordinating State and Actions'
        'Coordinating State and Actions': [
          'mainconcepts/coordinate-state-actions/state-overview',
          'mainconcepts/coordinate-state-actions/hoisting-state',
          'mainconcepts/coordinate-state-actions/events',
          'mainconcepts/coordinate-state-actions/communicating-between-components',
          'mainconcepts/coordinate-state-actions/trigger-events',
          'mainconcepts/coordinate-state-actions/componenttree',
          'mainconcepts/coordinate-state-actions/visibility-handling',
          'mainconcepts/coordinate-state-actions/keys-and-identity',
        ],
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
    // TODO: revisit wording to make it clear it's not just the 'kotlin' testing API
    'Testing': [
      'kotlin/testing-getting-started',
      'kotlin/testing-assertions',
      'kotlin/testing-actions',
    ],
    // TODO: de-dupe content with main concepts
    ...fbInternalOnly({
      'Migrating to the Kotlin API': [
        'kotlin/kotlin-intro',
        'kotlin/kotlin-api-basics',
        {
          'Hooks and State': [
            'kotlin/hooks-for-spec-developers',
            'kotlin/use-ref',
            'kotlin/use-transition',
            'kotlin/use-error-boundary',
          ],
        },
        'kotlin/kotlin-flexbox-containers',
        'kotlin/collections',
        'kotlin/event-handling',
        'kotlin/kotlin-api-cheatsheet',
      ],
    }),
    // TODO: clean this section up, add intro page
    'Codegen APIs': [
      'mainconcepts/uicomposition/layout-specs',
      'mainconcepts/uicomposition/mount-specs',
      {
        'Passing Data To Components': [
          'mainconcepts/passing-data-to-components/props',
          'mainconcepts/passing-data-to-components/treeprops',
        ],
      },
    ],
    'Animations': [
      'animations/transition-basics',
      'animations/transition-types',
      'animations/transition-all-layout',
      'animations/transition-choreography',
      'animations/transition-definitions',
      'animations/transition-key-types',
      'animations/dynamic-props',
    ],
    'Widgets': [
      'widgets/builtin-widgets',
      ...fbInternalOnly(['fb/widgets/design-components']),
    ],
    'Accessibility': ['accessibility/accessibility-overview'],
    // TODO: clean this section up, add intro page
    'Tooling': [
      {
        'Debugging': [
          'debugging/debugging-tips',
          'debugging/debugging-sections',
          ...fbInternalOnly(['debugging/fb/debugging-time-travel']),
        ],
        'Developer Tools': [
          'devtools/android-studio-plugin',
          'devtools/flipper-plugins',
        ],
      },
      'performance/analysing-performance',
    ],
    'Best Practices': [
      'best-practices/immutability',
      'best-practices/props-vs-state',
      'best-practices/coding-style',
    ],
    ...fbInternalOnly({
      'Contributing to the Documentation': [
        'fb/documentation/contributing-documentation',
        'fb/documentation/formatting-tips',
        'fb/documentation/writing-guide',
      ],
    }),
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

    //
    // Begin unused content
    //

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
          'Reference': ['common-props', 'cached-values'],
          'Handling Events': ['events-touch-handling'],
          'Sections': ['sections-tutorial', 'communicating-with-the-ui'],
          'Common use cases': [
            'updating-ui',
            'borders',
            'tooltips',
            'saving-state',
          ],
          'Compatibility': ['styles'],
          'Advanced Guides': [
            'architecture-overview',
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
          'Contributing': [
            'contributing',
            'community-showcase',
            'repo-structure',
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
            {
              Debugging: ['annotation-processor-debugging'],
            },
          ],
        },
      ],
    }),
  },
};
