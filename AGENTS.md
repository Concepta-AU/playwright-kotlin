# playwright-kotlin — Documentation Conventions

## Documentation Structure

```
playwright-kotlin/
├── README.md              # Project overview, purpose, "why Kotlin", API example. Entry point.
├── docs/                  # User-facing documentation (detailed/reference)
│   ├── getting-started.md # Dependency setup, full pattern walkthrough, extension function usage
│   └── running-tests.md   # Test execution, env vars, traces, accessibility
├── src/
│   └── ...
└── AGENTS.md              # This file — development conventions
```

## README vs. docs/

- **README.md** is the landing page. Keep it short: project identity, what it does, why it matters, and links to detailed docs. Never duplicate content that belongs in `docs/`.
- **docs/** holds everything else — getting started guides, reference pages, architecture deep-dives, etc.
- The README must always have a **Documentation** table/link section near the top so users can find detailed content.

## Writing Guidelines

- **Concise over complete.** Prefer short sections with clear headings over long walls of text.
- **Examples first.** When explaining a pattern, show working code before describing it.
- **Reference > tutorial for running tests.** The `running-tests.md` page is a reference (env vars, flags, output format), not a step-by-step walkthrough. Walkthroughs belong in `getting-started.md`.
- **Cross-link.** If a concept is explained in detail in one doc and referenced in another, link rather than repeat.

## Adding New Documentation

1. Create the file in `docs/`.
2. Add a row to the Documentation table in `README.md` if it's user-facing.
3. Keep the file title as an H1 matching the filename (e.g., `# Getting Started` in `getting-started.md`).

## KDoc on Public API

Public classes (`Application`, `ApplicationPage`, `TestBase`) and their public/protected members should have KDoc. Extension function files are self-explanatory mirrors of the Playwright API and don't need per-function KDoc, but file-level comments explaining the pattern are encouraged.
