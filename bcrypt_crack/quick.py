import bcrypt, sys
h = b"$2a$10$t2PEgq2vg4byrg7L/cJTx.nkPo7Ry3ICHAumLbodu6FG/AeZiIdIq"
for path in sys.argv[1:]:
    with open(path, encoding="utf-8", errors="ignore") as f:
        for line in f:
            c = line.rstrip("\r\n")
            if not c:
                continue
            try:
                if bcrypt.checkpw(c.encode(), h):
                    print("FOUND", repr(c))
                    sys.exit(0)
            except Exception:
                pass
print("done")
