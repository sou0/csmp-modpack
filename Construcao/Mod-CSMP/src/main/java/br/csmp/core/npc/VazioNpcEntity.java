package br.csmp.core.npc;

import br.csmp.core.CsmpSons;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * NPCs do Vazio, no clima Yume Nikki: nao atacam ninguem, so existem, olham e dizem coisas sem sentido.
 * Os tres tipos (sombra, crianca, sentinela) compartilham esta classe; o comportamento muda pelo tipo.
 */
public class VazioNpcEntity extends PathfinderMob {

    private static final String[] FALAS_SOMBRA = {
            "...voce tambem esqueceu o caminho?",
            "ele esta atras da porta. sempre esteve.",
            "silencio. escute. ainda escuta?",
            "eu ja fui alguem que andava ao sol.",
            "nao olhe para tras. nao ha nada. ainda."
    };
    private static final String[] FALAS_CRIANCA = {
            "o ceu esta chorando de novo...",
            "quer brincar de esconde-esconde? ninguem nunca me acha.",
            "a porta vermelha esta aberta. a porta vermelha esta sempre aberta.",
            "o balanco se mexe sozinho quando ninguem olha.",
            "eu contei ate cem. eu contei ate cem de novo."
    };
    private static final String[] FALAS_SENTINELA = {
            "NAO OLHE PARA CIMA.",
            "o santuario lembra de voce.",
            "alguem la fora ainda diz o seu nome.",
            "a luz que se move nao e sua amiga.",
            "TUDO AQUI FOI VISTO."
    };

    public VazioNpcEntity(EntityType<? extends VazioNpcEntity> tipo, Level nivel) {
        super(tipo, nivel);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder criarAtributos(double velocidade) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, velocidade)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    /** Nome do tipo (sombra, crianca ou sentinela), tirado do registro da entidade. */
    public String tipo() {
        return BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        if ("crianca".equals(tipo())) {
            this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
            this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.45D));
        } else {
            this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, "sentinela".equals(tipo()) ? 24.0F : 14.0F));
        }
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    /** Bloqueia todo dano, menos o que ignora invulnerabilidade (/kill, vazio) e jogadores no criativo. */
    @Override
    public boolean isInvulnerableTo(DamageSource fonte) {
        return !(fonte.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || fonte.isCreativePlayer());
    }

    @Override
    public boolean isInvulnerable() {
        return true;
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
    protected SoundEvent getAmbientSound() {
        return CsmpSons.NPC_MURMURIO.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    private String[] falas() {
        return switch (tipo()) {
            case "crianca" -> FALAS_CRIANCA;
            case "sentinela" -> FALAS_SENTINELA;
            default -> FALAS_SOMBRA;
        };
    }

    private void sussurrar(Player jogador) {
        String[] lista = falas();
        jogador.displayClientMessage(Component.literal(lista[this.random.nextInt(lista.length)])
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        // de tempos em tempos, perto de um jogador: um sussurro, e um efeito de terror proprio de cada tipo
        if (this.tickCount % 160 == 40) {
            Player perto = this.level().getNearestPlayer(this, 7.0D);
            if (perto != null) {
                sussurrar(perto);
            }
        }
        if ("sombra".equals(tipo()) && this.tickCount % 60 == 0) {
            Player muito = this.level().getNearestPlayer(this, 4.0D);
            if (muito instanceof ServerPlayer jogador && !jogador.hasEffect(MobEffects.DARKNESS)) {
                jogador.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false, false));
            }
        }
        if ("sentinela".equals(tipo()) && this.tickCount % 70 == 0) {
            Player perto = this.level().getNearestPlayer(this, 14.0D);
            if (perto != null) {
                this.level().playSound(null, perto.getX(), perto.getY(), perto.getZ(),
                        CsmpSons.SUSTO_CORACAO.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player jogador, InteractionHand mao) {
        if (mao != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide) {
            sussurrar(jogador);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    CsmpSons.NPC_MURMURIO.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
}
