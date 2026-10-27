import itertools, sys

leet = {
    "a": ["a", "4", "@"],
    "e": ["e", "3"],
    "i": ["i", "1", "!"],
    "o": ["o", "0"],
    "s": ["s", "5", "$"],
    "t": ["t", "7"],
}

def leet_variants(word, limit=8):
    positions = [i for i, ch in enumerate(word.lower()) if ch in leet]
    if not positions:
        return [word]
    out = set()
    for choices in itertools.product(*[leet[word[i].lower()] for i in positions]):
        chars = list(word)
        for pos, ch in zip(positions, choices):
            chars[pos] = ch
        out.add("".join(chars))
        if len(out) >= limit:
            break
    return list(out)

def build():
    seen = set()
    def add(w):
        w = w.strip()
        if w and w not in seen:
            seen.add(w)
            yield w

    # raw wordlists
    for path in ["top100000.txt", "extras.txt"]:
        with open(path, encoding="utf-8", errors="ignore") as f:
            for line in f:
                yield from add(line)

    # top bases with mutations
    with open("top10000.txt", encoding="utf-8", errors="ignore") as f:
        bases = [l.strip() for l in f if l.strip()][:3000]

    suffixes = ["", "1", "12", "123", "1234", "12345", "123456", "!", "!!", "@", "#",
                "01", "07", "007", "69", "777", "228", "2024", "2025", "2026",
                "1990", "1991", "1992", "1993", "1994", "1995", "1996", "1997",
                "1998", "1999", "2000", "2001", "2002", "2003", "2004", "2005", "2006"]

    for b in bases:
        forms = {b, b.lower(), b.upper(), b.capitalize()}
        forms.update(leet_variants(b, 6))
        for form in forms:
            for s in suffixes:
                yield from add(form + s)

if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "mutations.txt"
    n = 0
    with open(out, "w", encoding="utf-8") as f:
        for w in build():
            f.write(w + "\n")
            n += 1
    print(f"wrote {n} candidates to {out}")
