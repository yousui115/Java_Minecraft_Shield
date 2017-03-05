package yousui115.shield.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.eventhandler.Event;

public class BashEvent extends Event
{
    public final EntityLivingBase basher;
    public final Entity victim;
    public int power;
    public int amount;

    public BashEvent(EntityLivingBase basherIn, Entity victimIn, int powerIn, int amountIn)
    {
        basher = basherIn;
        victim = victimIn;
        power = powerIn;
        amount = amountIn;
    }

    @Override
    public boolean isCancelable() { return true; }
}
