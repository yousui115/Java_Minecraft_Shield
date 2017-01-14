package yousui115.shield.ai;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import yousui115.shield.Util;

public class EntityAIDonMov extends EntityAIBase
{
    public int tick = 20;
    public final EntityLiving myself;
    public final IAttributeInstance attri;

    public EntityAIDonMov(EntityLiving living)
    {
        myself = living;
        attri = myself.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        setMutexBits(~0x0);
    }

    @Override
    public boolean shouldExecute()
    {
        return tick > 0;
    }

    /**
     * ■中断可能か否か
     */
    @Override
    public boolean isInterruptible()
    {
        return false;
    }

    @Override
    public void startExecuting()
    {
        if (attri.hasModifier(Util.modifierDonmove))
        {
            attri.removeModifier(Util.modifierDonmove);
        }

        attri.applyModifier(Util.modifierDonmove);
    }

    @Override
    public void resetTask()
    {
        tick = 0;
        attri.removeModifier(Util.modifierDonmove);
    }

    /**
     * Updates the task
     */
    @Override
    public void updateTask()
    {
        --tick;
    }
}
