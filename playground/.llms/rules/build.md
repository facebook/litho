---
metadata:
  oncalls: ['litho_components_for_android']
---
# Playground Build Configuration

## Build Target

When modifying files in the Playground module, always run this buck build command to validate changes:

```bash
buck2 build litho
```

## Validation Workflow

After making changes to any file in the Playground directory:

1. Run AutodepsTool if imports were changed
2. Run the buck build command above
3. Report any build errors
