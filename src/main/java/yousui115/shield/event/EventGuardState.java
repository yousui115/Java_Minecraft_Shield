package yousui115.shield.event;

import java.util.Iterator;

import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import yousui115.shield.Util;


public class EventGuardState
{
    /**
     * ■ガード時 に行う処理
     * @param event
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void doGuard(LivingAttackEvent event)
    {
        //■ダメージ処理がスキップされるようなので、何もしない。
        if (event.isCanceled()) { return; }

        //■攻撃を受けるEntity
        EntityLivingBase blocker = event.getEntityLiving();
        //■だめーじそーす
        DamageSource source = event.getSource();

        //■バッシュ中は無防備なので、どんなダメージも受け流す (UnBlockable=true)
        if (Util.isBashing(blocker))
        {
            ObfuscationReflectionHelper.setPrivateValue(DamageSource.class, source, true, 20);
            return;
        }

        boolean isJG = Util.isJustGuard(blocker);
        float amount = event.getAmount();
        boolean isGuard = Util.canBlockDamageSource(source, blocker, null);

        //■イベント
        GuardEvent guardEvent = ShieldHooks.onGuard(blocker, isJG, isGuard, source, amount);

        isJG = guardEvent.isJG;
        source = guardEvent.source;
        amount = guardEvent.amount;

        if (amount <= 0) { event.setCanceled(true); return; }

        //■ガード可能か否か(ガード不可攻撃、ガード状態、ガード方向の判定）
        if (!Util.canBlockDamageSource(source, blocker, null)) { return; }

        //▼ジャストガード。
        if (isJG)
        {
            //■ジャストガードが発生したので、呼び出し元の後処理を行わせない。
            event.setCanceled(true);

            //■ジャストガード時の処理
            if (source.getImmediateSource() instanceof EntityLivingBase)
            {
                //■アタッカーにノックバック
                EntityLivingBase attacker = (EntityLivingBase)source.getImmediateSource();
                attacker.knockBack(blocker, 0.5F, blocker.posX - attacker.posX, blocker.posZ - attacker.posZ);

                //■イベント
                ShieldHooks.onGuardMelee(blocker, attacker, isJG, source, amount, false);
            }

            //■音
            blocker.getEntityWorld().playSound(null, blocker.posX, blocker.posY, blocker.posZ, SoundEvents.ENTITY_BLAZE_HURT, SoundCategory.HOSTILE, 1.0F, 0.8F + blocker.getEntityWorld().rand.nextFloat() * 0.4F);

        }
        //▼ノーマルガード。
        else
        {
            //■プレイヤー以外は、従来の処理を走らせる。
            if (!(blocker instanceof EntityPlayer)) { return; }

            //■ガードしたので、呼び出し元の後処理を行わせない。
            event.setCanceled(true);

            //■アタッカー
            EntityLivingBase attacker = null;

            //■盾を無効化出来るか否か
            boolean canDisableShield = true;

            //■ガード時の処理
            if (source.getImmediateSource() instanceof EntityLivingBase)
            {
                attacker = (EntityLivingBase)source.getImmediateSource();

                //■イベント
                GuardMeleeEvent gmEvent = ShieldHooks.onGuardMelee(blocker, attacker, isJG, source, amount, canDisableShield);

                canDisableShield = gmEvent.canDisableShield;
            }

            //■盾にダメージ
            Util.damageShield(blocker, amount);

            //■音
            blocker.getEntityWorld().playSound(null, blocker.posX, blocker.posY, blocker.posZ, SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1.0F, 0.8F + blocker.getEntityWorld().rand.nextFloat() * 0.4F);

            //■Meleeっぽい攻撃時の処理
            if (attacker != null)
            {
                //■アタッカーにノックバック
                attacker.knockBack(blocker, 0.5F, blocker.posX - attacker.posX, blocker.posZ - attacker.posZ);

                //■盾の一時無効化
                if (canDisableShield == true &&         //盾を無効化できる
                    blocker instanceof EntityPlayer &&  //プレイヤーである
                    source.isProjectile() == false)     //接近攻撃である(?)
                {
                    if (attacker.getHeldItemMainhand().getItem().canDisableShield(attacker.getHeldItemMainhand(), blocker.getActiveItemStack(), blocker, attacker))
                    {
                        ((EntityPlayer)blocker).disableShield(true);
                    }
                }
            }
        }
    }

    /**
     * ■爆発判定に巻き込まれた際のイベント
     * @param event イベントコンテナ
     * @param event.world
     * @param event.explosion 爆発本体(Entityに非ず)
     * @param event.entityList 爆発に巻き込まれたEntity群
     */
    @SubscribeEvent
    public void doExplosionGuard(ExplosionEvent.Detonate event)
    {
        for (Iterator itr = event.getAffectedEntities().iterator(); itr.hasNext(); )
        {
            Object obj = itr.next();

            //■EntityLivingBase へ置換
            if (!(obj instanceof EntityLivingBase)) { continue; }
            EntityLivingBase blocker = (EntityLivingBase)obj;

            //■爆破耐性があるなら、従来の処理で構わないのでスルー
            if (blocker.isImmuneToExplosions()) { continue; }

            //■ガード可能か否か(ガード不可攻撃、ガード方向の判定）
            DamageSource source = DamageSource.causeExplosionDamage(event.getExplosion());
            if (!Util.canBlockDamageSource(source, blocker, event.getExplosion().getPosition())) { continue; }

            //  Explosion.doExplosionA() を参考
            Explosion expl = event.getExplosion();
            double explosionX = expl.getPosition().x;
            double explosionY = expl.getPosition().y;
            double explosionZ = expl.getPosition().z;
            float explSize = ObfuscationReflectionHelper.getPrivateValue(Explosion.class, expl, 8);

            float f3 = explSize * 2.0f;
            double d12 = blocker.getDistance(explosionX, explosionY, explosionZ) / (double)f3;

            if (d12 <= 1.0D)
            {
                double d5 = blocker.posX - explosionX;
                double d7 = blocker.posY + (double)blocker.getEyeHeight() - explosionY;
                double d9 = blocker.posZ - explosionZ;
                double d13 = (double)MathHelper.sqrt(d5 * d5 + d7 * d7 + d9 * d9);

                if (d13 != 0.0D)
                {
                    d5 = d5 / d13;
                    d7 = d7 / d13;
                    d9 = d9 / d13;
                    double d14 = (double)blocker.getEntityWorld().getBlockDensity(expl.getPosition(), blocker.getEntityBoundingBox());
                    double d10 = (1.0D - d12) * d14;

                    //■ダメージ処理
                    float damage = (float)((int)((d10 * d10 + d10) / 2.0D * 7.0D * (double)f3 + 1.0D));

                    Entity exploder = (Entity)ObfuscationReflectionHelper.getPrivateValue(Explosion.class, expl, 7);
                    boolean isDamage = true;
                    if (exploder != null)
                    {
                        isDamage = blocker.attackEntityFrom((new EntityDamageSource("explosion", exploder)).setDifficultyScaled().setExplosion(), damage);
                    }
                    else
                    {
                        isDamage = blocker.attackEntityFrom(DamageSource.causeExplosionDamage(expl), damage);
                    }

                    if (isDamage || !Util.isJustGuard(blocker))
                    {
                        double d11 = d10;

                        //■エンチャント：爆発耐性 でのノックバック耐性
                        if (blocker instanceof EntityLivingBase)
                        {
                            d11 = EnchantmentProtection.getBlastDamageReduction((EntityLivingBase)blocker, d10);
                        }

                        double knock = 0.1;

                        blocker.motionX += d5 * d11 * knock;
                        blocker.motionY += d7 * d11 * knock;
                        blocker.motionZ += d9 * d11 * knock;

                        if (blocker instanceof EntityPlayer)
                        {
                            EntityPlayer entityplayer = (EntityPlayer)blocker;

                            if (!entityplayer.isSpectator() && (!entityplayer.isCreative() || !entityplayer.capabilities.isFlying))
                            {
                                expl.getPlayerKnockbackMap().put(entityplayer, new Vec3d(d5 * d10, d7 * d10, d9 * d10));
                            }
                        }
                    } //if (isDamage)
                }
            }

            //■ブロッキングしてる生物 なので、従来の処理を走らせないようにリストから削除
            itr.remove();
        }
    }
}
