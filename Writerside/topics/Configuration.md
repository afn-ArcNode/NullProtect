# Configuration

```yaml
database:
  type: SQLite

  # MySQL
  host: "127.0.0.1"
  port: 3306
  username: NullProtect
  password: NullProtect
  database: NullProtect

# Thread pool settings
async:
  # fixed or virtual
  mode: fixed

  # Core pool size
  core: 10
  # Max threads
  max: 30

hwid:
  enabled: false

  # Timeout check interval (seconds)
  check-interval: 10
  # Timeout (seconds)
  timeout: 10

  # Enable HWID binding
  bind: false
  # whitelist or blacklist or none
  mode: none
  # Commands to execute on blacklist triggered or added when target player is online
  on-blacklist:
    - "ban %player% HWID banned from this server"

activation:
  enabled: false
  # Timeout (seconds)
  timeout: 10

  # Block actions if not activated
  blocking:
    chat: true
    move: true
    interact: true

# Fake plugin commands
fake:
  enabled: true
  # Fake version command tab-completes
  fake-version: true
  # Append fake plugin names to version command
  # Also creates fake version messages
  fake-version-plugins:
    NotProtect:
      author: AFterNode
      version: 114.514
  # Hide NullProtect
  hide-self: true

# Mods hash check
mods:
  enabled: false

  # Timeout check interval (seconds)
  check-interval: 10
  # Timeout (seconds)
  timeout: 10

```

- `database`
  - `type` Database type, `SQLite` for file or `MySQL` for remote MySQL

- `async` Thread pool settings (Java ExecutorService)
  - `mode` `fixed` for ThreadPoolExecutor or `virtual` for VirtualThreadPerTask
  - `core` Core pool size (fixed mode)
  - `max` Maximum pool size (fixed mode)

- `hwid`
  - `enabled` Enable HWID verification
  - `check-interval` Timeout check interval (seconds)
  - `timeout` HWID packet timeout (seconds)
  - `bind` Bind player to HWID
  - `mode` HWID matching mode `whitelist/blacklist/none`
  - `on-blacklist` Command to execute on blacklist triggered or added when target player is online, its recommend to ban player to reduce database pressure

- `activation` Player account activation
  - `enabled` Enable account activation
  - `timout` Time to wait for activation
  - `blocking` Block player actions if account not activated
  
- `fake` Fake plugin commands
  - `fake-version` Fake in `version`(`ver`) command and tab-completes
  - `fake-version-plugins` Append fake plugins to `version` command
  - `hide-self` Hide self in `version` command (for everyone)

- `mods` Mods hash check
  - `enabled` Enable mods hash check
  - `check-interval` Timeout check interval (seconds)
  - `timeout` Hash packet timeout (seconds)
