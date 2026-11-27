# Release configuration

## Overview

This directory owns the executable release policy consumed by the `release-tools` module.

The policy governs RAG scope, evidence freshness, APK identity and publication. Shared test collections, governance rules and game strategy remain in the root `config/` directory because several modules consume them.

- Policy file: `release-platform-rag-policy.json`.
- When a policy reader or dependency changes, update the component dependency inventory and its reader seals.
