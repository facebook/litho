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

const {fbContent, fbInternalOnly} = require('docusaurus-plugin-internaldocs-fb/internal');
module.exports = {
  mainSidebar: [
    {
      // TODO: update content
      'What is Litho?': ['intro/motivation'],
      'Tutorial': [
        'tutorial/overview',
        // TODO: add kotlin dep
        'tutorial/project-setup',
        'tutorial/first-components',
        'tutorial/introducing-layout',
        'tutorial/adding-state',
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
            'mainconcepts/use-ref',
            'mainconcepts/use-cached',
            'mainconcepts/use-error-boundary',
            'mainconcepts/use-live-data',
            'mainconcepts/use-callback'
          ],
        },
        {
          'Primitive Components': [
            'mainconcepts/primitivecomponents/overview',
            'mainconcepts/primitivecomponents/primitive-component',
            'mainconcepts/primitivecomponents/primitive',
            'mainconcepts/primitivecomponents/primitive-measuring',
            'mainconcepts/primitivecomponents/primitive-controllers',
            'mainconcepts/primitivecomponents/primitive-preallocation',
            'mainconcepts/primitivecomponents/primitive-tracing',
          ],
        },
        {
          'Layout System with Flexbox': [
            'mainconcepts/flexbox-yoga',
            'mainconcepts/yoga-playground',
            'mainconcepts/troubleshooting',
          ],
        },
        'mainconcepts/coordinate-state-actions/keys-and-identity',
      ],
      'Building Lists': [
        'kotlin/lazycollections/lazycollections',
        'kotlin/lazycollections/lazycollections-layout',
        'kotlin/lazycollections/lazycollections-interactions',
        'kotlin/lazycollections/lazycollections-working-with-updates',
      ],
      'Animations': [
        'animations/transition-basics',
        'animations/transition-types',
        'animations/transition-all-layout',
        'animations/transition-choreography',
        'animations/dynamic-props-bindto',
        'animations/transition-key-types',
      ],
    },
    'mainconcepts/coordinate-state-actions/visibility-handling',
    'accessibility/accessibility',
    {
      'Testing': [
        'kotlin/testing-getting-started',
        'kotlin/testing-assertions',
        'kotlin/testing-actions',
      ],
      'Widgets': [
        'widgets/builtin-widgets',
        'widgets/canvas',
        ...fbInternalOnly(['fb/widgets/design-components']),
      ],
      'Adopting Litho': [
          'kotlin/migration-strategies',
          'kotlin/custom-view-compat',
        ],
      'Spec APIs': [
        'codegen/overview',
        'codegen/layout-specs',
        'codegen/mount-specs',
        'codegen/dynamic-props',
        'codegen/transition-definitions',
        'codegen/accessibility-overview',
        {
          'Sections API': [
            'sections/start',
            'sections/recycler-collection-component',
            'sections/best-practices',
            'sections/hscrolls',
            {
              'Advanced': [
                'sections/working-ranges',
                'sections/communicating-with-the-ui',
                'sections/services',
                'sections/view-support',
                'sections/diff-sections',
                'sections/architecture',
              ],
            },
          ],
        },
        {
          'Props, State, and Events for Specs': [
            'codegen/passing-data-to-components/spec-props',
            'codegen/passing-data-to-components/treeprops',
            'codegen/state-for-specs',
            'codegen/events-for-specs',
            'codegen/trigger-events',
          ],
        },
        'codegen/coding-style',
      ],
      'Migrating to the Kotlin API': [
        'kotlin/kotlin-intro',
        'kotlin/kotlin-api-basics',
        'kotlin/hooks-for-spec-developers',
        'kotlin/kotlin-flexbox-containers',
        'kotlin/event-handling',
        'kotlin/migrating-from-mountspecs-to-primitives',
        'kotlin/lazycollections/lazycollections-sections-migration',
        ...fbInternalOnly(['kotlin/lazycollections/fb/lazycollections-fb-internal']),
        'kotlin/kotlin-api-cheatsheet',
      ],
      // TODO: clean this section up, add intro page
      'Tooling': [
        {
          'Android Studio': [
            'ide/android-studio-plugin',
            ...fbInternalOnly(['ide/fb/android-studio-ui-preview-intro']),
            ...fbInternalOnly(['ide/fb/android-studio-ui-preview']),
            ...fbInternalOnly(['ide/fb/android-studio-ui-preview-setup']),
          ],
          'Debugging': [
            'debugging/debugging-tips',
            'debugging/debugging-sections',
            ...fbInternalOnly(['debugging/fb/debugging-time-travel']),
          ],
          'Developer Tools': ['devtools/flipper-plugins'],
        },
      ],
      'Best Practices': [
        'best-practices/immutability',
        'best-practices/props-vs-state',
        'mainconcepts/coordinate-state-actions/hoisting-state',
        'mainconcepts/coordinate-state-actions/communicating-between-components',
      ],
      ...fbInternalOnly({
        'Contributing to the Documentation': [
          'fb/documentation/documentation-standards',
          'fb/documentation/documentation-workflow',
          'fb/documentation/add-new-page',
          'fb/documentation/writing-guide',
          'fb/documentation/formatting-tips',
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
            'Sections': ['sections-tutorial'],
            'Common use cases': [
              'updating-ui',
              'borders',
              'tooltips',
              'saving-state',
            ],
            'Compatibility': ['styles'],
            'Advanced Guides': [
              'mainconcepts/coordinate-state-actions/componenttree',
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
  ]
};
