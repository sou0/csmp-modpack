package br.csmp.core.mundo;

import br.csmp.core.sanidade.ServerSanidade;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;

/**
 * Tronco de betula com marcas que parecem olhos. Fica de olho fechado; quando um jogador chega a
 * {@link #ALCANCE} blocos o olho abre e drena a sanidade de quem esta perto (a venda anula isso).
 * O bloco se acorda sozinho a cada segundo com um tick agendado.
 */
public class BetulaOlhoBlock extends RotatedPillarBlock {
    public static final BooleanProperty ABERTO = BooleanProperty.create("aberto");
    public static final double ALCANCE = 10.0D;

    public BetulaOlhoBlock(Properties propriedades) {
        super(propriedades);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, net.minecraft.core.Direction.Axis.Y).setValue(ABERTO, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        super.createBlockStateDefinition(construtor);
        construtor.add(ABERTO);
    }

    @Override
    protected void onPlace(BlockState estado, Level nivel, BlockPos pos, BlockState antigo, boolean movido) {
        if (!nivel.isClientSide && !antigo.is(this)) {
            nivel.scheduleTick(pos, this, 20);
        }
    }

    @Override
    protected void tick(BlockState estado, ServerLevel nivel, BlockPos pos, RandomSource aleatorio) {
        AABB area = new AABB(pos).inflate(ALCANCE);
        List<ServerPlayer> perto = nivel.getEntitiesOfClass(ServerPlayer.class, area,
                jogador -> !jogador.isSpectator() && jogador.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= ALCANCE * ALCANCE);
        boolean abrir = !perto.isEmpty();
        if (estado.getValue(ABERTO) != abrir) {
            nivel.setBlock(pos, estado.setValue(ABERTO, abrir), 3);
        }
        for (ServerPlayer jogador : perto) {
            ServerSanidade.drenarOlho(jogador, nivel.getGameTime());
        }
        nivel.scheduleTick(pos, this, 20);
    }
}
