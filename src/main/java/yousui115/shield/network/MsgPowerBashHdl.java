package yousui115.shield.network;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import yousui115.shield.Util;

import com.google.common.base.Predicate;

public class MsgPowerBashHdl implements IMessageHandler<MsgPowerBash, IMessage>
{
    @Override
    public IMessage onMessage(MsgPowerBash msg, MessageContext ctx)
    {
        //■サーバのプレイヤー
        EntityPlayer player = ctx.getServerHandler().playerEntity;

        float ticks = 1.0f;
        double range = msg.getRange();

        Vec3d posPlyEye = player.getPositionEyes(ticks);
        Vec3d vecPlyLook = player.getLook(ticks);
        Vec3d vecRange = vecPlyLook.scale(range);

        boolean isSound = false;

        for (Entity target : player.worldObj.getEntitiesWithinAABB(Entity.class, player.getEntityBoundingBox().expand(range, range, range),
                new Predicate<Entity>()
                {
                    public boolean apply(@Nullable Entity target)
                    {
                        return (target instanceof EntityLiving) || (target instanceof EntityFireball);
                    }
                }
        ))
        {
            //■P -> T の距離が range より遠いなら いらない子
            double dist = target.getDistanceSqToEntity(player);
            if (dist > range * range) { continue; }

            //■プレイヤーから見たターゲットのベクトルを算出
//            Vec3d posTrgEye = target.getPositionEyes(ticks);
            Vec3d posTrgEye = target.getPositionVector().addVector(0, (double)(target.height / 2f), 0);
            Vec3d vecPtoT = posTrgEye.subtract(posPlyEye);

            //■ベクトルからベクトル長を算出
            double lengthRange = vecRange.lengthVector();
            double lengthPtoT  = vecPtoT.lengthVector();

            //■内積とベクトル長を使ってcosを求める
            double cos = vecRange.dotProduct(vecPtoT) / ( lengthRange * lengthPtoT );

            //■cosineからradianを求める
            double radian = Math.acos(cos);

            //■-PI/6 < hit < PI/6 (6dはお好みで変更)
            if (radian < Math.PI / 6d)
            {
                target.attackEntityFrom(DamageSource.causePlayerDamage(player), 1);
                player.getActiveItemStack().damageItem(msg.getPower(), player);

                if (target instanceof EntityLiving)
                {
                    EntityLiving living = (EntityLiving)target;
                    living.knockBack(player, 0.5F * (float)msg.getPower(), (double)MathHelper.sin(player.rotationYaw * 0.017453292F), (double)(-MathHelper.cos(player.rotationYaw * 0.017453292F)));
                    Util.tameAIDonmov(living, msg.getPower());
                }

                isSound = true;
            }
        }

        if (isSound)
        {
            player.worldObj.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_BLAZE_HURT, player.getSoundCategory(), 1.0F, 1.0F);
        }

        return null;
    }
}
