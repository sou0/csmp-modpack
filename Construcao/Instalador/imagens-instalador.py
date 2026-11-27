# Gera as imagens do instalador (Inno Setup): faixa lateral com ceu e nuvens em pixel e a logo, e a logo pequena.
# Uso: python imagens-instalador.py <pasta de saida>
import sys
from PIL import Image, ImageDraw

LOGO = [
    "..BBBBBBBBBBBB..",
    ".BbbbbbbbbbbbbB.",
    "BbbbkkkkkkkkbbbB",
    "BbbkCCCCCCCckbbB",
    "BbkCccccccccckbB",
    "BbkCccssssssskbB",
    "BlkCcskkkkkkkllB",
    "BlkCcskllllllllB",
    "BlkCcskllllllllB",
    "BlkCcskkkkkkkllB",
    "BwkCccCCCCCCckwB",
    "BwkcccccccccskwB",
    "BwwkcssssssskwwB",
    "BwwwkkkkkkkkwwwB",
    ".BwwwwwwwwwwwwB.",
    "..BBBBBBBBBBBB..",
]
CORES = {'B': (70, 112, 214), 'b': (121, 162, 253), 'l': (142, 204, 255), 'w': (236, 246, 255),
         'k': (22, 24, 30), 'c': (174, 236, 255), 'C': (214, 255, 255), 's': (120, 186, 230)}


def logo(img, x, y, px):
    d = ImageDraw.Draw(img)
    for l, linha in enumerate(LOGO):
        for c, ch in enumerate(linha):
            if ch != '.':
                d.rectangle([x + c * px, y + l * px, x + (c + 1) * px - 1, y + (l + 1) * px - 1], fill=CORES[ch])


def faixa(w, h, escala):
    img = Image.new('RGB', (w, h))
    d = ImageDraw.Draw(img)
    topo, base = (108, 150, 246), (178, 222, 255)
    for y in range(h):
        t = y / (h - 1)
        d.line([(0, y), (w, y)], fill=tuple(int(topo[i] + (base[i] - topo[i]) * t) for i in range(3)))
    u = 6 * escala
    soltas = [(2, 6, 9), (16, 10, 7), (6, 17, 10), (18, 21, 6), (3, 27, 8)]
    for x, y, lw in soltas:
        d.rectangle([x * u, y * u, (x + lw) * u - 1, y * u + u - 1], fill=(205, 228, 255))
        d.rectangle([(x + 2) * u, (y - 1) * u, (x + lw - 2) * u - 1, y * u - 1], fill=(205, 228, 255))
    # nuvens de baixo, cheias
    linhas = h // u
    perfil = [3, 4, 4, 5, 5, 4, 3, 3, 4, 5, 6, 6, 5, 4, 4, 5, 6, 5, 4, 3, 3, 4, 5, 4, 3, 4, 5, 6]
    for i in range(w // u + 1):
        alt = perfil[i % len(perfil)] + 3
        d.rectangle([i * u, (linhas - alt) * u + u // 2, (i + 1) * u - 1, h], fill=(214, 232, 250))
        d.rectangle([i * u, (linhas - alt) * u + u, (i + 1) * u - 1, h], fill=(255, 255, 255))
    px = 6 * escala
    logo(img, (w - 16 * px) // 2, int(h * 0.30), px)
    return img


saida = sys.argv[1]
faixa(164, 314, 1).save(saida + '/faixa-100.bmp')
faixa(328, 628, 2).save(saida + '/faixa-200.bmp')
for tam, px in ((55, 3), (110, 6)):
    p = Image.new('RGB', (tam, tam), (255, 255, 255))
    logo(p, (tam - 16 * px) // 2, (tam - 16 * px) // 2, px)
    p.save(saida + '/logo-%d.bmp' % tam)
print('ok')
