package yousui115.shield.event;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.common.eventhandler.Event;

public class GuardMeleeEvent extends Event
{
    public final EntityLivingBase blocker;
    public final EntityLivingBase attacker;
    public boolean isJG;
    public final DamageSource source;
    public float amount;
    public boolean canDisableShield;

    public GuardMeleeEvent(EntityLivingBase blockerIn, EntityLivingBase attackerIn, boolean isJGIn, DamageSource sourceIn, float amountIn, boolean canDisableShieldIn)
    {
        blocker = blockerIn;
        attacker = attackerIn;
        isJG = isJGIn;
        source = sourceIn;
        amount = amountIn;
        canDisableShield = canDisableShieldIn;
    }
}
