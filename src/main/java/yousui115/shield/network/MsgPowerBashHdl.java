package yousui115.shield.network;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import yousui115.shield.Util;

public class MsgPowerBashHdl implements IMessageHandler<MsgPowerBash, IMessage>
{
    /**
     * ■Client -> Server
     *   TODO 要リファクタリング
     */
    @Override
    public IMessage onMessage(MsgPowerBash msg, MessageContext ctx)
    {
        //■サーバのプレイヤー
        EntityPlayer player = ctx.getServerHandler().playerEntity;
        if (player == null) { return null; }

        Entity target = player.worldObj.getEntityByID(msg.getEntityID());
        if (target == null) { return null; }
        if (target instanceof EntityLiving)
        {
            ((EntityLiving)target).knockBack(player, 0.4F, (double)MathHelper.sin(player.rotationYaw * 0.017453292F), (double)(-MathHelper.cos(player.rotationYaw * 0.017453292F)));
            Util.tameAIDonmov(((EntityLiving)target), msg.getPower());
        }

        int damage = msg.getAmount();
        if (target instanceof EntityBat) { damage *= 1000; } // コウモリなんてふぁいっきらいだ！ばーか！
        target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage);


        if (!(target instanceof EntityLivingBase)) { return null; }
        EntityLivingBase targetEntity = (EntityLivingBase)target;
        for (EntityLivingBase entitylivingbase : player.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, targetEntity.getEntityBoundingBox().expand(1.0D, 0.25D, 1.0D)))
        {
            if (entitylivingbase != player && entitylivingbase != targetEntity && !player.isOnSameTeam(entitylivingbase) && player.getDistanceSqToEntity(entitylivingbase) < 9.0D)
            {
                damage = 1;
                entitylivingbase.knockBack(player, 0.4F, (double)MathHelper.sin(player.rotationYaw * 0.017453292F), (double)(-MathHelper.cos(player.rotationYaw * 0.017453292F)));
                if (entitylivingbase instanceof EntityBat) { damage *= 1000; } // コウモリなんてふぁいっきらいだ！ばーか！
                entitylivingbase.attackEntityFrom(DamageSource.causePlayerDamage(player), damage);

                if (entitylivingbase instanceof EntityLiving)
                {
                    Util.tameAIDonmov((EntityLiving)entitylivingbase, msg.getPower());
                }
            }
        }

        //■盾にダメージ
        player.getActiveItemStack().damageItem(msg.getPower(), player);
        //■パワーバッシュ音
        player.worldObj.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_BLAZE_HURT, player.getSoundCategory(), 1.0F, 1.0F);

//        float ticks = 1.0f;
//        double range = msg.getRange();

//        Vec3d posPlyEye = player.getPositionEyes(ticks);
//        Vec3d vecPlyLook = player.getLook(ticks);
//        Vec3d vecPlyLookC = getLook(player, ticks, 0);
//        Vec3d vecRangeC = getLook(player, ticks, 0).scale(range);
//
//        boolean isSound = false;

//        for (Entity target : player.worldObj.getEntitiesWithinAABB(Entity.class, player.getEntityBoundingBox().expand(range, range, range),
//                new Predicate<Entity>()
//                {
//                    public boolean apply(@Nullable Entity target)
//                    {
//                        return target != null && target.canBeCollidedWith();
//                    }
//                }
//        ))
//        {
//            //■P -> T の距離が range より遠いなら いらない子
//            double dist = target.getDistanceSqToEntity(player);
//            if (dist > range * range) { continue; }
//
//            //■プレイヤーから見たターゲットのベクトルを算出
////            Vec3d posTrgEye = target.getPositionEyes(ticks);
//            Vec3d posTrgEye = target.getPositionVector().addVector(0, (double)(target.height / 2f), 0);
//            Vec3d vecPtoT = posTrgEye.subtract(posPlyEye);
//
//            //■ベクトルからベクトル長を算出
//            double lengthRange = vecRangeC.lengthVector();
//            double lengthPtoT  = vecPtoT.lengthVector();
//
//            //■内積とベクトル長を使ってcosを求める
//            double cos = vecRangeC.dotProduct(vecPtoT) / ( lengthRange * lengthPtoT );
//
//            //■cosineからradianを求める
//            double radian = Math.acos(cos);
//
//            //■-PI/6 < hit < PI/6 (6dはお好みで変更)
//            if (radian < Math.PI / 6d)
//            {
//                target.attackEntityFrom(DamageSource.causePlayerDamage(player), 1);
//
//                if (target instanceof EntityLiving)
//                {
//                    EntityLiving living = (EntityLiving)target;
//                    living.knockBack(player, 0.5F * (float)msg.getPower(), (double)MathHelper.sin(player.rotationYaw * 0.017453292F), (double)(-MathHelper.cos(player.rotationYaw * 0.017453292F)));
//                    Util.tameAIDonmov(living, msg.getPower());
//                }
//
//                isSound = true;
//            }
//        }
//
//        //■敵にHitした
//        if (isSound)
//        {
//            //■盾にダメージ
//            player.getActiveItemStack().damageItem(msg.getPower(), player);
//            //■パワーバッシュ音
//            player.worldObj.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_BLAZE_HURT, player.getSoundCategory(), 1.0F, 1.0F);
//        }

        return null;
    }
}
