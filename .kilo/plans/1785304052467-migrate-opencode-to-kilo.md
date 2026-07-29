# Plan: Migrate OpenCode Settings to Pure Kilo Configuration

## Objective
Transform the current `kilo.json` to use only Kilo-compatible configuration, removing all OpenCode-specific extensions while preserving MCP servers and shell settings.

## Current State
- `kilo.json` only contains context7 MCP server (minimal)
- OpenCode settings (in `.opencode/opencode.json`) contains:
  - Ollama provider with 4 models
  - 6 MCP servers: context7, shadcn, playwright, filesystem, sequential-thinking, searxng
  - Shell: powershell

## Target State
A valid Kilo configuration with:
- Shell set to `powershell`
- Log level set to `INFO`
- MCP servers: context7 (remote), shadcn, playwright, filesystem, sequential-thinking, searxng (all local)
- No OpenCode-specific fields (refs, workflows, remotes, opentype, etc.)

## Tasks

### 1. Update kilo.json with Complete MCP Configuration
- Add all 6 MCP servers from OpenCode config
- Map each to Kilo's `McpLocalConfig` or `McpRemoteConfig` schema
- Set appropriate `cwd`, `environment`, `timeout` for each
- Ensure `enabled: true` for all

### 2. Set Shell and Log Level
- `"shell": "powershell"`
- `"logLevel": "INFO"`

### 3. Remove OpenCode-Only Fields
- No `provider` configuration (Kilo uses different provider model)
- No `remotes`, `opentype`, `remoteControl`, `nativeNotebookTools`, etc.
- No `model` / `smallModel` (Kilo handles models differently)

### 4. Validate Against Kilo Schema
- Ensure all fields match Kilo's `Config` type
- Timeout values must be integers within valid range
- Paths must be valid for Windows PowerShell

## MCP Server Mappings

| OpenCode MCP | Kilo Type | Command | CWD | Timeout |
|--------------|-----------|---------|-----|---------|
| context7 | remote | N/A (url) | - | 30000 |
| shadcn | local | `npx @jpisnice/shadcn-ui-mcp-server` | frontend | 20000 |
| playwright | local | `npx -y @playwright/mcp@latest` | tests | 20000 |
| filesystem | local | `npx -y @modelcontextprotocol/server-filesystem C:\projects\kilo\intensive-care-unit-patient-chart` | . | 45000 |
| sequential-thinking | local | `npx -y @modelcontextprotocol/server-sequential-thinking` | .kilo | 30000 |
| searxng | local | `npx -y mcp-searxng` | .opencode | 30000 |

## Validation Steps
1. After update, run `kilo doctor` or equivalent to validate config
2. Test each MCP server starts correctly
3. Verify no schema validation errors

## Risks
- Path separators: Windows paths need escaping in JSON
- Timeout values: Must be integers (not strings)
- `cwd` paths: Relative to workspace root

## Success Criteria
- `kilo.json` passes Kilo schema validation
- All 6 MCP servers can be started by Kilo
- Shell is correctly set to powershell
- No OpenCode-specific fields remain