package br.csmp.core.santuario;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A estatua de pedra do ritual. Fica em cima da base. As partes (pernas, torso, bracos, cabeca) aparecem
 * conforme os itens sao colocados; no Vazio ela ja esta inteira e brilha. Modelo e textura provisorios.
 */
public class EstatuaEntity extends Mob {
    public static final int PERNAS = 1;
    public static final int TORSO = 2;
    public static final int BRACOS = 4;
    public static final int CABECA = 8;
    public static final int TUDO = 15;

    private static final EntityDataAccessor<Integer> PARTES =
            SynchedEntityData.defineId(EstatuaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BRILHA =
            SynchedEntityData.defineId(EstatuaEntity.class, EntityDataSerializers.BOOLEAN);

    public EstatuaEntity(EntityType<? extends EstatuaEntity> tipo, Level nivel) {
        super(tipo, nivel);
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.setNoAi(true);
    }

    public static AttributeSupplier.Builder criarAtributos() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder construtor) {
        super.defineSynchedData(construtor);
        construtor.define(PARTES, 0);
        construtor.define(BRILHA, false);
    }

    public int partes() {
        return this.entityData.get(PARTES);
    }

    public void definirPartes(int mascara) {
        this.entityData.set(PARTES, mascara & TUDO);
    }

    public boolean brilha() {
        return this.entityData.get(BRILHA);
    }

    public void definirBrilho(boolean brilha) {
        this.entityData.set(BRILHA, brilha);
    }

    @Override
    public boolean isCurrentlyGlowing() {
        return brilha() || super.isCurrentlyGlowing();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && brilha() && this.tickCount % 6 == 0) {
            this.level().addParticle(ParticleTypes.END_ROD, this.getX() + (this.random.nextDouble() - 0.5D) * 0.8D,
                    this.getY() + this.random.nextDouble() * 1.9D, this.getZ() + (this.random.nextDouble() - 0.5D) * 0.8D,
                    0.0D, 0.02D, 0.0D);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Partes", partes());
        tag.putBoolean("Brilha", brilha());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        definirPartes(tag.getInt("Partes"));
        definirBrilho(tag.getBoolean("Brilha"));
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource fonte) {
        return !(fonte.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || fonte.isCreativePlayer());
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distancia) {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
        // estatua nao se mexe
    }

    @Override
    public InteractionResult mobInteract(Player jogador, InteractionHand mao) {
        if (mao != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide && jogador instanceof ServerPlayer sp
                && this.level() instanceof net.minecraft.server.level.ServerLevel servidor) {
            BlockPos base = this.blockPosition().below();
            ServerSantuario.clicarBase(servidor, base, sp, sp.getMainHandItem());
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
}
