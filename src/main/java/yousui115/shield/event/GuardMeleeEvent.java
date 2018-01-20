package yousui115.shield.event;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.common.eventhandler.Event;

public class GuardMeleeEvent extends Event
{
    public final EntityLivingBase blocker;
    public final EntityLivingBase attacker;
    public final boolean isJG;
    public final DamageSource source;
    public final float amount;

    public GuardMeleeEvent(EntityLivingBase blockerIn, EntityLivingBase attackerIn, boolean isJGIn, DamageSource sourceIn, float amountIn)
    {
        blocker = blockerIn;
        attacker = attackerIn;
        isJG = isJGIn;
        source = sourceIn;
        amount = amountIn;
    }
}
