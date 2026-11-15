"""Synthesizes the app's UI sound cues (Pip-Boy style blips) into app/src/main/res/raw.

Usage: python tools/gen_sounds.py app/src/main/res/raw
All sounds are generated here from scratch (no third-party audio).
"""
import math, os, random, struct, sys, wave

RATE = 22050


def tone(freq, dur, vol=0.5, shape='square', f_end=None, attack=0.004, release=0.03):
    n = int(RATE * dur)
    out = []
    phase = 0.0
    for i in range(n):
        t = i / n
        f = freq if f_end is None else freq + (f_end - freq) * t
        phase += 2 * math.pi * f / RATE
        if shape == 'square':
            # soft square: sum of a few odd harmonics (less harsh than a true square)
            s = sum(math.sin(phase * k) / k for k in (1, 3, 5)) * 0.8
        elif shape == 'tri':
            s = 2 / math.pi * math.asin(math.sin(phase))
        else:
            s = math.sin(phase)
        env = min(1.0, (i / RATE) / attack) * min(1.0, ((n - i) / RATE) / release)
        out.append(s * env * vol)
    return out


def silence(dur):
    return [0.0] * int(RATE * dur)


def noise(dur, vol=0.2):
    n = int(RATE * dur)
    rnd = random.Random(76)
    return [(rnd.random() * 2 - 1) * vol * (1 - i / n) ** 3 for i in range(n)]


def mix(*tracks):
    n = max(len(t) for t in tracks)
    return [sum(t[i] for t in tracks if i < len(t)) for i in range(n)]


def save(path, samples):
    peak = max(1e-6, max(abs(s) for s in samples))
    scale = 0.85 / peak if peak > 0.85 else 1.0
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b''.join(struct.pack('<h', int(max(-1, min(1, s * scale)) * 32767)) for s in samples))


SOUNDS = {
    # checking an item: bright two-step blip up
    'sfx_check': tone(880, 0.035, 0.45) + tone(1320, 0.05, 0.45),
    # unchecking: two-step blip down
    'sfx_uncheck': tone(990, 0.035, 0.4) + tone(620, 0.05, 0.4),
    # small UI click (filters, chips, menu)
    'sfx_click': mix(tone(1600, 0.018, 0.35, 'tri', release=0.012), noise(0.012, 0.15)),
    # opening a category / navigating forward: rising chirp
    'sfx_nav': tone(500, 0.09, 0.4, 'tri', f_end=1100),
    # going home / back: falling chirp
    'sfx_back': tone(1000, 0.09, 0.4, 'tri', f_end=480),
    # drawer open / close: soft whoosh + tick
    'sfx_drawer': mix(noise(0.08, 0.25), tone(700, 0.03, 0.25, 'sine')),
    # language switch: double tick
    'sfx_lang': tone(1200, 0.025, 0.4, 'tri') + silence(0.03) + tone(1500, 0.03, 0.4, 'tri'),
    # completing a whole set / section: little arpeggio fanfare
    'sfx_complete': tone(660, 0.07, 0.4) + tone(880, 0.07, 0.4) + tone(1100, 0.07, 0.4) + tone(1320, 0.16, 0.45, release=0.1),
    # update dialog / error-ish notice
    'sfx_alert': tone(740, 0.08, 0.4, 'tri') + silence(0.04) + tone(740, 0.08, 0.4, 'tri'),
}

if __name__ == '__main__':
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    for name, s in SOUNDS.items():
        save(os.path.join(out, name + '.wav'), s)
        print(name, f'{len(s) / RATE * 1000:.0f} ms')
