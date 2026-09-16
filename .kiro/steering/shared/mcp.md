---
inclusion: always
---

# MCP (Model Context Protocol) Standards

## Configuration
- MCP server configs live in `.kiro/settings/mcp.json`
- User-level configs at `~/.kiro/settings/mcp.json`
- Workspace configs override user-level configs

## Approved MCP Servers
List your approved MCP servers here:
- (Add your approved servers)

## Usage Guidelines
- Only use approved MCP servers
- Document any new MCP server additions in a decision record
- Test MCP tools in development before enabling for the team
- Set appropriate environment variables for each server
- Use `disabled: true` to temporarily disable servers without removing config

## Security
- Never store secrets directly in mcp.json
- Use environment variable references for sensitive values
- Review MCP server permissions before enabling
