## Kilo Configuration Plan

### Finalized Configuration
```json
{
  "$schema": "https://app.kilo.ai/config.json",
  "mcp": {
    "ruflo": {
      "type": "local",
      "command": ["npx", "ruflo@latest", "mcp", "start"],
      "enabled": true,
      "timeout": 10000
    },
    "context7": {
      "type": "remote",
      "url": "https://mcp.context7.com/mcp",
      "enabled": true
    },
    "shadcn": {
      "type": "local",
      "command": ["npx", "-y", "@jpisnice/shadcn-ui-mcp-server"],
      "enabled": true
    },
    "playwright": {
      "type": "local",
      "command": ["npx", "-y", "@playwright/mcp@latest"],
      "enabled": true
    },
    "filesystem": {
      "type": "local",
      "command": ["npx", "-y", "@modelcontextprotocol/server-filesystem", "C:\\projects"],
      "enabled": true
    },
    "sequential-thinking": {
      "type": "local",
      "command": ["npx", "-y", "@modelcontextprotocol/server-sequential-thinking"],
      "enabled": true
    },
    "searxng": {
      "type": "local",
      "command": ["npx", "-y", "mcp-searxng"],
      "enabled": true,
      "environment": {
        "SEARXNG_URL": "http://localhost:8080"
      }
    }
  },
  "shell": "powershell",
  "logLevel": "INFO"
}
```

### Implementation Steps
1. **Run `kilo` with this config**:
   `kilo --config C:\projects\kilo\intensive-care-unit-patient-chart\.kilo\kilo.json`
2. **Verify MCP servers start**:
   - Check `mcp` logs for `ruflo`, `context7`, and others
3. **Test functionality**:
   - Confirm ollama models work via MCP
   - Validate filesystem MCP access to `C:\\projects`

### Prerequisites
- Ollama server must be running at `http://192.168.24.49:11434/v1`
- `mcp-searxng` requires Tor disabled or network access to `localhost:8080`

### Validation
1. Run `kilo doctor`
2. Check each MCP server status
3. Test model communication with a sample query