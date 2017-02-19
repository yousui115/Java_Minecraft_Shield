package yousui115.shield.network;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import yousui115.shield.Util;
import yousui115.shield.event.BashEvent;

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
            target.attackEntityFrom(DamageSource.causePlayerDamage(player), msg.getAmount());

            //■ドンムブ
            Util.tameAIDonmov(target, power);

            //■バッシュ打撃音
            player.worldObj.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_SHIELD_BLOCK, player.getSoundCategory(), 1.0F, 1.0F);

            //■耐久値減少(バッシュ:1 パワーバッシュ:2)
            player.getActiveItemStack().damageItem(power, player);

            //■イベント
            BashEvent bashEvent = new BashEvent(player, (EntityLivingBase)target, power);
            MinecraftForge.EVENT_BUS.post(bashEvent);
        }

        return null;
    }
}
