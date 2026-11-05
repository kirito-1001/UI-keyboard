#!/usr/bin/env python3
"""Builds the Arabic and English word lists used for word suggestions.

Downloads the open "FrequencyWords" lists (https://github.com/hermitdave/FrequencyWords, CC BY-SA 4.0, derived
from OpenSubtitles) and writes cleaned "word<TAB>count" files to app/src/main/assets/ime/dict/{ar,en}.txt.
Runs in the GitHub Actions build, because the files are not committed to the repository.
"""
import re
import sys
import urllib.request
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "app/src/main/assets/ime/dict"
BASE = "https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content"
SOURCES = {
    "ar": [f"{BASE}/2018/ar/ar_50k.txt", f"{BASE}/2018/ar/ar_full.txt", f"{BASE}/2016/ar/ar_50k.txt"],
    "en": [f"{BASE}/2018/en/en_50k.txt", f"{BASE}/2018/en/en_full.txt", f"{BASE}/2016/en/en_50k.txt"],
}
LIMIT = 50000

AR_WORD = re.compile(r"^[ء-غف-يٱپچڤگ]{1,20}$")
EN_WORD = re.compile(r"^[a-z][a-z']{0,24}$")


def fetch(urls):
    last = None
    for url in urls:
        try:
            with urllib.request.urlopen(url, timeout=60) as r:
                return r.read().decode("utf-8")
        except Exception as e:  # try the next URL
            last = e
            print(f"  failed {url}: {e}")
    raise RuntimeError(f"all sources failed: {last}")


def clean(lang, text):
    pattern = AR_WORD if lang == "ar" else EN_WORD
    words = []
    for line in text.splitlines():
        parts = line.split()
        if len(parts) != 2 or not parts[1].isdigit():
            continue
        word, count = parts[0], int(parts[1])
        if lang == "en":
            word = word.lower()
            if len(word) == 1 and word not in ("a", "i"):
                continue
        if not pattern.match(word):
            continue
        words.append((word, count))
        if len(words) >= LIMIT:
            break
    return words


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for lang, urls in SOURCES.items():
        print(f"Downloading {lang} ...")
        words = clean(lang, fetch(urls))
        if len(words) < 5000:
            sys.exit(f"{lang}: only {len(words)} usable words, something is wrong")
        with open(OUT / f"{lang}.txt", "w", encoding="utf-8") as f:
            for word, count in words:
                f.write(f"{word}\t{count}\n")
        print(f"  wrote {len(words)} words to {lang}.txt")


if __name__ == "__main__":
    main()
