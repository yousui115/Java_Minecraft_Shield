package yousui115.shield.event;

import java.util.Iterator;

import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import yousui115.shield.Util;


public class EventGuardAction
{
    /**
     * ■バッシュ中はダメージを通す。
     *   メリットもあれば、デメリットもある。
     * @param event
     */
    @SubscribeEvent
    public void changeUnBlockable(LivingAttackEvent event)
    {
        EntityLivingBase defender = event.getEntityLiving();
        DamageSource source = event.getSource();

        if (Util.isBashing(defender))
        {
            try
            {
                ObfuscationReflectionHelper.setPrivateValue(DamageSource.class, source, true, 19);
            }
            catch(Exception e)
            {
                //■例外メッセージを表示
                System.out.println("======================== ↓キリトリ↓ ========================");
                System.out.println(e.getMessage());
                System.out.println("======================== ↑キリトリ↑ ========================");
            }
        }
    }

    /**
     * ■ジャストガード時 に行う処理
     * @param event
     */
    @SubscribeEvent
    public void doJustGuard(LivingAttackEvent event)
    {
        EntityLivingBase defender = event.getEntityLiving();
        DamageSource source = event.getSource();

        //■ガード可能か否か(ガード不可攻撃、ガード状態、ガード方向の判定）
        if (!Util.canBlockDamageSource(source, defender, null)) { return; }

        //■ジャストガードしたか否か
        if (Util.isJustGuard(defender))
        {
            //■ジャストガードが発生したので、呼び出し元の後処理を行わせない。
            event.setCanceled(true);

            //■ジャストガード時の処理
            if (source.getSourceOfDamage() != null &&
                source.getSourceOfDamage().isEntityAlive() &&
                source.getSourceOfDamage() instanceof EntityLivingBase)
            {
                //■アタッカーに1ダメージ＋ノックバック
                EntityLivingBase attacker = (EntityLivingBase)source.getSourceOfDamage();
//                attacker.attackEntityFrom(DamageSource.causeMobDamage(defender), 1);
                attacker.knockBack(defender, 0.5F, defender.posX - attacker.posX, defender.posZ - attacker.posZ);
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

            //■
            if (Util.isJustGuard(blocker))
            {
                //▼爆発をジャストガードした時の処理

            }
//            else if (Util.isGuard(blocker) && getEnchGuardLevel_UsingItem(blocker) != 0)
            else if (Util.isGuard(blocker))
            {
                //▼爆発をガードした時の処理(ジャストガード除く)

                //■爆発を通常ガードした時の処理
                //  Explosion.doExplosionA() のほぼコピペ
                Explosion expl = event.getExplosion();
                double explosionX = expl.getPosition().xCoord;
                double explosionY = expl.getPosition().yCoord;
                double explosionZ = expl.getPosition().zCoord;
                float explSize = ObfuscationReflectionHelper.getPrivateValue(Explosion.class, expl, 8);

                float f3 = explSize * 2.0f;
                double d12 = blocker.getDistance(explosionX, explosionY, explosionZ) / (double)f3;

                if (d12 <= 1.0D)
                {
                    double d5 = blocker.posX - explosionX;
                    double d7 = blocker.posY + (double)blocker.getEyeHeight() - explosionY;
                    double d9 = blocker.posZ - explosionZ;
                    double d13 = (double)MathHelper.sqrt_double(d5 * d5 + d7 * d7 + d9 * d9);

                    if (d13 != 0.0D)
                    {
                        d5 = d5 / d13;
                        d7 = d7 / d13;
                        d9 = d9 / d13;
                        double d14 = (double)blocker.worldObj.getBlockDensity(expl.getPosition(), blocker.getEntityBoundingBox());
                        double d10 = (1.0D - d12) * d14;

//                        //■ノックバックはあるが、上には吹っ飛ばない。
//                        int level = getEnchGuardLevel_UsingItem(blocker);
//                        if (level == 1)
//                        {
//                            //■ノックバック耐性上昇 の剥奪
//                            IAttributeInstance iattributeinstance = blocker.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
//                            iattributeinstance.removeModifier(modifierGuardKnockback1);
//                        }

                        //■ダメージ処理
                        float damage = (float)((int)((d10 * d10 + d10) / 2.0D * 7.0D * (double)f3 + 1.0D));
//                        //TNT用
//                        if (level == 1)
//                        {
//                            //▼ガード性能１
//                            damage -= 2.0f;
//                        }
//                        else if (level == 2)
//                        {
//                            //▼ガード性能２
//                            damage -= 5.0f;
//                        }

//                        blocker.attackEntityFrom(DamageSource.causeExplosionDamage(expl), damage);
                        Entity exploder = (Entity)ObfuscationReflectionHelper.getPrivateValue(Explosion.class, expl, 7);
                        blocker.attackEntityFrom((new EntityDamageSource("explosion", exploder)).setDifficultyScaled().setExplosion(), damage);
//                        Util.damageShield(blocker, damage);

//                        double d11 = 1.0D;
                        double d11 = d10;

                        //■エンチャント：爆発耐性 でのノックバック耐性
                        if (blocker instanceof EntityLivingBase)
                        {
                            d11 = EnchantmentProtection.getBlastDamageReduction((EntityLivingBase)blocker, d10);
                        }

//                        //■ノックバックはあるが、上には吹っ飛ばない。
//                        if (level == 1)
//                        {
//                            blocker.motionX += d5 * d11;
//                            blocker.motionZ += d9 * d11;
//                        }

                        blocker.motionX += d5 * d11;
                        blocker.motionY += d7 * d11;
                        blocker.motionZ += d9 * d11;

                        if (blocker instanceof EntityPlayer)
                        {
                            EntityPlayer entityplayer = (EntityPlayer)blocker;

                            if (!entityplayer.isSpectator() && (!entityplayer.isCreative() || !entityplayer.capabilities.isFlying))
                            {
                                expl.getPlayerKnockbackMap().put(entityplayer, new Vec3d(d5 * d10, d7 * d10, d9 * d10));
                            }
                        }

                    }
                }
            }
            else
            {
                continue;
            }

            //■ブロッキングしてる生物 なので、従来の処理を走らせないようにリストから削除
            itr.remove();
        }
    }
}
