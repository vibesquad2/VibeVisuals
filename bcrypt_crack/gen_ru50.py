# -*- coding: utf-8 -*-
layout = {
'й':'q','ц':'w','у':'e','к':'r','е':'t','н':'y','г':'u','ш':'i','щ':'o','з':'p','х':'[','ъ':']',
'ф':'a','ы':'s','в':'d','а':'f','п':'g','р':'h','о':'j','л':'k','д':'l','ж':';','э':"'",
'я':'z','ч':'x','с':'c','м':'v','и':'b','т':'n','ь':'m','б':',','ю':'.',
}

out = set()
def add(s):
    if s:
        out.add(s)

# top Russian frequency words
words = []
with open("ru_50k.txt", encoding="utf-8", errors="ignore") as f:
    for line in f:
        parts = line.strip().split()
        if not parts:
            continue
        w = parts[0].lower()
        if all('а' <= ch <= 'я' or ch == 'ё' for ch in w) and len(w) >= 3:
            words.append(w)
        if len(words) >= 20000:
            break

for w in words:
    add(w)
    add(w.capitalize())
    lay = "".join(layout.get(ch, ch) for ch in w)
    add(lay)
    add(lay.capitalize())

# russian slang / profanity / common leaked patterns
slang = """
блядь блят бля сука сучка пиzда пизда хуй хер нахуй заебал заебали пиздец говно гандон мудак лох чмо
красавчик молодец норм нормально збс заебись охуенно пиздато класс супер отлично
люблю любовь счастье удача деньги работа учёба школа универ сессия экзамен
привет пока спасибо пожалуйста извини прости
кот котик котёнок кошка собака пёс щенок зайка зайчик мишка медведь волк лиса
солнце луна звезда небо море река лес гора ветер дождь снег
мама папа мамочка папочка сынок дочка брат сестра
""".split()
suffixes = ["", "1", "12", "123", "1234", "12345", "123456", "!", "@", "01", "007", "777", "228", "2023", "2024", "2025", "2026"]
for w in slang:
    forms = {w, w.capitalize()}
    lay = "".join(layout.get(ch, ch) for ch in w)
    forms.add(lay); forms.add(lay.capitalize())
    for form in forms:
        add(form)
        for s in suffixes:
            add(form + s)

with open("ru50_candidates.txt", "w", encoding="utf-8") as f:
    for w in sorted(out):
        f.write(w + "\n")
print(len(out))
