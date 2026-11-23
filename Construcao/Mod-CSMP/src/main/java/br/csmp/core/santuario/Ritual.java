package br.csmp.core.santuario;

import br.csmp.core.CsmpRegistro;
import br.csmp.core.CsmpSons;
import br.csmp.core.vazio.Vazio;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * O ritual de troca: os dois santuarios (o do overworld e o do Vazio, na mesma coordenada) trocam de lugar
 * um pedaco do terreno em volta, de formato irregular. Os jogadores NAO trocam: quem estava no overworld
 * continua la; quem estava preso no Vazio e devolvido ao overworld. Dura alguns segundos: primeiro o efeito,
 * depois a troca em lotes (para nao travar o servidor) e por fim a volta de quem estava preso.
 */
final class Ritual {
    private static final int ALTURA_DO_VAZIO = 50;
    private static final int COLUNAS_POR_TICK = 40;
    private static final int TICKS_DE_EFEITO = 80;
    private static final double ALCANCE_DOS_EFEITOS = 24.0D;

    private final MinecraftServer servidor;
    private final ServerLevel overworld;
    private final ServerLevel vazio;
    private final int cx;
    private final int cz;
    private final int yOverworld;
    private final int yVazio;
    private final int deslocamento;
    private final UUID chamador;
    private final List<int[]> colunas;
    private int indice = 0;
    private int tick = 0;

    private Ritual(MinecraftServer servidor, ServerLevel overworld, ServerLevel vazio, int cx, int cz,
                   int yOverworld, int yVazio, UUID chamador) {
        this.servidor = servidor;
        this.overworld = overworld;
        this.vazio = vazio;
        this.cx = cx;
        this.cz = cz;
        this.yOverworld = yOverworld;
        this.yVazio = yVazio;
        this.deslocamento = yOverworld - yVazio;
        this.chamador = chamador;
        this.colunas = forma(cx, cz);
    }

    /** Acha o altar nas duas dimensoes e prepara o ritual; devolve null se faltar algum dos dois. */
    static Ritual criar(MinecraftServer servidor, int x, int z, UUID chamador) {
        ServerLevel overworld = servidor.getLevel(Level.OVERWORLD);
        ServerLevel vazio = servidor.getLevel(Vazio.DIMENSAO);
        if (overworld == null || vazio == null) {
            return null;
        }
        int yOv = acharAltar(overworld, x, z);
        int yVz = acharAltar(vazio, x, z);
        if (yOv < 0 || yVz < 0) {
            return null;
        }
        return new Ritual(servidor, overworld, vazio, x, z, yOv, yVz, chamador);
    }

    int yOverworld() {
        return yOverworld;
    }

    int yVazio() {
        return yVazio;
    }

    private static int acharAltar(ServerLevel nivel, int x, int z) {
        if (!nivel.hasChunk(x >> 4, z >> 4)) {
            return -1;
        }
        for (int y = nivel.getMaxBuildHeight() - 1; y >= nivel.getMinBuildHeight(); y--) {
            if (nivel.getBlockState(new BlockPos(x, y, z)).is(CsmpRegistro.ALTAR_SANTUARIO.get())) {
                return y;
            }
        }
        return -1;
    }

    /** Coluna do miolo (santuario inteiro): troca a altura toda. */
    private static final int MIOLO = 0;
    /** Coluna da borda e das raizes: troca so ate um pouco acima do santuario, para nao abrir buracos no terreno. */
    private static final int RAIZ = 1;
    private static final int ALTURA_DA_RAIZ = 8;

