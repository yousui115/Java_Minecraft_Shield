package yousui115.shield.event;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import yousui115.shield.Util;

public class EventGuardSplash
{
    /**
     * ■スプラッシュポーションをガードする
     * 　（厳密には、ガード状態だとスプラッシュポーションを打ち消す）
     * @param event
     */
    @SubscribeEvent
    public void deleteSplashPotion(ProjectileImpactEvent.Throwable event)
    {
        //■ポーション以外は処理しない
        if (!(event.getThrowable() instanceof EntityThrowable)) { return; }

        EntityThrowable throwable = event.getThrowable();
        boolean isSoundP = false;

        //■EntityLivingに当たった
        if (event.getRayTraceResult() != null &&
            event.getRayTraceResult().entityHit instanceof EntityLivingBase)
        {
            EntityLivingBase living = (EntityLivingBase)event.getRayTraceResult().entityHit;

            //■そのEntityはガード中であった
            if (Util.isGuard(living))
            {
                Vec3d motionP = new Vec3d(throwable.motionX, 0, throwable.motionZ);

                //■ガードしてるEntityのガード可能範囲内で、スプラッシュポーションがEntityに当たった。
                if (living.getLookVec().dotProduct(motionP) < 0.0D)
                {
                    throwable.setDead();
                    event.setCanceled(true);

                    //■ポーションなら、瓶の割れる音と、盾へのダメージ
                    if (throwable instanceof EntityPotion)
                    {
                        //TODO 本来はサーバで一連の処理をして、効果音用メッセージをクライアントに投げる。
                        //■効果音
                        living.playSound(SoundEvents.ENTITY_SPLASH_POTION_BREAK, 1.0F, living.getEntityWorld().rand.nextFloat() * 0.1F + 0.9F);

                        //■盾へのダメージ（腐食的な）
                        Util.damageShield(living, 5);
                    }

                    //■効果音
                    living.playSound(SoundEvents.ITEM_SHIELD_BLOCK, 1.0F, 0.8F + living.getEntityWorld().rand.nextFloat() * 0.4F);
                }

            }
        }
    }
}
