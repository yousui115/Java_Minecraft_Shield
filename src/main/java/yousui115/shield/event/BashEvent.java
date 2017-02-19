package yousui115.shield.event;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.eventhandler.Event;

public class BashEvent extends Event
{
    private final EntityLivingBase bashLiving;
    private final EntityLivingBase victimLiving;
    private final int power;

    public BashEvent(EntityLivingBase bashIn, EntityLivingBase victimIn, int powerIn)
    {
        bashLiving = bashIn;
        victimLiving = victimIn;
        power = powerIn;
    }

    public EntityLivingBase getBashLiving() { return bashLiving; }
    public EntityLivingBase getVictimLiving() { return victimLiving; }
    public int getPower() { return power; }
}
