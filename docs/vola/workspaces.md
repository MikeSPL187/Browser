# Workspaces

Workspaces (RU: «Пространства») are Vola's take on Zen Browser workspaces: separate sets of tabs,
each with a name, an icon and an accent color. In code they are still `BrowserProfile`
(`shared/.../browser/BrowserProfile.kt`); only the UI calls them workspaces.

## Model

| Field | Meaning |
| --- | --- |
| `name` | User-given name, normalized by `WorkspaceNameRules` (trimmed, single spaces, at most 32 characters). Blank shows a localized default: «Личное» / Personal for the default workspace, «Пространство» / Workspace otherwise. |
| `emoji` | Stable icon key, stored and synced as before. Vola never draws it: `WorkspaceIcons` maps every key of the sync icon catalog to a Material Symbol (Rounded), tinted like the surrounding content. The default workspace uses the home icon. |
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

## Icons

`scripts/workspace-icons/icons.txt` maps each catalog id (`sync/protocol/device-icons-v1.json`) to a
Material Symbols name. `scripts/workspace-icons/generate.py` downloads the symbols and writes
`WorkspaceIcons.kt` (Compose vectors), `WidgetWorkspaceIcons.kt` and the `ic_widget_workspace_*`
drawables for the home screen widget. Unknown keys, such as an emoji from older data, fall back to
the star icon. Everywhere a workspace used to be shown as a bare emoji (history and filter chips,
command palette, external link bar, launcher shortcuts, widget) it now shows the icon and the
workspace name.

## Next steps

1. The accent tints the browser chrome: address bar, tab overview and new tab background.
2. Switching workspaces by swiping in the tab overview, plus a workspace strip.
3. Essentials: pinned sites per workspace above the tab overview.
