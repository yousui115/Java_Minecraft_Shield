package yousui115.shield.ai;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.MathHelper;

public class EntityAIAnger extends EntityAIBase
{
    public final EntityLiving myself;

    private int tickMax = 300;
    private int tick = tickMax;
    private EntityLivingBase target;

    public EntityAIAnger(EntityLiving myselfIn, EntityLivingBase targetIn)
    {
        myself = myselfIn;
        target = targetIn;
        setMutexBits(1);
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

    }

    @Override
    public void resetTask()
    {
        tick = 0;
    }

    /**
     * Updates the task
     */
    @Override
    public void updateTask()
    {
        tick = MathHelper.clamp(--tick, 0, tickMax);

        myself.setAttackTarget(target);
    }

    public void setTarget(EntityLivingBase targetIn)
    {
        if (tick == 0)
        {
            target = targetIn;
            tick = tickMax;
        }
    }
}
