package br.csmp.core.vazio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Direction;

/**
 * O "plano" do Vazio: onde ficam as ruas, as construcoes, os pontos estranhos e as estatuas. Tudo e calculado so a
 * partir da semente e da posicao, entao qualquer chunk chega ao mesmo plano sem guardar nada e sem uma construcao
 * nascer dentro da outra.
 * <ul>
 *   <li><b>Ruas:</b> uma rede de nos espalhados (a cada {@link #G} blocos, deslocados ao acaso); parte dos pares
 *       vizinhos ganha uma rua em curva, e muitas ruas sao so um pedaco (comecam e terminam no meio do nada).</li>
 *   <li><b>Lotes:</b> casas, lojas, capelas, pracas e ruinas ao lado das ruas, viradas para elas; pontos estranhos
 *       (monolito, balanco, escada, portas, fonte) longe das ruas; estatuas raras.</li>
 *   <li><b>Sem sobreposicao:</b> os lotes sao aceitos por prioridade (estatua, depois ruas, depois pontos) e um lote
 *       so entra se nao encosta em nenhum ja aceito nem numa rua alheia.</li>
 * </ul>
 */
final class VazioMapa {
    /** Distancia media entre os nos da rede de ruas. */
    static final int G = 80;
    /** Primeiro ar acima do chao do Vazio (o chao fica em y = 3). */
    static final int Y = 4;
    /** Celula das estatuas: uma a cada 26x26 chunks, com 45% de chance (raras de achar). */
    static final int CELULA_ESTATUA = 416;
    /** Celula dos pontos estranhos fora das ruas. */
    static final int CELULA_PONTO = 64;
    /** Lado do bloco de cache (cada um guarda as ruas e os lotes da sua area). */
    private static final int TILE = 128;
    private static final int MARGEM = 150;

    static final int CASA = 0;
    static final int LOJA = 1;
    static final int CAPELA = 2;
    static final int PRACA = 3;
    static final int RUINA = 4;
    static final int MONOLITO = 10;
    static final int BALANCO = 11;
    static final int ESCADA = 12;
    static final int PORTAS = 13;
    static final int FONTE = 14;
    static final int ESTATUA = 20;

    private VazioMapa() {}

    // ------------------------------------------------------------------ sorteio

