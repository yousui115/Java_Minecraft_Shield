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

    /**
     * ■ガード時の移動速度調整
     * @param event
     */
//    @SubscribeEvent
//    public void applyGuardMoveSprint(LivingEvent.LivingUpdateEvent event)
//    {
//        if (!(event.getEntityLiving() instanceof EntityPlayer)) { return; }
//        EntityPlayer player = (EntityPlayer)event.getEntityLiving();
//
//        IAttributeInstance attriSpeed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
//        IAttributeInstance attriKnock = player.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
//
//        //■「ガード時歩行速度上昇」 の剥奪
//        if (attriSpeed.hasModifier(Util.guardSprintSpeedModifier))
//        {
//            attriSpeed.removeModifier(Util.guardSprintSpeedModifier);
//        }
//        if (attriKnock.hasModifier(Util.modifierGuardKnockback1))
//        {
//            attriKnock.removeModifier(Util.modifierGuardKnockback1);
//        }
//
//        //■満腹度が15以下なら走れない(Max 20)
//         boolean canChangeSpeed = player.getFoodStats().getFoodLevel() > 15 ? true : false;
//
//        //マルチとかしらんしらーん
//        if (canChangeSpeed
//            && Util.isGuard(player)
//            && Minecraft.getMinecraft().gameSettings.keyBindSprint.isKeyDown()
//            && Minecraft.getMinecraft().gameSettings.keyBindForward.isKeyDown()
//            && !Util.isBashing(player)
//            && Shield.proxy.getNumAttackTick() == 0)
//        {
//            //■「ガード時歩行速度上昇」 の付与
//            attriSpeed.applyModifier(Util.guardSprintSpeedModifier);
//            attriKnock.applyModifier(Util.modifierGuardKnockback1);
//        }
//    }

    /**
     * ■シールドチャージ中に接触したEntityをノックバック
     * @param event
     */
//    @SubscribeEvent
//    public void attackShieldCharge(LivingEvent.LivingUpdateEvent event)
//    {
//        if (!(event.getEntityLiving() instanceof EntityPlayer)) { return; }
//        EntityPlayer player = (EntityPlayer)event.getEntityLiving();
//
//        //■サーバーのみ
//        if (player.worldObj.isRemote) { return; }
//
//        //■シールドチャージ中
//        IAttributeInstance attri = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
//        if (!attri.hasModifier(Util.guardSprintSpeedModifier)) { return; }
//
//        List<Entity> entities = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, player.getEntityBoundingBox());
//
//        for (Entity target : entities)
//        {
//            if (target instanceof EntityLivingBase)
//            {
//                EntityLivingBase living = (EntityLivingBase)target;
//                living.knockBack(player, 0.5F * 3f, (double)MathHelper.sin(player.rotationYaw * 0.017453292F), (double)(-MathHelper.cos(player.rotationYaw * 0.017453292F)));
//                living.attackEntityFrom(DamageSource.causeMobDamage(player), 1);
//                //player.getActiveItemStack().damageItem(1, player);
//            }
//        }
//    }
}
