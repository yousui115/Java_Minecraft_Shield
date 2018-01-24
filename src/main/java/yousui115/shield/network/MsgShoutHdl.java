package yousui115.shield.network;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import yousui115.shield.Util;

public class MsgShoutHdl implements IMessageHandler<MsgShout, IMessage>
{
    /**
     * ■Client -> Server
     */
    @Override
    public IMessage onMessage(MsgShout message, MessageContext ctx)
    {

        //■サーバのプレイヤー
        EntityPlayer player = ctx.getServerHandler().player;

        List<Entity> entities = player.getEntityWorld().getEntitiesInAABBexcluding(
                                    player,
                                    player.getEntityBoundingBox().grow(10d),
                                    new Predicate<Entity>()
                                    {
                                        public boolean apply(@Nullable Entity target)
                                        {
                                            if (target instanceof EntityLiving)
                                            {
                                                if (target instanceof EntityTameable)
                                                {
                                                    return false;
                                                }

                                                return true;
                                            }
                                            else
                                            {
                                                return false;
                                            }
                                        }
                                    });

        for (Entity entity : entities)
        {
            if (entity instanceof EntityLiving)
            {
                Util.tameAIAnger((EntityLiving)entity, player);
            }
        }

        player.getEntityWorld().playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_SHIELD_BLOCK, player.getSoundCategory(), 1.0F, 1.0F);


        return null;
    }

}
