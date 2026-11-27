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
 * Chao do Vazio: se comporta como o auroracite do The Beyond (particula e brilho ao andar), sem exigir a bota
 * especial. Som combinado com o lider: de terra, misturado com o som de passo do auroracite por cima. A particula e o
 * som do auroracite vem do The Beyond se ele estiver instalado; sem ele, so terra e esporos.
 */
public class ChaoVazioBlock extends Block {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ChaoVazioBlock(Properties propriedades) {
        super(propriedades);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    /** Sons de terra (como o bloco de terra do jogo); o passo do auroracite toca junto em stepOn. */
    public static SoundType tipoDeSom() {
        return SoundType.GRAVEL;
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
            // o "piso" por cima da terra: passo do auroracite mais baixo, procurado agora (registro ja completo)
            SoundEvent piso = som("the_beyond:block.auroracite.step", null);
            if (piso != null && !entidade.isSteppingCarefully()) {
                servidor.playSound(null, pos, piso, net.minecraft.sounds.SoundSource.BLOCKS, 0.45F,
                        0.9F + servidor.random.nextFloat() * 0.2F);
            }
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
