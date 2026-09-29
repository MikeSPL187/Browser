# Workspaces

Workspaces (RU: «Пространства») are Vola's take on Zen Browser workspaces: separate sets of tabs,
each with a name, an emoji and an accent color. In code they are still `BrowserProfile`
(`shared/.../browser/BrowserProfile.kt`); only the UI calls them workspaces.

## Model

| Field | Meaning |
| --- | --- |
| `name` | User-given name, normalized by `WorkspaceNameRules` (trimmed, single spaces, at most 32 characters). Blank shows a localized default: «Личное» / Personal for the default workspace, «Пространство» / Workspace otherwise. |
| `emoji` | Icon in the workspace switcher. The default workspace starts with 🏠. |
| `accent` | `WorkspaceAccent`: violet, blue, teal, green, amber, coral, rose or graphite. Colors live in `ui/theme/WorkspaceAccents.kt` (tone 40 for light themes, tone 80 for dark). |
| `isolationEnabled` | Container mode: separate cookies, sign-ins, site data and cache. |
| `protection`, wallpapers | Unchanged from the upstream profile model. |

`name` and `accent` are stored with the other profile fields in the `profiles` JSON of
`BrowserSessionStore`. Older data without them loads with a blank name and the default accent.
Synced workspaces from other devices keep their synced name and icon and cannot be renamed or
recolored locally.

## Editing

The create sheet and the workspace options sheet (long press on a workspace in the tab overview)
share `WorkspaceIdentityEditor`: a name field and an accent row. A new name is saved when the
options sheet closes or on the keyboard's Done action, not on every keystroke. A Site Capsule that
gets its own dedicated workspace names it after the capsule.

## Next steps

1. The accent tints the browser chrome: address bar, tab overview and new tab background.
2. Switching workspaces by swiping in the tab overview, plus a workspace strip.
3. Essentials: pinned sites per workspace above the tab overview.
