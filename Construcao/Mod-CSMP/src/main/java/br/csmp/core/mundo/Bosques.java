package br.csmp.core.mundo;

import br.csmp.core.CsmpMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * Onde ficam as Florestas das Betulas Indoceis. O mundo e dividido em celulas de {@link #CELULA} blocos e cada
 * celula tem uma floresta, com o centro sorteado pela semente do mundo (por isso e sempre o mesmo lugar, sem
 * precisar guardar nada). O centro cai no meio de um chunk para o cemiterio caber inteiro.
 */
public final class Bosques {
    /** Lado de cada celula, em blocos (uma floresta por celula). */
    public static final int CELULA = 3000;
    /** Raio medio de cada floresta, em blocos. */
    public static final int RAIO = 130;

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "betulas_indoceis");
    public static final ResourceKey<Biome> BIOMA = ResourceKey.create(Registries.BIOME, ID);

    private Bosques() {}

    private static long mistura(long semente, int x, int z) {
        long h = semente ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ 0x51ED270B2L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    /** Quantos lugares tentar em cada celula ate achar um que seja terra firme. */
    private static final int TENTATIVAS = 16;

    /** Centros ja calculados (celula -> centro, ou SEM_FLORESTA). Calcular usa o ruido de biomas, entao guarda. */
    private static final java.util.Map<Long, int[]> CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int[] SEM_FLORESTA = new int[0];
    private static volatile long sementeDoCache = Long.MIN_VALUE;

    /**
     * Centro (x, z) da floresta da celula (cx, cz), sempre no meio de um chunk e longe das bordas da celula.
     * Cada celula tenta varios lugares (sempre na mesma ordem, pela semente) e fica com o primeiro em terra firme:
     * o centro e a volta toda (raio da floresta) nao podem ser oceano, rio ou praia. Usa o ruido de biomas do
     * gerador, entao funciona antes de o terreno existir. Devolve null se a celula inteira for agua.
     */
    public static int[] centroDaCelula(net.minecraft.server.level.ServerLevel nivel, int cx, int cz) {
        long semente = nivel.getSeed();
        if (sementeDoCache != semente) {
            CACHE.clear();
            sementeDoCache = semente;
        }
        long chave = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
        int[] guardado = CACHE.get(chave);
        if (guardado == null) {
            guardado = procurarCentro(nivel, semente, cx, cz);
            CACHE.put(chave, guardado);
        }
        return guardado.length == 0 ? null : guardado;
    }

    private static int[] procurarCentro(net.minecraft.server.level.ServerLevel nivel, long semente, int cx, int cz) {
        var gerador = nivel.getChunkSource().getGenerator();
        var amostrador = nivel.getChunkSource().randomState().sampler();
        int margem = 500;
        for (int tentativa = 0; tentativa < TENTATIVAS; tentativa++) {
            long h = mistura(semente + tentativa * 0x632BE59BD9B4E019L, cx, cz);
            int x = cx * CELULA + margem + (int) Math.floorMod(h >>> 8, (long) (CELULA - 2 * margem));
            int z = cz * CELULA + margem + (int) Math.floorMod(h >>> 28, (long) (CELULA - 2 * margem));
            int[] centro = {(x >> 4) * 16 + 8, (z >> 4) * 16 + 8};
            if (terraFirme(gerador, amostrador, centro)) {
                return centro;
            }
        }
        return SEM_FLORESTA;
    }

    /** O centro e 12 pontos na volta da floresta (e um pouco alem) precisam ser terra (sem oceano, rio ou praia). */
    private static boolean terraFirme(net.minecraft.world.level.chunk.ChunkGenerator gerador,
                                      net.minecraft.world.level.biome.Climate.Sampler amostrador, int[] centro) {
        if (agua(gerador, amostrador, centro[0], centro[1])) {
            return false;
        }
        for (int i = 0; i < 12; i++) {
            double ang = i * Math.PI * 2.0D / 12.0D;
            int x = centro[0] + (int) Math.round(Math.cos(ang) * RAIO * 1.15D);
            int z = centro[1] + (int) Math.round(Math.sin(ang) * RAIO * 1.15D);
            if (agua(gerador, amostrador, x, z)) {
                return false;
            }
        }
        return true;
    }

    private static boolean agua(net.minecraft.world.level.chunk.ChunkGenerator gerador,
                                net.minecraft.world.level.biome.Climate.Sampler amostrador, int x, int z) {
        var bioma = gerador.getBiomeSource().getNoiseBiome(x >> 2, 64 >> 2, z >> 2, amostrador);
        return bioma.is(net.minecraft.tags.BiomeTags.IS_OCEAN) || bioma.is(net.minecraft.tags.BiomeTags.IS_DEEP_OCEAN)
                || bioma.is(net.minecraft.tags.BiomeTags.IS_RIVER) || bioma.is(net.minecraft.tags.BiomeTags.IS_BEACH);
    }

    /** Centro da floresta mais proxima de (x, z), ou null se nao houver nenhuma nas celulas em volta. */
    public static int[] maisPerto(net.minecraft.server.level.ServerLevel nivel, int x, int z) {
        int cx = Math.floorDiv(x, CELULA);
        int cz = Math.floorDiv(z, CELULA);
        int[] melhor = null;
        double menor = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int[] c = centroDaCelula(nivel, cx + dx, cz + dz);
                if (c == null) {
                    continue;
                }
                double d = Math.hypot(c[0] - x, c[1] - z);
                if (d < menor) {
                    menor = d;
                    melhor = c;
                }
            }
        }
        return melhor;
    }

    /**
     * Raio da floresta na direcao de (x, z) a partir do centro: um contorno irregular (nunca um circulo certinho),
     * calculado so com a posicao, para dar o mesmo resultado em todos os chunks.
     */
    public static double raioNaDirecao(int[] centro, int x, int z) {
        double angulo = Math.atan2(z - centro[1], x - centro[0]);
        long h = mistura(centro[0], centro[1], 7);
        double fase1 = (h & 0xFF) / 255.0D * Math.PI * 2.0D;
        double fase2 = ((h >>> 8) & 0xFF) / 255.0D * Math.PI * 2.0D;
        return RAIO * (1.0D + 0.14D * Math.sin(3.0D * angulo + fase1) + 0.09D * Math.sin(5.0D * angulo + fase2));
    }

    /** Distancia de (x, z) ate a borda da floresta (negativa dentro, positiva fora). */
    public static double distanciaDaBorda(int[] centro, int x, int z) {
        return Math.hypot(x - centro[0], z - centro[1]) - raioNaDirecao(centro, x, z);
    }
}