    /**
     * Formato de ruina de portal: um miolo redondo que cobre o santuario inteiro, uma borda porosa (blocos
     * soltos que vao rareando) e raizes que se espalham para fora, tortas e com ramos. Cada elemento e
     * {dx, dz, tipo}; a lista vem do mais perto do altar para o mais longe.
     */
    private static List<int[]> forma(int x, int z) {
        long h = (x * 73856093L) ^ (z * 19349663L);
        h *= 0x9E3779B97F4A7C15L;
        h ^= h >>> 31;
        Random r = new Random(h);
        Map<Long, int[]> mapa = new HashMap<>();
        // miolo
        for (int dx = -13; dx <= 13; dx++) {
            for (int dz = -13; dz <= 13; dz++) {
                if (dx * dx + dz * dz <= 12.5D * 12.5D) {
                    mapa.put(chave(dx, dz), new int[] {dx, dz, MIOLO});
                }
            }
        }
        // borda porosa: a chance de trocar cai com a distancia
        for (int dx = -18; dx <= 18; dx++) {
            for (int dz = -18; dz <= 18; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 12.5D && d < 17.0D && r.nextDouble() < (17.0D - d) / 4.5D * 0.8D) {
                    mapa.putIfAbsent(chave(dx, dz), new int[] {dx, dz, RAIZ});
                }
            }
        }
        // raizes que saem do miolo
        int raizes = 7 + r.nextInt(3);
        for (int i = 0; i < raizes; i++) {
            double angulo = (i + r.nextDouble() * 0.6D) / raizes * Math.PI * 2.0D;
            raiz(mapa, r, Math.cos(angulo) * 10.0D, Math.sin(angulo) * 10.0D, angulo, 12 + r.nextInt(6), 0);
        }
        List<int[]> lista = new ArrayList<>(mapa.values());
        lista.sort(Comparator.comparingDouble(c -> c[0] * c[0] + c[1] * c[1]));
        return lista;
    }

    private static long chave(int dx, int dz) {
        return ((long) dx << 32) ^ (dz & 0xFFFFFFFFL);
    }

    /** Uma raiz: anda torta, grossa no comeco e fina no fim, e as vezes se divide em dois ramos. */
    private static void raiz(Map<Long, int[]> mapa, Random r, double px, double pz, double angulo, int passos, int nivel) {
        for (int s = 0; s < passos; s++) {
            angulo += (r.nextDouble() - 0.5D) * 0.8D;
            px += Math.cos(angulo);
            pz += Math.sin(angulo);
            if (Math.abs(px) > 26.0D || Math.abs(pz) > 26.0D) {
                return;
            }
            double grossura = s < passos / 3 ? 1.5D : (s < passos * 2 / 3 ? 1.0D : 0.0D);
            for (int ax = -1; ax <= 1; ax++) {
                for (int az = -1; az <= 1; az++) {
                    if (ax * ax + az * az <= grossura * grossura) {
                        int dx = (int) Math.round(px) + ax;
                        int dz = (int) Math.round(pz) + az;
                        mapa.putIfAbsent(chave(dx, dz), new int[] {dx, dz, RAIZ});
                    }
                }
            }
            if (nivel < 2 && s > 2 && s < passos - 3 && r.nextDouble() < 0.14D) {
                double lado = (r.nextBoolean() ? 1.0D : -1.0D) * (0.6D + r.nextDouble() * 0.6D);
                raiz(mapa, r, px, pz, angulo + lado, (passos - s) * 2 / 3, nivel + 1);
            }
        }
    }

    /** Anda um tick do ritual; devolve true quando terminou. */
    boolean tick() {
        this.tick++;
        if (this.tick == 1) {
            sonsEMensagem();
        }
        if (this.tick <= TICKS_DE_EFEITO) {
            if (this.tick % 3 == 0) {
                particulas();
            }
            if (this.tick == 1 || this.tick == 40) {
                efeitosNosJogadores();
            }
            return false;
        }
        if (this.indice < this.colunas.size()) {
            trocarLote();
            return false;
        }
        finalizar();
        return true;
    }

    private void sonsEMensagem() {
        this.overworld.playSound(null, this.cx + 0.5D, this.yOverworld, this.cz + 0.5D,
                CsmpSons.SANTUARIO_TROCA.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
        this.vazio.playSound(null, this.cx + 0.5D, this.yVazio, this.cz + 0.5D,
                CsmpSons.SANTUARIO_TROCA.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
        Component aviso = Component.literal("O santuario ressoa... o chao comeca a tremer, e os dois lados se confundem.")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
        avisarPerto(this.overworld, this.yOverworld, aviso);
        avisarPerto(this.vazio, this.yVazio, aviso);
    }

    private void avisarPerto(ServerLevel nivel, int y, Component mensagem) {
        for (ServerPlayer jogador : nivel.players()) {
            if (jogador.distanceToSqr(this.cx + 0.5D, y, this.cz + 0.5D) <= ALCANCE_DOS_EFEITOS * ALCANCE_DOS_EFEITOS) {
                jogador.sendSystemMessage(mensagem);
            }
        }
    }

    private void particulas() {
        this.overworld.sendParticles(ParticleTypes.REVERSE_PORTAL, this.cx + 0.5D, this.yOverworld + 1.2D, this.cz + 0.5D,
                14, 2.0D, 1.2D, 2.0D, 0.4D);
        this.vazio.sendParticles(ParticleTypes.REVERSE_PORTAL, this.cx + 0.5D, this.yVazio + 1.2D, this.cz + 0.5D,
                14, 2.0D, 1.2D, 2.0D, 0.4D);
    }

    private void efeitosNosJogadores() {
        efeito(this.overworld, this.yOverworld);
        efeito(this.vazio, this.yVazio);
    }

    private void efeito(ServerLevel nivel, int y) {
        for (ServerPlayer jogador : nivel.players()) {
            if (jogador.distanceToSqr(this.cx + 0.5D, y, this.cz + 0.5D) <= ALCANCE_DOS_EFEITOS * ALCANCE_DOS_EFEITOS) {
                jogador.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, false, false));
                jogador.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0, false, false, false));
            }
        }
    }

    /** Troca, coluna a coluna, o que esta no Vazio com o que esta no overworld (mesma coordenada x, z). */
    private void trocarLote() {
        int fim = Math.min(this.colunas.size(), this.indice + COLUNAS_POR_TICK);
        for (; this.indice < fim; this.indice++) {
            int[] coluna = this.colunas.get(this.indice);
            int x = this.cx + coluna[0];
            int z = this.cz + coluna[1];
            if (!this.overworld.hasChunk(x >> 4, z >> 4) || !this.vazio.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            int altura = coluna[2] == MIOLO ? ALTURA_DO_VAZIO : Math.min(ALTURA_DO_VAZIO, this.yVazio + ALTURA_DA_RAIZ);
            for (int yv = 1; yv <= altura; yv++) {
                int yo = yv + this.deslocamento;
                if (yo < this.overworld.getMinBuildHeight() || yo >= this.overworld.getMaxBuildHeight()) {
                    continue;
                }
                BlockPos posVazio = new BlockPos(x, yv, z);
                BlockPos posOverworld = new BlockPos(x, yo, z);
                BlockState doVazio = this.vazio.getBlockState(posVazio);
                BlockState doOverworld = this.overworld.getBlockState(posOverworld);
                if (doVazio == doOverworld || doVazio.hasBlockEntity() || doOverworld.hasBlockEntity()) {
                    continue;
                }
                this.vazio.setBlock(posVazio, doOverworld, 18);
                this.overworld.setBlock(posOverworld, doVazio, 18);
            }
        }
    }

    /** Devolve ao overworld quem estava preso e fecha o ritual. */
    private void finalizar() {
        ServerPlayer preso = this.chamador == null ? null : this.servidor.getPlayerList().getPlayer(this.chamador);
        if (preso != null && Vazio.eh(preso.level())) {
            double x = this.cx + 0.5D;
            // 8 blocos a frente do altar: no caminho, entre o salao e o torii (nao em cima do telhado)
            double z = this.cz + 8.5D;
            int y = this.yOverworld + 2;
            for (int cima = this.yOverworld + 25; cima >= this.yOverworld - 6; cima--) {
                BlockPos pos = new BlockPos(this.cx, cima, this.cz + 8);
                if (!this.overworld.getBlockState(pos.below()).isAir() && this.overworld.getBlockState(pos).isAir()
                        && this.overworld.getBlockState(pos.above()).isAir()) {
                    y = cima;
                    break;
                }
            }
            preso.removeEffect(MobEffects.DARKNESS);
            preso.removeEffect(MobEffects.CONFUSION);
            preso.teleportTo(this.overworld, x, y, z, preso.getYRot(), 0.0F);
            preso.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false, false));
            preso.sendSystemMessage(Component.literal("Voce sente o chao de verdade sob os pes. Voce voltou.")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        this.overworld.playSound(null, this.cx + 0.5D, this.yOverworld, this.cz + 0.5D,
                CsmpSons.SANTUARIO_CHAMADO.get(), SoundSource.BLOCKS, 2.0F, 1.2F);
        avisarPerto(this.overworld, this.yOverworld, Component.literal("A troca terminou. Os lados estao invertidos.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
