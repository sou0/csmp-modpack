package br.csmp.core.sanidade;

import br.csmp.core.CsmpMod;
import br.csmp.core.CsmpRegistro;
import br.csmp.core.mundo.Bosques;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Visao de quem usa a venda: a tela fica preta. Na Floresta das Betulas Indocis (e so la) aparece o contorno
 * dos blocos proximos e cada som que toca de verdade por perto (passos, mobs, portas) vira um anel que se abre. Tudo e desenhado em 2D por cima
 * do preto, projetando os pontos do mundo na tela.
 */
@EventBusSubscriber(modid = CsmpMod.ID, value = Dist.CLIENT)
public final class ClientVenda {
    private static final int RAIO = 7;

    private static Matrix4f modelo = new Matrix4f();
    private static Matrix4f projecao = new Matrix4f();
    private static Vec3 camera = Vec3.ZERO;
    private static boolean temMatrizes = false;
    /** Arestas {x1,y1,z1,x2,y2,z2} dos blocos expostos perto do jogador. */
    private static final List<float[]> ARESTAS = new ArrayList<>();

    private static final long DURACAO_DO_ANEL_MS = 1400L;

    /** Um som ouvido: posicao, quando tocou e quao forte (volume). */
    private record Som(double x, double y, double z, long inicio, double forca) {}

    private static final java.util.Deque<Som> SONS = new java.util.concurrent.ConcurrentLinkedDeque<>();

    private ClientVenda() {}

    /** Guarda a posicao de cada som do mundo que toca perto de quem esta vendado na floresta. */
    @SubscribeEvent
    public static void ouvir(net.neoforged.neoforge.client.event.sound.PlaySoundEvent evento) {
        var som = evento.getSound();
        Minecraft mc = Minecraft.getInstance();
        if (som == null || mc.level == null || !usando(mc.player) || !naFloresta(mc.level, mc.player)
                || som.isRelative()) {
            return;
        }
        var fonte = som.getSource();
        if (fonte == net.minecraft.sounds.SoundSource.MUSIC || fonte == net.minecraft.sounds.SoundSource.RECORDS
                || fonte == net.minecraft.sounds.SoundSource.MASTER || fonte == net.minecraft.sounds.SoundSource.VOICE) {
            return;
        }
        double dist = mc.player.distanceToSqr(som.getX(), som.getY(), som.getZ());
        if (dist < 2.25D || dist > 24.0D * 24.0D) {
            return;
        }
        double forca = 0.6D + Math.min(1.0D, (fonte == net.minecraft.sounds.SoundSource.HOSTILE ? 0.6D : 0.3D));
        SONS.addLast(new Som(som.getX(), som.getY(), som.getZ(), System.currentTimeMillis(), forca));
        while (SONS.size() > 64) {
            SONS.pollFirst();
        }
    }

    private static boolean usando(LocalPlayer jogador) {
        return jogador != null && jogador.getItemBySlot(EquipmentSlot.HEAD).is(CsmpRegistro.VENDA.get());
    }

    private static boolean naFloresta(ClientLevel nivel, LocalPlayer jogador) {
        return nivel.getBiome(jogador.blockPosition()).is(Bosques.BIOMA);
    }

    @SubscribeEvent
    public static void guardarMatrizes(RenderLevelStageEvent evento) {
        if (evento.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            modelo = new Matrix4f(evento.getModelViewMatrix());
            projecao = new Matrix4f(evento.getProjectionMatrix());
            camera = evento.getCamera().getPosition();
            temMatrizes = true;
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post evento) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel nivel = mc.level;
        LocalPlayer jogador = mc.player;
        if (nivel == null || jogador == null || !usando(jogador) || !naFloresta(nivel, jogador)) {
            ARESTAS.clear();
            return;
        }
        if (nivel.getGameTime() % 4L != 0L) {
            return;
        }
        ARESTAS.clear();
        BlockPos centro = jogador.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos vizinho = new BlockPos.MutableBlockPos();
        for (int dx = -RAIO; dx <= RAIO; dx++) {
            for (int dy = -RAIO; dy <= RAIO; dy++) {
                for (int dz = -RAIO; dz <= RAIO; dz++) {
                    pos.set(centro.getX() + dx, centro.getY() + dy, centro.getZ() + dz);
                    if (!nivel.getBlockState(pos).isSolidRender(nivel, pos)) {
                        continue;
                    }
                    for (Direction face : Direction.values()) {
                        vizinho.set(pos).move(face);
                        if (nivel.getBlockState(vizinho).isSolidRender(nivel, vizinho)) {
                            continue;
                        }
                        adicionarFace(pos, face);
                    }
                }
            }
        }
    }

