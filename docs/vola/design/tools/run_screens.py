"""Build Vola v4 screen groups: python3 run_screens.py [group ...] (all groups when none given)."""
import sys

import build_v4_screens as screens

GROUPS = {name[2:]: fn for name, fn in vars(screens).items() if name.startswith('g_') and callable(fn)}

if __name__ == '__main__':
    names = sys.argv[1:] or list(GROUPS)
    for name in names:
        GROUPS[name]()
    print('built', names)
