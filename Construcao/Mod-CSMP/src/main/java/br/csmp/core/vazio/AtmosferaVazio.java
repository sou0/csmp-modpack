package br.csmp.core.vazio;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Conta da atmosfera do Vazio: a chuva vermelha vai e volta sozinha, so pelo tempo do mundo
 * (assim todos os jogadores veem a mesma chuva sem precisar de rede) e as cores seguem a chuva.
 */
public final class AtmosferaVazio {
    /** A chuva muda a cada 45 segundos (900 ticks), com 62% de chance de chover em cada trecho. */
    private static final long SEGMENTO = 900L;
    private static final int CHANCE_DE_CHUVA = 62;

    private AtmosferaVazio() {}

    private static boolean chovendo(long trecho) {
        long h = trecho * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return Math.floorMod(h, 100L) < CHANCE_DE_CHUVA;
    }

    private static float suave(float x) {
        float c = Mth.clamp(x, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }

    /** Intensidade da chuva vermelha de 0.0 a 1.0, com subida e descida suaves entre os trechos. */
    public static float chuva(long tempo, float parcial) {
        float f = (tempo + parcial) / SEGMENTO;
        long n = (long) Math.floor(f);
        float x = f - n;
        float antes = chovendo(n - 1) ? 1.0F : 0.0F;
        float agora = chovendo(n) ? 1.0F : 0.0F;
        float depois = chovendo(n + 1) ? 1.0F : 0.0F;
        float v = Mth.lerp(suave(x / 0.13F), antes, agora);
        return Mth.lerp(suave((x - 0.87F) / 0.13F), v, depois);
    }

    private static float pulso(long tempo, float parcial, float velocidade) {
        return 0.5F + 0.5F * Mth.sin((tempo + parcial) * velocidade);
    }

    public static Vec3 corNuvem(long tempo, float parcial) {
        float c = chuva(tempo, parcial);
        float p = pulso(tempo, parcial, 0.02F) * 0.06F;
        return new Vec3(0.66F - 0.30F * c + p, 0.05F - 0.02F * c, 0.07F - 0.03F * c);
    }

    public static Vec3 corCeu(long tempo, float parcial) {
        float c = chuva(tempo, parcial);
        float p = pulso(tempo, parcial, 0.013F) * 0.05F;
        return new Vec3(0.30F - 0.14F * c + p, 0.02F, 0.035F - 0.012F * c);
    }

    public static Vec3 corNevoa(long tempo, float parcial) {
        float c = chuva(tempo, parcial);
        return new Vec3(0.20F - 0.10F * c, 0.02F - 0.01F * c, 0.04F - 0.02F * c);
    }

    /** Distancia da nevoa: perto e densa, e ainda mais fechada durante a chuva. */
    public static float distanciaDaNevoa(long tempo, float parcial) {
        return Mth.lerp(chuva(tempo, parcial), 70.0F, 34.0F);
    }
}