    /** As 4 arestas da face do bloco que olha para a direcao dada. */
    private static void adicionarFace(BlockPos p, Direction face) {
        float x = p.getX();
        float y = p.getY();
        float z = p.getZ();
        float[][] c;
        switch (face) {
            case UP -> c = new float[][] {{x, y + 1, z}, {x + 1, y + 1, z}, {x + 1, y + 1, z + 1}, {x, y + 1, z + 1}};
            case DOWN -> c = new float[][] {{x, y, z}, {x + 1, y, z}, {x + 1, y, z + 1}, {x, y, z + 1}};
            case NORTH -> c = new float[][] {{x, y, z}, {x + 1, y, z}, {x + 1, y + 1, z}, {x, y + 1, z}};
            case SOUTH -> c = new float[][] {{x, y, z + 1}, {x + 1, y, z + 1}, {x + 1, y + 1, z + 1}, {x, y + 1, z + 1}};
            case WEST -> c = new float[][] {{x, y, z}, {x, y, z + 1}, {x, y + 1, z + 1}, {x, y + 1, z}};
            default -> c = new float[][] {{x + 1, y, z}, {x + 1, y, z + 1}, {x + 1, y + 1, z + 1}, {x + 1, y + 1, z}};
        }
        for (int i = 0; i < 4; i++) {
            float[] a = c[i];
            float[] b = c[(i + 1) % 4];
            ARESTAS.add(new float[] {a[0], a[1], a[2], b[0], b[1], b[2]});
        }
    }

    /** Projeta um ponto do mundo em coordenadas da GUI; false se estiver atras da camera. */
    private static boolean projetar(double x, double y, double z, int w, int h, float[] saida) {
        Vector4f v = new Vector4f((float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z), 1.0F);
        v.mul(modelo);
        v.mul(projecao);
        if (v.w <= 0.05F) {
            return false;
        }
        saida[0] = (v.x / v.w * 0.5F + 0.5F) * w;
        saida[1] = (1.0F - (v.y / v.w * 0.5F + 0.5F)) * h;
        return true;
    }

    @SubscribeEvent
    public static void desenhar(RenderGuiEvent.Pre evento) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel nivel = mc.level;
        LocalPlayer jogador = mc.player;
        if (nivel == null || !usando(jogador)) {
            return;
        }
        GuiGraphics g = evento.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();
        g.fill(0, 0, w, h, 0xFF000000);
        if (!temMatrizes || !naFloresta(nivel, jogador)) {
            return;
        }
        Matrix4f m = g.pose().last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        float[] a = new float[2];
        float[] b = new float[2];
        Vec3 olho = jogador.getEyePosition();
        for (float[] aresta : ARESTAS) {
            double dx = aresta[0] - olho.x;
            double dy = aresta[1] - olho.y;
            double dz = aresta[2] - olho.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > RAIO + 1 || !projetar(aresta[0], aresta[1], aresta[2], w, h, a)
                    || !projetar(aresta[3], aresta[4], aresta[5], w, h, b)) {
                continue;
            }
            int alfa = (int) (255 * Math.max(0.0D, 1.0D - dist / (RAIO + 1.0D)));
            bb.addVertex(m, a[0], a[1], 0.0F).setColor(220, 225, 235, alfa);
            bb.addVertex(m, b[0], b[1], 0.0F).setColor(220, 225, 235, alfa);
        }
        // sons: cada som que tocou de verdade por perto vira um anel que se abre e some
        long agora = System.currentTimeMillis();
        SONS.removeIf(som -> agora - som.inicio > DURACAO_DO_ANEL_MS);
        for (Som som : SONS) {
            if (!projetar(som.x, som.y, som.z, w, h, a)) {
                continue;
            }
            double fase = (agora - som.inicio) / (double) DURACAO_DO_ANEL_MS;
            float raio = (float) (2.0D + 22.0D * fase * som.forca);
            int alfa = (int) (220 * (1.0D - fase));
            int segmentos = 20;
            for (int i = 0; i < segmentos; i++) {
                double a0 = i * Math.PI * 2.0D / segmentos;
                double a1 = (i + 1) * Math.PI * 2.0D / segmentos;
                bb.addVertex(m, a[0] + (float) Math.cos(a0) * raio, a[1] + (float) Math.sin(a0) * raio, 0.0F).setColor(255, 90, 80, alfa);
                bb.addVertex(m, a[0] + (float) Math.cos(a1) * raio, a[1] + (float) Math.sin(a1) * raio, 0.0F).setColor(255, 90, 80, alfa);
            }
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(1.6F);
        try {
            BufferUploader.drawWithShader(bb.buildOrThrow());
        } catch (IllegalStateException semVertices) {
            // nada para desenhar neste quadro
        }
    }
}
