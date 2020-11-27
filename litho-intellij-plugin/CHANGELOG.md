# CHANGELOG

## Version 1.6.2

-2020-xx-xx_

* Fix: Change logging for method/param goto navigation to include class.

## Version 1.6.1

_2020-11-19_

* New: Dynamic OnUpdateState method template.
* New: Additional method completions for `Spec`.

## Version 1.6.0

_2020-11-4_

* New: "Resolve Litho Red Symbols" for `MountSpec`.
* New: Warning Annotations for `MountSpec`.
* New: "Litho Spec" view for `MountSpec`.
* New: "Regenerate Component" for `MountSpec`.

## Version 1.5.11

_2020-10-20_

* New: `MountSpec` method completion.
* New: Highlight `Param` as `MountSpec` method parameter.

## Version 1.5.10

_2020-10-05_

* New: More straightforward `OnEvent` generation.
* New: Resolve `OnEvent` name conflicts.
* New: `OnEvent` completion in the method arguments.

## Version 1.5.9

_2020-09-11_

* New: `Prop` navigation.

## Version 1.5.8

_2020-08-10_

* New: LayoutSpec Annotator re-generates Component.
* New: LayoutSpec Annotator analyzes PsiFile.
* New: Red Symbols analysis restarts on file open.
* New: Red Symbols analysis uses non-blocking Read action.
* New: Red Symbols analysis uses MarkupModel instead of Daemon analysis.

## Version 1.5.7

_2020-07-14_

* New: Toolwindow auto updates.
* New: Kotlin LayoutSpec template.
* Fix: Red symbols binding on disposed Editor.

## Version 1.5.6

_2020-07-01_

* Fix: Toolwindow icon.
* New: Change log tags.

## Version 1.5.5

_2020-06-19_

* New: Change log tags.

## Version 1.5.4

_2020-06-17_

* New: Add tool window to show `@LayoutSpec` Structure.
* New: Log red symbols binding time.
* New: Increase supported version to 1.9.1.

## Version 1.5.3

_2020-06-09_

* New: Increase logging interval for method navigation.
* New: Add logging for Settings update.

## Version 1.5.2

_2020-06-06_

* Fix: Omit `.class` during Component generation.

## Version 1.5.1

_2020-06-04_

* New: Add navigation from Component method to Spec method.
* New: FindUsages of Spec method include Component method.
* New: Add method completion for LayoutSpec methods: OnAttached, OnCreateInitialState, OnCreateLayout, OnCreateLayoutWithSizeSpec, OnCreateTransition, OnDetached, OnUpdateState, OnUpdateStateWithTransition.

## Version 1.5.0

_2020-05-29_

* New: Add Settings to resolve Litho red symbols automatically.

## Version 1.4.2

_2020-05-20_

* Fix: Sending Events without metadata.

## Version 1.4.1

_2020-05-16_

* New: Add template for `Event`.
* New: NewTemplate actions are divided into separate menu items.
* New: Remove `BuildInfoProvider` extension point.
* New: Add `TemplateProvider` extension point.
* New: Add more logs to `ResolveRedSymbols` Action.

## Version 1.4.0

_2020-04-20_

* New: Litho red symbols resolution.
  - New menu action to trigger in-memory component creation.
  - Update red symbols in the file after triggered action.
  - Enable method completion for in-memory components.
  - Enable FindUsages action to work with created in-memory components.
  - Enable RegenerateComponent action to create in-memory component if it doesn't exist.
  - Clean-up in-memory components when file system changes are detected.
  - New AddImportFix for in-memory components.
* New: Add details to the line marker message about missing required prop.
* Fix: Builder formatting when it is inserted in a nested method.
* Fix: Double Spec suffix when creating new file from a template.

## Version 1.3.1

_2020-01-07_

* New: Logtag for method annotations and method parameter annotations.
* New: Line marker for missing required prop.

## Version 1.3.0

_2019-12-13_

* New: QuickFix to create new `EventHandler` as `Component.Builder` method parameter.
* New: Annotation completion for LayoutSpec delegate method parameters.
* New: BUCK file
* Fix: More precise LayoutSpec log tag.
* Fix: AddArgumentFix logging.

## Version 1.2.0

_2019-11-29_

* New: QuickFix to add existing `EventHandler` as `Component.Builder` method parameter.
* New: Prioritize `Component.Builder` with required properties completion.

## Version 1.1.1

_2019-11-21_

* Fix: `RequiredPropMethodContributor` doesn't create duplicate methods for `Component.Builder`.
* Fix: `RequiredPropMethodContributor` sets high priority to `Component` required methods.
* Fix: `TypeSpec.DeclaredTypeSpec` usage is aligned with changes in its constructor in `PsiTypeUtils`.
* Fix: `RequiredPropAnnotator` logging interval is increased.

## Version 1.1.0

_2019-11-13_

* New: Annotator for missing Required Props in a single statement
* New: Component builder completion with Required Props
* New: Accent on Required Prop setters
* New: Regenerate component notification
* New: Plugin icon
* New: BuildInfoProvider extension point
* Fix: PsiAnnotationProxy comparison
* Fix: Parsing for method generic type

## Version 1.0.0

_2019-10-01_

* New: GoTo Component action in EditorPopup menu
* New: List of available method annotations in the LayoutSpec completion suggestions
* New: Hide generated class from the found usages

## Version 0.0.9

_2019-09-09_

* Fix: Array type parsing
* Fix: Trigger method return type parsing

## Version 0.0.8

_2019-08-16_

* Fix: Disable Regenerate component on file saving
* Fix: Error on navigating to unrecognized ComponentSpec

## Version 0.0.7

_2019-08-12_

* Fix: Range method parsing
* Fix: Wildcard parsing
* Fix: Section context for the generated events

## Version 0.0.6

_2019-07-22_

* New: FindUsages shows Component occurrences in the search results
* New: Component is regenerated on file saving

## Version 0.0.5

_2019-07-02_

* New: Editor menu Regenerate Component action
* New: Component is updated after Event Handler completion

## Version 0.0.4

_2019-06-20_

* Fix: Minor bugs

## Version 0.0.3

_2019-06-18_

* New: Logging as an extension point
* Fix: Event completion return value

## Version 0.0.2

_2019-06-04_

* New: On-the-go error check is enabled for LayoutSpec
* New: Completion for ClickEvent handler
* New: File New menu contains Litho Mount Component option
* New: File New menu contains Litho GroupSection Component option

## Version 0.0.1

_2019-04-17_

* New: Generate menu contains OnEvent method creation option
* New: Command-click on a LithoSection takes to the SectionSpec
* New: Command-click on a LithoComponent takes to the ComponentSpec
* New: @DefaultProp value is shown near the @Prop parameter as a folding
* New: @Prop and @State method parameters have completion suggestions
* New: File New menu contains Litho Layout Component option
