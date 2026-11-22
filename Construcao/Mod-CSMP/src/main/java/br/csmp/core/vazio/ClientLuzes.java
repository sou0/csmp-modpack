package br.csmp.core.vazio;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

/** Desenha, no Vazio, uma luz para cada jogador do overworld; as luzes seguem os jogadores. */
public final class ClientLuzes {
    /** Altura em que as luzes flutuam (o chao do Vazio termina em y=4). */
    private static final double ALTURA = 7.0D;

    private static final class Estado {
        double x;
        double z;
        double alvoX;
        double alvoZ;
        final int fase;

        Estado(double x, double z, int fase) {
            this.x = x;
            this.z = z;
            this.alvoX = x;
            this.alvoZ = z;
            this.fase = fase;
        }
    }

    private static final Map<UUID, Estado> LUZES = new HashMap<>();

    private ClientLuzes() {}

    public static void atualizar(LuzesPayload pacote) {
        Set<UUID> presentes = new HashSet<>();
        for (LuzesPayload.Luz luz : pacote.luzes()) {
            UUID id = new UUID(luz.idAlto(), luz.idBaixo());
            presentes.add(id);
            Estado estado = LUZES.get(id);
            if (estado == null) {
                estado = new Estado(luz.x(), luz.z(), id.hashCode());
                LUZES.put(id, estado);
            }
            estado.alvoX = luz.x();
            estado.alvoZ = luz.z();
        }
        LUZES.keySet().retainAll(presentes);
    }

    public static void limpar() {
        LUZES.clear();
    }

    public static void tick(ClientLevel nivel, LocalPlayer jogador, long tempo) {
        RandomSource r = nivel.random;
        for (Estado luz : LUZES.values()) {
            luz.x += (luz.alvoX - luz.x) * 0.2D;
            luz.z += (luz.alvoZ - luz.z) * 0.2D;
            double dx = luz.x - jogador.getX();
            double dz = luz.z - jogador.getZ();
            if (dx * dx + dz * dz > 100.0D * 100.0D) {
                continue; // longe demais para ser vista
            }
            double y = ALTURA + 0.4D * Math.sin(tempo * 0.1D + luz.fase);
            nivel.addParticle(ParticleTypes.END_ROD, luz.x, y, luz.z, 0.0D, 0.01D, 0.0D);
            if (tempo % 4 == 0) {
                nivel.addParticle(new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.45F), 2.0F),
                        luz.x + (r.nextDouble() - 0.5D) * 0.4D, y + r.nextDouble() * 0.3D,
                        luz.z + (r.nextDouble() - 0.5D) * 0.4D, 0.0D, 0.0D, 0.0D);
            }
            if (tempo % 6 == 0) {
                nivel.addParticle(ParticleTypes.END_ROD, luz.x, y + 1.0D + r.nextDouble() * 5.0D, luz.z, 0.0D, 0.0D, 0.0D);
            }
        }
    }
}
