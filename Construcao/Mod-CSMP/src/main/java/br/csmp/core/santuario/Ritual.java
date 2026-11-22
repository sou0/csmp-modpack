package br.csmp.core.santuario;

import br.csmp.core.CsmpRegistro;
import br.csmp.core.CsmpSons;
import br.csmp.core.vazio.Vazio;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private static final int COLUNAS_POR_TICK = 30;
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

    /** Formato irregular (uma "mancha" de uns 10 blocos de raio), do mais perto do altar para o mais longe. */
    private static List<int[]> forma(int x, int z) {
        long h = (x * 73856093L) ^ (z * 19349663L);
        h *= 0x9E3779B97F4A7C15L;
        h ^= h >>> 31;
        double fase1 = ((h >>> 5) & 1023L) / 1023.0D * Math.PI * 2.0D;
        double fase2 = ((h >>> 15) & 1023L) / 1023.0D * Math.PI * 2.0D;
        List<int[]> lista = new ArrayList<>();
        for (int dx = -15; dx <= 15; dx++) {
            for (int dz = -15; dz <= 15; dz++) {
                double distancia = Math.sqrt(dx * dx + dz * dz);
                double angulo = Math.atan2(dz, dx);
                double raio = 10.5D + 3.0D * Math.sin(2.0D * angulo + fase1) + 1.8D * Math.sin(5.0D * angulo + fase2);
                if (distancia <= raio) {
                    lista.add(new int[] {dx, dz});
                }
            }
        }
        lista.sort(Comparator.comparingDouble(c -> c[0] * c[0] + c[1] * c[1]));
        return lista;
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
            for (int yv = 1; yv <= ALTURA_DO_VAZIO; yv++) {
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
            double z = this.cz + 3.5D;
            int y = this.yOverworld + 2;
            for (int cima = this.yOverworld + 25; cima >= this.yOverworld - 6; cima--) {
                BlockPos pos = new BlockPos(this.cx, cima, this.cz + 3);
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
