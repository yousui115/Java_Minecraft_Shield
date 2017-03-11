package yousui115.shield.event;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.common.eventhandler.Event;

public class GuardEvent  extends Event
{
    public EntityLivingBase blocker;
    public boolean isJG;
    public DamageSource source;
    public float amount;

    public GuardEvent(EntityLivingBase blockerIn, boolean isJGIn, DamageSource sourceIn, float amountIn)
    {
        blocker = blockerIn;
        isJG = isJGIn;
        source = sourceIn;
        amount = amountIn;
    }
}
