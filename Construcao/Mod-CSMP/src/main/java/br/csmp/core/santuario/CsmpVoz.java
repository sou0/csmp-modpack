package br.csmp.core.santuario;

import br.csmp.core.vazio.ServerLuzes;
import br.csmp.core.vazio.Vazio;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Tudo que o CSMP faz com o Simple Voice Chat. Tres caminhos de voz:
 * <ol>
 *   <li><b>Estatuas ligadas:</b> quem esta a 16 blocos de uma estatua ouve quem esta a 16 blocos da outra, nos
 *       dois sentidos. A voz sai na mesma posicao relativa a estatua, como se fossem o mesmo lugar.</li>
 *   <li><b>Luzes do Vazio:</b> quem esta preso ouve, vindo da luz, a voz dos jogadores do overworld que estao a
 *       ate {@link ServerLuzes#ALCANCE_DO_ECO} blocos (no plano) dele.</li>
 *   <li><b>Sussurros:</b> de 1 a 3 minutos, quem esta preso ouve uns segundos da voz real de alguem vivo, longe,
 *       distorcida e baixa, sem posicao (parece vir de dentro da cabeca).</li>
 * </ol>
 * Os eventos de microfone chegam numa thread propria do voice chat; por isso os mapas sao concorrentes.
 */
@ForgeVoicechatPlugin
public class CsmpVoz implements VoicechatPlugin {
    private static final double RAIO_DA_ESTATUA = 16.0D;
    private static final double ALTURA_DAS_LUZES = 7.0D;
    private static final long DURACAO_DO_SUSSURRO_MS = 3500L;
    private static final long INTERVALO_MIN_MS = 60_000L;
    private static final long INTERVALO_MAX_MS = 180_000L;

    private static volatile VoicechatServerApi api;

    /** Um sussurro em andamento (ou agendado) para quem esta preso. */
    private static final class Sussurro {
        volatile long proximo = System.currentTimeMillis() + sortearIntervalo();
        volatile UUID fonte;
        volatile long ate;
        double fase;
        double filtro;
        OpusEncoder codificador;
        StaticAudioChannel canal;
    }

    private static final Map<UUID, Sussurro> SUSSURROS = new ConcurrentHashMap<>();
    private static final Map<UUID, OpusDecoder> DECODIFICADORES = new ConcurrentHashMap<>();

    @Override
    public String getPluginId() {
        return "csmp";
    }

    @Override
    public void initialize(VoicechatApi voicechat) {
        // a API de servidor chega no evento abaixo
    }

    @Override
    public void registerEvents(EventRegistration registro) {
        registro.registerEvent(VoicechatServerStartedEvent.class, evento -> api = evento.getVoicechat());
        registro.registerEvent(MicrophonePacketEvent.class, CsmpVoz::microfone);
        registro.registerEvent(PlayerDisconnectedEvent.class, evento -> esquecer(evento.getPlayerUuid()));
    }

    private static long sortearIntervalo() {
        return ThreadLocalRandom.current().nextLong(INTERVALO_MIN_MS, INTERVALO_MAX_MS);
    }

    private static void esquecer(UUID jogador) {
        OpusDecoder decodificador = DECODIFICADORES.remove(jogador);
        if (decodificador != null) {
            decodificador.close();
        }
        Sussurro s = SUSSURROS.remove(jogador);
        if (s != null && s.codificador != null) {
            s.codificador.close();
        }
    }

    private static void microfone(MicrophonePacketEvent evento) {
        VoicechatServerApi voz = api;
        MinecraftServer servidor = ServerSantuario.servidor();
        VoicechatConnection origem = evento.getSenderConnection();
        if (voz == null || servidor == null || origem == null
                || !(origem.getPlayer().getPlayer() instanceof ServerPlayer emissor)) {
            return;
        }
        pontesDasEstatuas(voz, servidor, emissor, evento);
        if (emissor.level().dimension() == Level.OVERWORLD) {
            ServerLevel vazio = servidor.getLevel(Vazio.DIMENSAO);
            if (vazio != null && !vazio.players().isEmpty()) {
                luzesESussurros(voz, vazio, emissor, evento);
            }
        }
    }

    // ------------------------------------------------------------------ 1. estatuas ligadas

    private static void pontesDasEstatuas(VoicechatServerApi voz, MinecraftServer servidor, ServerPlayer emissor,
                                          MicrophonePacketEvent evento) {
        boolean noVazio = Vazio.eh(emissor.level());
        for (BlockPos[] par : ServerSantuario.paresAtivos()) {
            BlockPos daqui = noVazio ? par[0] : par[1];
            BlockPos para = noVazio ? par[1] : par[0];
            if (emissor.distanceToSqr(daqui.getX() + 0.5D, daqui.getY() + 1.0D, daqui.getZ() + 0.5D)
                    > RAIO_DA_ESTATUA * RAIO_DA_ESTATUA) {
                continue;
            }
            ServerLevel destino = servidor.getLevel(noVazio ? Level.OVERWORLD : Vazio.DIMENSAO);
            if (destino == null) {
                continue;
            }
            for (ServerPlayer ouvinte : destino.players()) {
                if (ouvinte == emissor || ouvinte.distanceToSqr(para.getX() + 0.5D, para.getY() + 1.0D, para.getZ() + 0.5D)
                        > RAIO_DA_ESTATUA * RAIO_DA_ESTATUA) {
                    continue;
                }
                Position posicao = voz.createPosition(
                        emissor.getX() - daqui.getX() + para.getX(),
                        emissor.getY() - daqui.getY() + para.getY(),
                        emissor.getZ() - daqui.getZ() + para.getZ());
                enviarPosicional(voz, ouvinte, evento, posicao, RAIO_DA_ESTATUA);
            }
        }
    }

    private static void enviarPosicional(VoicechatServerApi voz, ServerPlayer ouvinte, MicrophonePacketEvent evento,
                                         Position posicao, double alcance) {
        VoicechatConnection conexao = voz.getConnectionOf(ouvinte.getUUID());
        if (conexao == null || !conexao.isConnected()) {
            return;
        }
        voz.sendLocationalSoundPacketTo(conexao, evento.getPacket().locationalSoundPacketBuilder()
                .position(posicao).distance((float) alcance).build());
    }

    // ------------------------------------------------------------------ 2 e 3. luzes e sussurros

    private static void luzesESussurros(VoicechatServerApi voz, ServerLevel vazio, ServerPlayer emissor,
                                        MicrophonePacketEvent evento) {
        long agora = System.currentTimeMillis();
        double alcance = ServerLuzes.ALCANCE_DO_ECO;
        short[] amostras = null;
        for (ServerPlayer preso : vazio.players()) {
            double dx = preso.getX() - emissor.getX();
            double dz = preso.getZ() - emissor.getZ();
            if (dx * dx + dz * dz <= alcance * alcance) {
                // perto da luz: ouve a voz de verdade, vindo da luz
                Position luz = voz.createPosition(emissor.getX(), ALTURA_DAS_LUZES, emissor.getZ());
                enviarPosicional(voz, preso, evento, luz, alcance);
                continue;
            }
            Sussurro s = SUSSURROS.computeIfAbsent(preso.getUUID(), id -> new Sussurro());
            if (s.fonte == null && agora >= s.proximo) {
                s.fonte = emissor.getUUID();
                s.ate = agora + DURACAO_DO_SUSSURRO_MS;
            }
            if (!emissor.getUUID().equals(s.fonte)) {
                continue;
            }
            if (agora > s.ate) {
                s.fonte = null;
                s.proximo = agora + sortearIntervalo();
                continue;
            }
            if (amostras == null) {
                amostras = decodificar(voz, emissor.getUUID(), evento.getPacket().getOpusEncodedData());
                if (amostras == null) {
                    return;
                }
            }
            sussurrar(voz, vazio, preso, s, amostras);
        }
    }

    private static short[] decodificar(VoicechatServerApi voz, UUID fonte, byte[] opus) {
        if (opus == null || opus.length == 0) {
            return null;
        }
        OpusDecoder decodificador = DECODIFICADORES.computeIfAbsent(fonte, id -> voz.createDecoder());
        return decodificador.decode(opus);
    }

    /**
     * Distorce e manda para quem esta preso: modulacao em anel (voz metalica, "de outro lugar"), um filtro que
     * abafa os agudos e volume baixo. O tamanho do quadro nao muda, entao o voice chat toca no tempo certo.
     */
    private static void sussurrar(VoicechatServerApi voz, ServerLevel vazio, ServerPlayer preso, Sussurro s, short[] entrada) {
        VoicechatConnection conexao = voz.getConnectionOf(preso.getUUID());
        if (conexao == null || !conexao.isConnected()) {
            return;
        }
        if (s.canal == null || s.canal.isClosed()) {
            s.canal = voz.createStaticAudioChannel(UUID.randomUUID(), voz.fromServerLevel(vazio), conexao);
            if (s.canal == null) {
                return;
            }
        }
        if (s.codificador == null || s.codificador.isClosed()) {
            s.codificador = voz.createEncoder();
        }
        short[] saida = new short[entrada.length];
        double passo = 2.0D * Math.PI * 42.0D / 48000.0D;
        for (int i = 0; i < entrada.length; i++) {
            double anel = Math.sin(s.fase);
            s.fase += passo;
            double amostra = entrada[i] * (0.55D * anel + 0.25D);
            s.filtro += 0.18D * (amostra - s.filtro);
            saida[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, s.filtro * 0.7D));
        }
        if (s.fase > Math.PI * 2.0D) {
            s.fase %= Math.PI * 2.0D;
        }
        s.canal.send(s.codificador.encode(saida));
    }
}
