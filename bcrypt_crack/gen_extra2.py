# -*- coding: utf-8 -*-
layout = {
'й':'q','ц':'w','у':'e','к':'r','е':'t','н':'y','г':'u','ш':'i','щ':'o','з':'p','х':'[','ъ':']',
'ф':'a','ы':'s','в':'d','а':'f','п':'g','р':'h','о':'j','л':'k','д':'l','ж':';','э':"'",
'я':'z','ч':'x','с':'c','м':'v','и':'b','т':'n','ь':'m','б':',','ю':'.',
}
translit = {
'а':'a','б':'b','в':'v','г':'g','д':'d','е':'e','ё':'e','ж':'zh','з':'z','и':'i','й':'y',
'к':'k','л':'l','м':'m','н':'n','о':'o','п':'p','р':'r','с':'s','т':'t','у':'u','ф':'f',
'х':'kh','ц':'ts','ч':'ch','ш':'sh','щ':'shch','ъ':'','ы':'y','ь':'','э':'e','ю':'yu','я':'ya',
}

words = """
студент студентка университет универ группа сессия экзамен зачёт зачет диплом курс лекция семинар
преподаватель препод информатика программирование программист разработчик разработка проект
лабораторная работа задача решение система база данных сервер клиент пароль логин доступ
компьютер ноутбук клавиатура мышка экран монитор программа приложение сайт страница
java javascript python spring boot security bcrypt hash password admin root user test
backend frontend fullstack developer student university college school
qwerty qazwsx password passwd secret
""".split()

suffixes = ["", "1", "12", "123", "1234", "12345", "123456", "!", "@", "01", "007", "777", "228", "2024", "2025", "2026"]

out = set()
def add(s):
    if s:
        out.add(s)

for w in words:
    forms = {w, w.capitalize(), w.upper()}
    lay = "".join(layout.get(ch, ch) for ch in w.lower())
    forms.add(lay); forms.add(lay.capitalize())
    tr = "".join(translit.get(ch, ch) for ch in w.lower())
    forms.add(tr); forms.add(tr.capitalize())
    for f in forms:
        add(f)
        for s in suffixes:
            add(f + s)
        for s in [" ", "  "]:
            add(f + s)
            add(s + f)

# explicit whitespace variants of very common passwords
for w in ["password","qwerty","123456","admin","secret","test","pass","root","qwerty123","password123"]:
    for s in [" ", "  "]:
        add(w + s); add(s + w)

with open("extra2.txt", "w", encoding="utf-8", newline="") as f:
    for w in sorted(out):
        f.write(w + "\n")
print(len(out))
