package yousui115.shield.network;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIFindEntityNearestPlayer;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
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

//        player.world.getEntitiesWithinAABB(EntityLiving.class, aabb, filter)   //コッチの方がいいかもー
        List<Entity> entities = player.getEntityWorld().getEntitiesInAABBexcluding(
                                    player,
                                    player.getEntityBoundingBox().grow(10d),
                                    new Predicate<Entity>()
                                    {
                                        public boolean apply(@Nullable Entity target)
                                        {
                                            //■タゲ
                                            boolean tage = false;

                                            //■M・O・B
                                            if (target instanceof EntityLiving && target instanceof IMob)
                                            {
                                                EntityLiving mob = (EntityLiving)target;
                                                for(EntityAITasks.EntityAITaskEntry entry : mob.targetTasks.taskEntries)
                                                {
                                                    //■「近くのプレイヤー」を狙う奴は敵だ
//                                                  if (entry.action.getClass() == EntityAINearestAttackableTarget.class)
                                                    if (entry.action instanceof EntityAINearestAttackableTarget)
                                                    {
                                                        Class clazz = (Class)ObfuscationReflectionHelper.getPrivateValue(EntityAINearestAttackableTarget.class,
                                                                                                                         (EntityAINearestAttackableTarget)entry.action, 0);

                                                        if (clazz == EntityPlayer.class)
                                                        {
                                                            tage = true;
                                                            break;
                                                        }
                                                    }
                                                    else if (entry.action instanceof EntityAIFindEntityNearestPlayer)
                                                    {
                                                        tage = true;
                                                        break;
                                                    }
                                                }
                                            }
//                                            //■敵MOBのタゲは必ず取る。
//                                            if (target instanceof IMob) { tage = true; }
//                                            //■ゴーレムは、「標的：敵MOB以外」の時にタゲが取れる。
//                                            else if (target instanceof EntityGolem)
//                                            {
//                                                EntityLivingBase enemy = ((EntityGolem) target).getAttackTarget();
//                                                if (enemy != null && !(enemy instanceof IMob))
//                                                {
//                                                    tage = true;
//                                                }
//                                            }

                                            return tage;
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
