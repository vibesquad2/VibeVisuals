import bcrypt
h = b"$2a$10$t2PEgq2vg4byrg7L/cJTx.nkPo7Ry3ICHAumLbodu6FG/AeZiIdIq"
base = ["password","qwerty","123456","12345678","admin","secret","test","qwerty123",
        "password123","admin123","root","pass","12345","123456789","qwertyuiop",
        "passwd","letmein","welcome","master","login","user","student"]
extra = ["\n","\r\n"," ","\t","  ","\n\n"]
cands = set()
for w in base:
    cands.add(w)
    for e in extra:
        cands.add(w + e)
        cands.add(e + w)
for c in cands:
    try:
        if bcrypt.checkpw(c.encode(), h):
            print("FOUND", repr(c)); break
    except Exception:
        pass
else:
    print("done", len(cands))
