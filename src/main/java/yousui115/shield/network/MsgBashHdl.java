package yousui115.shield.network;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import yousui115.shield.Util;

public class MsgBashHdl implements IMessageHandler<MsgBash, IMessage>
{

    @Override
    public IMessage onMessage(MsgBash msg, MessageContext ctx)
    {
        //■サーバのプレイヤー
        EntityPlayer player = ctx.getServerHandler().playerEntity;

        //■バッシュターゲット
        Entity entity = player.worldObj.getEntityByID(msg.getTargetID());

        if (entity instanceof EntityLiving)
        {
            //■ターゲット(置き換え)
            EntityLiving target = (EntityLiving)entity;
            int power = MathHelper.clamp_int(msg.getPower(), 1, 2);

            //■メインターゲットをノックバック＋ダメージ(バッシュ強度依存)
            target.knockBack(player, 0.5F * (float)power, player.posX - target.posX, player.posZ - target.posZ);
            target.attackEntityFrom(DamageSource.causePlayerDamage(player), power - msg.getAmount());

            Util.tameAIDonmov(target, power);

            //■バッシュが決まってるのでドンムブのポーション効果付与
//            Potion potion = Potion.REGISTRY.getObject(new ResourceLocation("slowness"));
//            potion.registerPotionAttributeModifier(SharedMonsterAttributes.MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160890", -1.0D, 2);
//            target.addPotionEffect(new PotionEffect(potion, 20 * power, 2));

            //▼パワーバッシュの場合
//            if (power == 2)
//            {
//                //■メインターゲット周囲に居るサブターゲットをピックアップ
//                for (EntityLiving subTarget : player.worldObj.getEntitiesWithinAABB(EntityLiving.class, target.getEntityBoundingBox().expand(1.0D, 0.25D, 1.0D)))
//                {
//                    //▼メインターゲット以外 かつ プレイヤーと同チーム以外 かつ 距離が9より近い
//                    if (subTarget != target
//                        && !player.isOnSameTeam(subTarget)
//                        && player.getDistanceSqToEntity(subTarget) < 9.0D)
//                    {
//                        //■サブターゲットをノックバック＋ダメージ(バッシュ強度依存)
//                        subTarget.knockBack(player, 0.5F * (float)power, (double)MathHelper.sin(player.rotationYaw * 0.017453292F), (double)(-MathHelper.cos(player.rotationYaw * 0.017453292F)));
//                        subTarget.attackEntityFrom(DamageSource.causePlayerDamage(player), message.getAmount());
//
//                        tameAIDonmov(subTarget, power);
//                        //■バッシュが決まるとドンムブ
////                        subTarget.addPotionEffect(new PotionEffect(potion, 20 * power, 2));
//                    }
//                }
//
//                player.worldObj.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_BLAZE_HURT, player.getSoundCategory(), 1.0F, 1.0F);
//                player.spawnSweepParticles();
//            }

            //■バッシュ打撃音
            player.worldObj.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_SHIELD_BLOCK, player.getSoundCategory(), 1.0F, 1.0F);

            //■耐久値減少(バッシュ:1 パワーバッシュ:2)
            player.getActiveItemStack().damageItem(power, player);
        }

        return null;
    }
}
