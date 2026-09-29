#!/usr/bin/env python3
"""Checks that every translated Android string resource matches its English source.

Lint already reports missing translations; this adds the checks lint cannot do reliably for
Russian: identical format placeholders, safe apostrophes and quotes, and complete
one/few/many/other plural forms.
"""

import re
import sys
import unittest
from collections import Counter
from pathlib import Path
from xml.etree import ElementTree

RES = Path(__file__).resolve().parent.parent / "app/src/main/res"
LOCALES = {"ru": ("one", "few", "many", "other")}
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[sdfxXc%]")


def inner_text(element):
    text = element.text or ""
    for child in element:
        text += ElementTree.tostring(child, encoding="unicode")
    return text


def load(directory):
    strings, plurals = {}, {}
    for path in sorted(directory.glob("*.xml")):
        root = ElementTree.parse(path).getroot()
        for element in root:
            name = element.get("name")
            if element.get("translatable") == "false":
                continue
            if element.tag == "string":
                strings[name] = inner_text(element)
            elif element.tag == "plurals":
                plurals[name] = {item.get("quantity"): inner_text(item) for item in element}
    return strings, plurals


class TranslationTest(unittest.TestCase):
    def setUp(self):
        self.strings, self.plurals = load(RES / "values")

    def test_locales(self):
        for locale, quantities in LOCALES.items():
            with self.subTest(locale=locale):
                self.check_locale(locale, quantities)

    def check_locale(self, locale, quantities):
        strings, plurals = load(RES / f"values-{locale}")
        problems = []
        problems += [f"missing string {name}" for name in self.strings.keys() - strings.keys()]
        problems += [f"unknown string {name}" for name in strings.keys() - self.strings.keys()]
        problems += [f"missing plurals {name}" for name in self.plurals.keys() - plurals.keys()]
        for name, source in self.strings.items():
            if name in strings:
                problems += self.compare(name, source, strings[name])
        for name, source in self.plurals.items():
            translated = plurals.get(name, {})
            missing = [quantity for quantity in quantities if quantity not in translated]
            if missing:
                problems.append(f"plurals {name} lacks {missing}")
            if "other" in translated:
                problems += self.compare(name, source["other"], translated["other"])
            for quantity, value in translated.items():
                problems += self.check_escaping(f"{name}#{quantity}", value)
        self.assertEqual([], problems, f"values-{locale}: " + "\n".join(problems))

    def compare(self, name, source, translated):
        problems = self.check_escaping(name, translated)
        if Counter(PLACEHOLDER.findall(source)) != Counter(PLACEHOLDER.findall(translated)):
            problems.append(f"{name}: placeholders differ")
        return problems

    @staticmethod
    def check_escaping(name, value):
        problems = []
        if re.search(r"(?<!\\)'", value):
            problems.append(f"{name}: unescaped apostrophe")
        if re.search(r'(?<!\\)"', value):
            problems.append(f"{name}: unescaped double quote")
        if value.startswith(("@", "?")):
            problems.append(f"{name}: starts with a resource reference character")
        return problems


if __name__ == "__main__":
    sys.exit(0 if unittest.main(exit=False).result.wasSuccessful() else 1)
