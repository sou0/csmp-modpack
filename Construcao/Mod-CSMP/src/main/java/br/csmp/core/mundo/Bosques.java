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

    /** Centro (x, z) da floresta da celula (cx, cz): sempre no meio de um chunk, longe das bordas da celula. */
    public static int[] centroDaCelula(long semente, int cx, int cz) {
        long h = mistura(semente, cx, cz);
        int margem = 500;
        int x = cx * CELULA + margem + (int) Math.floorMod(h >>> 8, (long) (CELULA - 2 * margem));
        int z = cz * CELULA + margem + (int) Math.floorMod(h >>> 28, (long) (CELULA - 2 * margem));
        return new int[] {(x >> 4) * 16 + 8, (z >> 4) * 16 + 8};
    }

    /** Centro da floresta mais proxima de (x, z). */
    public static int[] maisPerto(long semente, int x, int z) {
        int cx = Math.floorDiv(x, CELULA);
        int cz = Math.floorDiv(z, CELULA);
        int[] melhor = null;
        double menor = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int[] c = centroDaCelula(semente, cx + dx, cz + dz);
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
