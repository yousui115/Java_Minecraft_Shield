package yousui115.shield.event;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import yousui115.shield.Util;

public class EventGuardMove
{
    /**
     * ■ガード時の移動速度調整
     * @param event
     */
    @SubscribeEvent
    public void changeGuardMoveSpeed(LivingEvent.LivingUpdateEvent event)
    {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) { return; }
        EntityPlayer player = (EntityPlayer)event.getEntityLiving();

        IAttributeInstance attri = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);

        //■「ガード時歩行速度上昇」 の剥奪
        if (attri.hasModifier(Util.modifierGuardWalkSpeed))
        {
            attri.removeModifier(Util.modifierGuardWalkSpeed);
        }

        //■満腹度が15以下なら走れない(Max 20)
         boolean canChangeSpeed = player.getFoodStats().getFoodLevel() > 15 ? true : false;

        if (canChangeSpeed && Util.isGuard(player))
        {
            //■「ガード時歩行速度上昇」 の付与
            attri.applyModifier(Util.modifierGuardWalkSpeed);
        }
    }
}
