#!/usr/bin/env python3
"""Builds the light "next word" lists (word pairs) for Arabic and English.

Counts which word follows which in the first part of the Wikipedia dump of each language (formal language, so
dialects are mostly left out), keeps only words that are in the word list of tools/build_dict.py, drops rare pairs
and keeps the best few next words per word. Writes app/src/main/assets/ime/dict/{ar,en}_pairs.txt as
`previous<TAB>next<TAB>count` (the previous word in normalized form). Runs after build_dict.py in GitHub Actions.
"""
import bz2
import re
import sys
import urllib.request
from collections import Counter, defaultdict
from pathlib import Path

DICT = Path(__file__).resolve().parent.parent / "app/src/main/assets/ime/dict"
DUMPS = {
    "ar": ("https://dumps.wikimedia.org/arwiki/latest/arwiki-latest-pages-articles.xml.bz2", 60_000_000),
    "en": ("https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2", 40_000_000),
}
MAX_PAIRS = {"ar": 50000, "en": 30000}
MIN_COUNT = {"ar": 4, "en": 6}
PER_WORD = 5

TASHKEEL = re.compile("[ً-ٰٟـ]")
WORD = {"ar": re.compile(r"[ء-غف-يٱپچڤگ]+"), "en": re.compile(r"[A-Za-z']+")}
SPLIT = re.compile(r"[.!?؟،,;:؛()\[\]{}\"“”«»|=*#<>/\\\n\t–—-]+")


def normalize(text, lang):
    if lang != "ar":
        return text.lower()
    text = TASHKEEL.sub("", text)
    for a in "أإآٱ":
        text = text.replace(a, "ا")
    return text.replace("ى", "ي").replace("ة", "ه")


def load_words(lang):
    words = {}
    with open(DICT / f"{lang}.txt", encoding="utf-8") as f:
        for line in f:
            w = line.split("\t")[0]
            words.setdefault(normalize(w, lang), w)
    return words


def stream_text(url, limit):
    """Yields plain-ish text lines from the first [limit] compressed bytes of a bz2 dump."""
    req = urllib.request.Request(url, headers={"User-Agent": "ui-keyboard-build/1.0"})
    dec = bz2.BZ2Decompressor()
    got = 0
    tail = ""
    with urllib.request.urlopen(req, timeout=120) as r:
        while got < limit:
            chunk = r.read(1 << 20)
            if not chunk:
                break
            got += len(chunk)
            # The dump is "multistream": many bz2 streams one after another
            data = b""
            while chunk:
                try:
                    data += dec.decompress(chunk)
                except EOFError:
                    break
                if dec.eof:
                    chunk = dec.unused_data
                    dec = bz2.BZ2Decompressor()
                else:
                    chunk = b""
            text = tail + data.decode("utf-8", "ignore")
            lines = text.split("\n")
            tail = lines.pop()
            yield from lines


def clean_line(line):
    if line.lstrip().startswith(("{|", "|", "!", "<", "[[Category", "[[تصنيف", "==", "*", "#", "{{")):
        return ""
    line = re.sub(r"<ref[^>]*>.*?</ref>|<[^>]+>|\{\{[^}]*\}\}|https?://\S+", " ", line)
    line = re.sub(r"\[\[(?:[^\]|]*\|)?([^\]]*)\]\]", r"\1", line)
    return line.replace("'''", "").replace("''", "")


def build(lang):
    url, limit = DUMPS[lang]
    known = load_words(lang)
    pairs = Counter()
    for raw in stream_text(url, limit):
        line = clean_line(raw)
        if not line:
            continue
        for sentence in SPLIT.split(line):
            toks = WORD[lang].findall(sentence)
            prev = None
            for t in toks:
                key = normalize(t, lang)
                if key not in known:
                    prev = None  # unknown word breaks the chain
                    continue
                if prev is not None:
                    pairs[(prev, known[key])] += 1
                prev = key
    by_prev = defaultdict(list)
    for (p, n), c in pairs.items():
        if c >= MIN_COUNT[lang]:
            by_prev[p].append((c, n))
    kept = []
    for p, lst in by_prev.items():
        lst.sort(reverse=True)
        kept.extend((c, p, n) for c, n in lst[:PER_WORD])
    kept.sort(reverse=True)
    return kept[: MAX_PAIRS[lang]]


def main():
    ok = 0
    for lang in DUMPS:
        try:
            print(f"Building pairs for {lang} ...")
            kept = build(lang)
            if len(kept) < 2000:
                print(f"  only {len(kept)} pairs, skipping {lang}")
                continue
            with open(DICT / f"{lang}_pairs.txt", "w", encoding="utf-8") as f:
                for c, p, n in kept:
                    f.write(f"{p}\t{n}\t{c}\n")
            print(f"  wrote {len(kept)} pairs")
            ok += 1
        except Exception as e:
            print(f"  failed for {lang}: {e}")
    if ok == 0:
        sys.exit("no pair lists were built")


if __name__ == "__main__":
    main()