    static long h(long s, long a, long b, long c) {
        long x = s ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL) ^ (c * 0x165667B19E3779F9L);
        x ^= x >>> 33;
        x *= 0xFF51AFD7ED558CCDL;
        x ^= x >>> 33;
        x *= 0xC4CEB9FE1A85EC53L;
        x ^= x >>> 33;
        return x;
    }

    static double u(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }

    // ------------------------------------------------------------------ ruas

    /** Uma rua: curva de Bezier quadratica (a -> b, puxada por c) e o trecho [t0, t1] que existe de fato. */
    record Rua(double ax, double az, double cx, double cz, double bx, double bz, double t0, double t1, long semente) {
        double[] ponto(double t) {
            double v = 1.0D - t;
            return new double[] {v * v * ax + 2 * v * t * cx + t * t * bx, v * v * az + 2 * v * t * cz + t * t * bz};
        }

        /** Normal unitaria (perpendicular a direcao da rua) no ponto t. */
        double[] normal(double t) {
            double dx = 2 * (1 - t) * (cx - ax) + 2 * t * (bx - cx);
            double dz = 2 * (1 - t) * (cz - az) + 2 * t * (bz - cz);
            double l = Math.max(1.0E-6D, Math.hypot(dx, dz));
            return new double[] {-dz / l, dx / l};
        }

        double comprimento() {
            double total = 0.0D;
            double[] antes = ponto(t0);
            for (int i = 1; i <= 24; i++) {
                double[] p = ponto(t0 + (t1 - t0) * i / 24.0D);
                total += Math.hypot(p[0] - antes[0], p[1] - antes[1]);
                antes = p;
            }
            return total;
        }

        /** Distancia aproximada de (x, z) ate o trecho existente da rua. */
        double distancia(double x, double z) {
            double menor = Double.MAX_VALUE;
            for (int i = 0; i <= 48; i++) {
                double[] p = ponto(t0 + (t1 - t0) * i / 48.0D);
                menor = Math.min(menor, Math.hypot(p[0] - x, p[1] - z));
            }
            return menor;
        }
    }

    private static double[] no(long s, int i, int j) {
        long k = h(s, i, j, 1);
        return new double[] {i * (double) G + (u(k) - 0.5D) * G * 0.55D, j * (double) G + (u(k * 31 + 7) - 0.5D) * G * 0.55D};
    }

    private static List<Rua> calcularRuas(long s, int minX, int minZ, int maxX, int maxZ) {
        List<Rua> ruas = new ArrayList<>();
        for (int i = Math.floorDiv(minX, G) - 1; i <= Math.floorDiv(maxX, G) + 1; i++) {
            for (int j = Math.floorDiv(minZ, G) - 1; j <= Math.floorDiv(maxZ, G) + 1; j++) {
                for (int dir = 0; dir < 2; dir++) {
                    long k = h(s, i, j, 10 + dir);
                    if (u(k) >= 0.42D) {
                        continue;
                    }
                    double[] a = no(s, i, j);
                    double[] b = dir == 0 ? no(s, i + 1, j) : no(s, i, j + 1);
                    double mx = (a[0] + b[0]) / 2.0D;
                    double mz = (a[1] + b[1]) / 2.0D;
                    double nx = -(b[1] - a[1]);
                    double nz = b[0] - a[0];
                    double curva = (u(k * 13 + 3) - 0.5D) * 0.9D;
                    double cx = mx + nx * curva;
                    double cz = mz + nz * curva;
                    double t0 = 0.0D;
                    double t1 = 1.0D;
                    if (u(k * 17 + 5) >= 0.45D) {
                        // so um pedaco de rua, que comeca e termina no meio do nada
                        t0 = u(k * 19 + 1) * 0.45D;
                        t1 = Math.min(1.0D, t0 + 0.3D + u(k * 23 + 9) * 0.45D);
                    }
                    double x0 = Math.min(Math.min(a[0], b[0]), cx) - 12;
                    double x1 = Math.max(Math.max(a[0], b[0]), cx) + 12;
                    double z0 = Math.min(Math.min(a[1], b[1]), cz) - 12;
                    double z1 = Math.max(Math.max(a[1], b[1]), cz) + 12;
                    if (x1 < minX || x0 > maxX || z1 < minZ || z0 > maxZ) {
                        continue;
                    }
                    ruas.add(new Rua(a[0], a[1], cx, cz, b[0], b[1], t0, t1, k));
                }
            }
        }
        return ruas;
    }

    // ------------------------------------------------------------------ lotes

    /** Um lote: centro, tipo, para onde a frente olha (para a rua), raio ocupado e semente propria. */
    record Lote(int x, int z, int tipo, Direction frente, int raio, long semente, int prioridade) {}

    static int raioDe(int tipo) {
        return switch (tipo) {
            case CASA -> 7;
            case LOJA -> 8;
            case CAPELA -> 9;
            case PRACA -> 8;
            case RUINA -> 6;
            case ESTATUA -> 16;
            default -> 6;
        };
    }

    /** Direcao cardinal mais proxima do vetor (dx, dz). */
    private static Direction cardinal(double dx, double dz) {
        return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    private static List<Lote> calcularLotes(long s, int minX, int minZ, int maxX, int maxZ) {
        int ex0 = minX - MARGEM;
        int ez0 = minZ - MARGEM;
        int ex1 = maxX + MARGEM;
        int ez1 = maxZ + MARGEM;
        List<Rua> ruas = calcularRuas(s, ex0 - 40, ez0 - 40, ex1 + 40, ez1 + 40);
        List<Lote> candidatos = new ArrayList<>();
        // estatuas (raras)
        for (int ci = Math.floorDiv(ex0, CELULA_ESTATUA); ci <= Math.floorDiv(ex1, CELULA_ESTATUA); ci++) {
            for (int cj = Math.floorDiv(ez0, CELULA_ESTATUA); cj <= Math.floorDiv(ez1, CELULA_ESTATUA); cj++) {
                long k = h(s, ci, cj, 50);
                if (u(k) >= 0.45D) {
                    continue;
                }
                int x = ci * CELULA_ESTATUA + 60 + (int) (u(k * 3 + 1) * (CELULA_ESTATUA - 120));
                int z = cj * CELULA_ESTATUA + 60 + (int) (u(k * 5 + 2) * (CELULA_ESTATUA - 120));
                candidatos.add(new Lote(x, z, ESTATUA, Direction.SOUTH, raioDe(ESTATUA), k, 0));
            }
        }
        // construcoes ao lado das ruas, viradas para elas
        for (Rua rua : ruas) {
            double[] vagas = {0.18D, 0.38D, 0.62D, 0.82D};
            for (int v = 0; v < vagas.length; v++) {
                double t = vagas[v];
                if (t < rua.t0() + 0.06D || t > rua.t1() - 0.06D) {
                    continue;
                }
                long k = h(rua.semente(), v, 7, 3);
                if (u(k) >= 0.55D) {
                    continue;
                }
                double sorteio = u(k * 7 + 11);
                int tipo = sorteio < 0.40D ? CASA : sorteio < 0.56D ? LOJA : sorteio < 0.68D ? CAPELA : sorteio < 0.84D ? RUINA : PRACA;
                int raio = raioDe(tipo);
                double lado = u(k * 5 + 13) < 0.5D ? 1.0D : -1.0D;
                double[] p = rua.ponto(t);
                double[] n = rua.normal(t);
                double afastamento = raio + 4.0D;
                int x = (int) Math.round(p[0] + n[0] * lado * afastamento);
                int z = (int) Math.round(p[1] + n[1] * lado * afastamento);
                Direction frente = cardinal(-n[0] * lado, -n[1] * lado);
                candidatos.add(new Lote(x, z, tipo, frente, raio, k, 1));
            }
        }
        // pontos estranhos, longe das ruas
        for (int pi = Math.floorDiv(ex0, CELULA_PONTO); pi <= Math.floorDiv(ex1, CELULA_PONTO); pi++) {
            for (int pj = Math.floorDiv(ez0, CELULA_PONTO); pj <= Math.floorDiv(ez1, CELULA_PONTO); pj++) {
                long k = h(s, pi, pj, 60);
                if (u(k) >= 0.30D) {
                    continue;
                }
                int tipo = MONOLITO + (int) (u(k * 3 + 5) * 5);
                int x = pi * CELULA_PONTO + 12 + (int) (u(k * 7 + 1) * (CELULA_PONTO - 24));
                int z = pj * CELULA_PONTO + 12 + (int) (u(k * 11 + 3) * (CELULA_PONTO - 24));
                Direction frente = Direction.from2DDataValue((int) (u(k * 13 + 9) * 4));
                candidatos.add(new Lote(x, z, tipo, frente, raioDe(tipo), k, 2));
            }
        }
        candidatos.sort(Comparator.comparingInt(Lote::prioridade).thenComparingLong(Lote::semente));
        List<Lote> aceitos = new ArrayList<>();
        for (Lote c : candidatos) {
            boolean livre = true;
            for (Lote a : aceitos) {
                if (Math.hypot(a.x() - c.x(), a.z() - c.z()) < a.raio() + c.raio() + 3) {
                    livre = false;
                    break;
                }
            }
            if (livre && c.tipo() != ESTATUA) {
                // a rua da propria construcao fica a raio + 4; qualquer rua mais perto que isso a atravessaria
                double folga = c.prioridade() == 2 ? c.raio() + 5.0D : c.raio() + 2.5D;
                for (Rua rua : ruas) {
                    if (rua.distancia(c.x(), c.z()) < folga) {
                        livre = false;
                        break;
                    }
                }
            }
            if (livre) {
                aceitos.add(c);
            }
        }
        List<Lote> dentro = new ArrayList<>();
        for (Lote a : aceitos) {
            if (a.x() >= minX && a.x() < maxX && a.z() >= minZ && a.z() < maxZ) {
                dentro.add(a);
            }
        }
        return dentro;
    }

    // ------------------------------------------------------------------ cache e consultas

    private record Tile(List<Rua> ruas, List<Lote> lotes) {}

    private static final Map<Long, Tile> CACHE = new ConcurrentHashMap<>();
    private static volatile long sementeDoCache = Long.MIN_VALUE;

    private static Tile tile(long s, int tx, int tz) {
        if (sementeDoCache != s) {
            CACHE.clear();
            sementeDoCache = s;
        }
        if (CACHE.size() > 4096) {
            CACHE.clear();
        }
        long chave = ((long) tx << 32) ^ (tz & 0xFFFFFFFFL);
        return CACHE.computeIfAbsent(chave, c -> {
            int x0 = tx * TILE;
            int z0 = tz * TILE;
            return new Tile(calcularRuas(s, x0, z0, x0 + TILE, z0 + TILE), calcularLotes(s, x0, z0, x0 + TILE, z0 + TILE));
        });
    }

    /** Ruas que passam perto da area. */
    static List<Rua> ruas(long s, int minX, int minZ, int maxX, int maxZ) {
        List<Rua> todas = new ArrayList<>();
        for (int tx = Math.floorDiv(minX, TILE); tx <= Math.floorDiv(maxX, TILE); tx++) {
            for (int tz = Math.floorDiv(minZ, TILE); tz <= Math.floorDiv(maxZ, TILE); tz++) {
                for (Rua r : tile(s, tx, tz).ruas()) {
                    if (!todas.contains(r)) {
                        todas.add(r);
                    }
                }
            }
        }
        return todas;
    }

    /** Lotes com centro dentro da area. */
    static List<Lote> lotes(long s, int minX, int minZ, int maxX, int maxZ) {
        List<Lote> todos = new ArrayList<>();
        for (int tx = Math.floorDiv(minX, TILE); tx <= Math.floorDiv(maxX, TILE); tx++) {
            for (int tz = Math.floorDiv(minZ, TILE); tz <= Math.floorDiv(maxZ, TILE); tz++) {
                for (Lote l : tile(s, tx, tz).lotes()) {
                    if (l.x() >= minX && l.x() < maxX && l.z() >= minZ && l.z() < maxZ) {
                        todos.add(l);
                    }
                }
            }
        }
        return todos;
    }
}
