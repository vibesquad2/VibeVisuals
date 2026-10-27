import bcrypt
h = b"$2a$10$t2PEgq2vg4byrg7L/cJTx.nkPo7Ry3ICHAumLbodu6FG/AeZiIdIq"
cands = [b"", b"\x00", b" ", b"\n", b"\r\n", b"\x00\x00", b"password\x00", b"qwerty\x00", b"123456\x00"]
for c in cands:
    try:
        if bcrypt.checkpw(c, h):
            print("FOUND", repr(c))
    except Exception as e:
        print("err", repr(c), e)
print("done")
