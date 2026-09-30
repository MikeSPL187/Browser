"""Check WCAG contrast of text/background token pairs in a generated CSS file."""
import re, sys
css = open(sys.argv[1]).read()
blocks = re.findall(r'(\.[^{]+)\{([^}]*)\}', css)
def lum(h):
    h = h.lstrip('#'); r, g, b = (int(h[i:i+2], 16) / 255 for i in (0, 2, 4))
    f = lambda c: c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b)
def ratio(a, b):
    la, lb = lum(a), lum(b); return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
pairs = [('on-sf','sf'),('on-sf-v','sf'),('on-sf','sf-high'),('on-sf-v','sf-highest'),('on-sf-v','sf-lowest'),('on-pri','pri'),('on-pri-c','pri-c'),('on-sec-c','sec-c'),('on-ter-c','ter-c'),('on-err-c','err-c'),('on-ok-c','ok-c'),('on-warn-c','warn-c'),('pri','sf'),('pri','sf-lowest'),('err','sf'),('ok','sf'),('warn','sf'),('inv-on-sf','inv-sf')]
bad = 0
for sel, body in blocks:
    v = dict(re.findall(r'--([\w-]+):\s*(#[0-9A-Fa-f]{6})', body))
    low = [(a, b, round(ratio(v[a], v[b]), 2)) for a, b in pairs if a in v and b in v and ratio(v[a], v[b]) < 4.5]
    print(sel.strip(), 'OK' if not low else low); bad += len(low)
sys.exit(1 if bad else 0)
