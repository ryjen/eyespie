# EyesPie Documentation Index

Welcome to the EyesPie documentation. This index provides an overview of all available documentation.

## Quick Links

- [README](README.md) — Project overview and getting started
- [CONTRIBUTING](CONTRIBUTING.md) — How to contribute
- [ARCHITECTURE](ARCHITECTURE.md) — Technical architecture
- [Public client / private product-services boundary](docs/architecture/public-client-private-services-boundary.md) — Repository, dependency, and licensing boundary
- [LICENSES](LICENSES.md) — Repository and nested-license scope
- [DEVELOPMENT](DEVELOPMENT.md) — Development guide
- [SECURITY](SECURITY.md) — Security policy

## Documentation Overview

### For New Users

Start here if you're new to EyesPie:

1. **[README](README.md)** — Project overview, features, and quick start
2. **[GETTING_STARTED.md](GETTING_STARTED.md)** — Detailed setup instructions

### For Contributors

If you want to contribute to EyesPie:

1. **[CONTRIBUTING.md](CONTRIBUTING.md)** — Contribution guidelines and workflow
2. **[ARCHITECTURE.md](ARCHITECTURE.md)** — Understand the codebase structure
3. **[DEVELOPMENT.md](DEVELOPMENT.md)** — Development environment setup
4. **[LICENSES.md](LICENSES.md)** — Understand MPL-2.0 root scope and nested component licenses

### For Developers

Technical documentation for developers:

1. **[ARCHITECTURE.md](ARCHITECTURE.md)** — System architecture and patterns
2. **[Public client / private product-services boundary](docs/architecture/public-client-private-services-boundary.md)** — Public/private repository and dependency direction
3. **[DEVELOPMENT.md](DEVELOPMENT.md)** — Build system, debugging, and tools
4. **[CLAUDE.md](CLAUDE.md)** — AI assistant guidance

### For Security Researchers

Security-related documentation:

1. **[SECURITY.md](SECURITY.md)** — Security policy and reporting
2. **[Security and privacy architecture](docs/architecture/security-and-privacy.md)** — Product trust boundaries and privacy architecture

## Documentation by Topic

### Getting Started

- [Installation](README.md#prerequisites)
- [Environment Setup](README.md#getting-started)
- [First Build](README.md#build-the-project)
- [Running the App](README.md#run-the-app)

### Architecture

- [System Overview](ARCHITECTURE.md#overview)
- [Module Structure](ARCHITECTURE.md#module-structure)
- [Data Flow](ARCHITECTURE.md#data-flow)
- [Dependency Injection](ARCHITECTURE.md#dependency-injection)
- [Public/private repository boundary](docs/architecture/public-client-private-services-boundary.md)
- [Security and privacy](docs/architecture/security-and-privacy.md)

### Development

- [Build Commands](DEVELOPMENT.md#build-system)
- [Debugging](DEVELOPMENT.md#debugging)
- [Performance](DEVELOPMENT.md#performance-profiling)
- [IDE Setup](DEVELOPMENT.md#ide-configuration)

### Contributing

- [Code Style](CONTRIBUTING.md#code-style-guidelines)
- [Testing](CONTRIBUTING.md#testing-requirements)
- [Pull Requests](CONTRIBUTING.md#pull-request-process)
- [Issue Guidelines](CONTRIBUTING.md#issue-guidelines)

### Features

- [Core Features](README.md#features)
- [AI/ML Pipeline](ARCHITECTURE.md#machine-learning-pipeline)
- [Offline Support](ARCHITECTURE.md#offline-support)
- [Real-time Features](ARCHITECTURE.md#real-time-features)

### Security

- [Security Measures](SECURITY.md#security-measures)
- [Best Practices](SECURITY.md#best-practices)
- [Reporting Vulnerabilities](SECURITY.md#reporting-a-vulnerability)
- [Security and privacy architecture](docs/architecture/security-and-privacy.md)

### Licensing and commercial distribution

- [Repository license boundaries](LICENSES.md)
- [Public client / private product-services boundary](docs/architecture/public-client-private-services-boundary.md)
- [Commercial distribution inventory](docs/commercial/commercial-distribution-inventory.md)
- [Provenance evidence](docs/commercial/provenance-evidence.md)
- [Bluebell provenance delta](docs/commercial/bluebell-provenance-delta.md)
- [SBOM license-gap review](docs/commercial/sbom-license-gap-review.md)
- [Monetization strategy](docs/product/monetization.md)

Historical commercial/provenance documents may discuss the repository's former GPLv3 state because that history remains relevant evidence. The current repository-level licensing decision is MPL-2.0 with the embedded `bluebell/` subtree separately Apache-2.0; use `LICENSES.md` and the repository-boundary architecture document for current scope.

## Design Documentation

Located in the `design/` directory:

- **[overview.md](design/overview.md)** — User flow overview
- **[features.md](design/features.md)** — Feature specifications
- **[workflow.md](design/workflow.md)** — Detailed workflows
- **[enhancements.md](design/enhancements.md)** — Planned enhancements

## AI Assistant Documentation

For AI assistants working with the codebase:

- **[AGENTS.md](AGENTS.md)** — Repository guidelines for AI agents
- **[CLAUDE.md](CLAUDE.md)** — Claude-specific guidance

## Project Management

- [Project Board](https://github.com/orgs/hackelia-micrantha/projects/3/views/2) — Sprint planning
- [Issues](https://github.com/ryjen/eyespie/issues) — Bug reports and features

## Documentation Structure

```text
eyespie/
├── README.md                 # Project overview
├── CONTRIBUTING.md           # Contribution guidelines
├── ARCHITECTURE.md           # Technical architecture
├── DEVELOPMENT.md            # Development guide
├── SECURITY.md               # Security policy
├── DOCS.md                   # This file
├── AGENTS.md                 # AI agent guidelines
├── CLAUDE.md                 # Claude-specific guidance
├── LICENSE                   # MPL-2.0 root license
├── LICENSES.md               # Repository/nested license scope
├── bluebell/
│   └── LICENSE               # Apache-2.0 nested Bluebell scope
├── design/                   # Design documentation
└── docs/
    ├── architecture/         # Architecture decisions and boundaries
    ├── commercial/           # Commercial provenance/distribution evidence
    ├── product/              # Product and monetization design
    ├── release/              # Release planning and evidence
    └── security/             # Threat/privacy/security documentation
```

## Contributing to Documentation

### How to Update

1. **Edit directly**: Use GitHub's web editor
2. **Local editing**: Clone and edit, then submit PR
3. **Suggestions**: Open an issue with documentation tag

### Documentation Standards

- **Format**: Markdown (`.md`)
- **Style**: Clear, concise, developer-focused
- **Code Examples**: Include working examples
- **Screenshots**: Add visuals for UI documentation where useful
- **Links**: Cross-reference related documentation
- **Current versus historical decisions**: Preserve historical evidence, but explicitly identify superseded policy/license state

### Review Process

1. Submit documentation changes via PR
2. Review for accuracy and clarity
3. Check for broken links and stale policy references
4. Merge after approval

## Getting Help

### Documentation Issues

- **Missing docs**: Open an issue with `documentation` context
- **Incorrect info**: Submit a PR with correction
- **Unclear content**: Open an issue for clarification

---

*Last updated: August 2026*