package br.csmp.core.npc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * O Profeta: NPC unico do CSMP. Fica parado, invulneravel e olha para quem chega perto.
 * Clicar com o botao direito abre a tela de missoes e conversa. Visual provisorio.
 */
public class ProfetaEntity extends PathfinderMob {

    public ProfetaEntity(EntityType<? extends ProfetaEntity> tipo, Level nivel) {
        super(tipo, nivel);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder criarAtributos() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    /** Sempre invulneravel: /summon recarrega os dados da entidade e apagaria a marca do construtor. */
    @Override
    public boolean isInvulnerable() {
        return true;
    }

    /**
     * Bloqueia todo dano, menos o que ignora invulnerabilidade (/kill, vazio) e jogadores no criativo.
     * Precisa ser aqui: o jogo decide o dano por este metodo, nao por isInvulnerable().
     */
    @Override
    public boolean isInvulnerableTo(DamageSource fonte) {
        return !(fonte.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || fonte.isCreativePlayer());
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanciaDoJogador) {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player jogador, InteractionHand mao) {
        if (mao != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide && jogador instanceof ServerPlayer servidorJogador) {
            PacketDistributor.sendToPlayer(servidorJogador,
                    new AbrirProfetaPayload(ServerProfeta.eruditosLibertados(servidorJogador.server)));
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
}
