import bcrypt, sys, itertools, os, time

TARGET = b"$2a$10$t2PEgq2vg4byrg7L/cJTx.nkPo7Ry3ICHAumLbodu6FG/AeZiIdIq"

base = """
password Password PASSWORD password1 password12 password123 password1234 password12345
pass pass1 pass123 passw0rd Passw0rd P@ssw0rd P@ssword Pa$$word Password1 Password123 Password1234
admin Admin admin1 admin12 admin123 admin1234 administrator root toor root123
qwerty Qwerty qwerty1 qwerty12 qwerty123 qwerty1234 qwerty12345 qwertyuiop
123456 1234567 12345678 123456789 1234567890 12345 1234 123 12 1 0
111111 1111111 11111111 222222 333333 444444 555555 666666 777777 888888 999999 000000
123123 123321 121212 112233 654321 987654321 9876543210
qwe qwe123 qweasd qweasdzxc qweqwe qwertyqwerty
asd asd123 asdf asdf123 asdfgh asdfghjkl asdfasdf
zxc zxc123 zxcvbnm zxcvbn zxczxc
1q2w3e 1q2w3e4r 1q2w3e4r5t 1qaz2wsx 1qazxsw2 1q2w3e4r5t6y 1qaz2wsx3edc
qazwsx qazwsxedc qazwsx123 zaq12wsx zaq1zaq1 zaqwsxcde q1w2e3r4 q1w2e3r4t5
letmein letmein123 welcome welcome1 welcome123 monkey monkey123 dragon dragon123
master master123 secret secret123 iloveyou sunshine princess football baseball
abc123 abcdef abcdefg abcd1234 a1b2c3 a1b2c3d4 aaa111
test test1 test12 test123 testing tester
user user1 user123 guest guest1 guest123 login login123 pass1234
michael jennifer jordan hunter harley ranger buster thomas robert soccer hockey killer
george summer winter spring autumn
superman batman starwars pokemon minecraft
internet computer samsung google yandex mail.ru vkontakte
love love123 loveyou lovely
money cash dollar
freedom hello hello123 helloworld world peace
shadow ninja mustang access matrix
default changeme change_me
temp temporary
qwerty! Qwerty123 Password! Password1! P@ssw0rd! admin@123 Admin@123
iloveyou1 iloveyou123
letmein! qwerty@123
""".split()

# russian keyboard layout classics (typed with english keys)
ru = """
gfhjkm gfhjkm123 gfhjkm1 gfhjkbnm
ghbdtn ghbdtn123 ghbdtn1
rjvgm.nth rjvgm.nth123 rjvgfnth
vjcrdf vjcrdf123 vbh vbh123
ybynth ybynth123
ltvj ltvj123
crhjvyj ckfdrf
tcnm tcnm123
gjhjkm gjhjkm123
zxs zxs123
qwerty zxcvbnm
""".split()

words = base + ru
suffixes = ["", "1", "12", "123", "1234", "12345", "123456", "!", "!!", "@", "#", "2020", "2021", "2022", "2023", "2024", "2025", "2026", "01", "007", "69", "777", "228"]

cands = []
seen = set()
def add(x):
    if x not in seen:
        seen.add(x)
        cands.append(x)

for w in words:
    add(w)
    for s in suffixes:
        add(w + s)
    add(w.capitalize())
    for s in ["1", "123", "!"]:
        add(w.capitalize() + s)

# wordlist file support
if len(sys.argv) > 1 and os.path.isfile(sys.argv[1]):
    with open(sys.argv[1], "r", encoding="utf-8", errors="ignore") as f:
        for line in f:
            add(line.rstrip("\r\n"))

print(f"candidates: {len(cands)}", flush=True)
t0 = time.time()
for i, cand in enumerate(cands):
    if bcrypt.checkpw(cand.encode("utf-8"), TARGET):
        print(f"\nFOUND: {cand!r}  (took {time.time()-t0:.1f}s)")
        sys.exit(0)
    if i % 200 == 0 and i:
        print(f"{i} tried, {(i)/(time.time()-t0):.1f}/s", flush=True)
print(f"not found in {len(cands)} candidates ({time.time()-t0:.1f}s)")
