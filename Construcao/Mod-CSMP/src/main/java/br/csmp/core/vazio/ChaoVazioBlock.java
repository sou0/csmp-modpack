package br.csmp.core.vazio;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Chao do Vazio: terra preta com mato vermelho que se comporta como o auroracite do The Beyond (som de passo,
 * particula e brilho ao andar), mas sem exigir a bota especial. Os sons e a particula sao pegos do The Beyond
 * se ele estiver instalado; sem ele, cai para sons de grama e particulas de esporo.
 */
public class ChaoVazioBlock extends Block {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ChaoVazioBlock(Properties propriedades) {
        super(propriedades);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    /** Sons de bloco copiados do auroracite (passo leve, quebra pesada). */
    public static SoundType tipoDeSom() {
        SoundEvent passo = som("the_beyond:block.auroracite.step", SoundEvents.GRASS_STEP);
        SoundEvent quebra = som("the_beyond:block.auroracite.break", SoundEvents.GRASS_BREAK);
        return new SoundType(1.0F, 1.0F, quebra, passo, quebra, passo, quebra);
    }

    private static SoundEvent som(String id, SoundEvent padrao) {
        return BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.parse(id)).orElse(padrao);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        construtor.add(POWERED);
    }

    @Override
    public void stepOn(Level nivel, BlockPos pos, BlockState estado, Entity entidade) {
        if (nivel instanceof ServerLevel servidor && !estado.getValue(POWERED)
                && entidade.getKnownMovement().length() > 0.1D) {
            servidor.setBlockAndUpdate(pos, estado.setValue(POWERED, true));
            servidor.scheduleTick(pos, this, 12);
            ParticleType<?> tipo = BuiltInRegistries.PARTICLE_TYPE
                    .getOptional(ResourceLocation.parse("the_beyond:auroracite_step")).orElse(null);
            ParticleOptions particula = tipo instanceof ParticleOptions opcoes ? opcoes : ParticleTypes.CRIMSON_SPORE;
            servidor.sendParticles(particula, pos.getX() + 0.5D, pos.getY() + 1.01D, pos.getZ() + 0.5D,
                    1, 0.1D, 0.0D, 0.1D, 0.0D);
        }
        super.stepOn(nivel, pos, estado, entidade);
    }

    @Override
    protected void tick(BlockState estado, ServerLevel nivel, BlockPos pos, RandomSource aleatorio) {
        if (estado.getValue(POWERED)) {
            nivel.setBlockAndUpdate(pos, estado.setValue(POWERED, false));
        }
    }
}
