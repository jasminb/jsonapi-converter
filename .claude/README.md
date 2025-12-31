# Claude Context Directory

This directory contains project-specific context files that provide consistent background information and workflows for Claude Code development sessions.

## Directory Structure

### 📋 `/workflows/`
Development process and planning documents:

- **`golden_path_workflow.txt`** - Systematic 6-phase development workflow
  - Feature planning → Test scaffolding → Code generation → Local testing → Critical reassessment → PR preparation
  - Ensures quality through iterative validation and small diffs

- **`implementation_plan.md`** - JSON Library Abstraction Implementation Plan
  - Complete roadmap for abstracting Jackson dependencies
  - 7 epics with detailed work breakdown
  - Risk assessment and mitigation strategies

### 📚 `/documentation/`
Comprehensive codebase documentation:

- **`README.md`** - Master overview and architecture summary
- **`core-converter.md`** - ResourceConverter, ConverterConfiguration, JSONAPIDocument (20 classes)
- **`annotations.md`** - @Type, @Id, @Relationship and other annotations (8 annotations)
- **`exceptions.md`** - Custom exception classes and error handling (4 exceptions)
- **`error-models.md`** - JSON API error object models (4 classes)
- **`retrofit-integration.md`** - Retrofit framework integration (5 classes)

### 🔍 `/context/`
Current state analysis and findings:

- **`codebase_analysis.md`** - Assessment of current architecture, Jackson coupling, test coverage, and refactoring roadmap

## Usage Guidelines

### For New Development Sessions
1. **Review workflows** - Understand the Golden Path process
2. **Check implementation plan** - Know the current epic/phase status
3. **Read relevant documentation** - Understand the module being worked on
4. **Review codebase analysis** - Current state and key findings

### For Refactoring Work
- Follow **Golden Path workflow phases** systematically
- Refer to **implementation plan** for strategic guidance
- Use **documentation** to understand component interactions
- Check **codebase analysis** for complexity assessments

### For Code Reviews
- Verify changes align with **implementation plan** goals
- Ensure **Golden Path quality gates** are met
- Reference **documentation** for architectural consistency

## Current Project Status

**Phase**: Ready for Golden Path Phase 2 (Test Scaffolding)
**Test Status**: ✅ All 119 tests passing
**Next Epic**: Core JSON Abstraction Layer
**Complexity**: High (extensive Jackson coupling throughout codebase)

## Benefits

This organized context ensures:
- **Consistent Development Approach** - Same workflow regardless of session
- **Maintained Context** - No need to re-explain project background
- **Quality Assurance** - Systematic validation at each step
- **Documentation Currency** - Always up-to-date project understanding

---

*Maintained as part of JSON API Converter development workflow*