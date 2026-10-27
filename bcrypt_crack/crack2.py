import bcrypt, sys, os, time, multiprocessing as mp

TARGET = b"$2a$10$t2PEgq2vg4byrg7L/cJTx.nkPo7Ry3ICHAumLbodu6FG/AeZiIdIq"
CHUNK = 400

def init():
    global _target
    _target = TARGET

def check_chunk(cands):
    for c in cands:
        try:
            if bcrypt.checkpw(c.encode("utf-8", "ignore"), _target):
                return c
        except Exception:
            pass
    return None

def main():
    words = set()
    for path in sys.argv[1:]:
        if os.path.isfile(path):
            with open(path, "r", encoding="utf-8", errors="ignore") as f:
                for line in f:
                    w = line.rstrip("\r\n")
                    if w:
                        words.add(w)
    cands = sorted(words)
    print(f"total candidates: {len(cands)}", flush=True)

    chunks = [cands[i:i+CHUNK] for i in range(0, len(cands), CHUNK)]
    t0 = time.time()
    done = 0
    with mp.Pool(processes=min(8, os.cpu_count() or 4), initializer=init) as pool:
        for res in pool.imap_unordered(check_chunk, chunks):
            done += CHUNK
            if res is not None:
                print(f"\nFOUND: {res!r} after ~{done} candidates, {time.time()-t0:.1f}s", flush=True)
                pool.terminate()
                return
            if done % 4000 < CHUNK:
                el = time.time() - t0
                print(f"{done}/{len(cands)}  {done/el:.1f}/s", flush=True)
    print(f"not found ({time.time()-t0:.1f}s)", flush=True)

if __name__ == "__main__":
    main()
