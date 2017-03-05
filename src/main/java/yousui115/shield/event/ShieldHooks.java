package yousui115.shield.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;

public class ShieldHooks
{
    public static BashEvent onBashAttack(EntityLivingBase attackerIn, Entity victimIn, int powerIn, int amountIn)
    {
        BashEvent event = new BashEvent(attackerIn, victimIn, powerIn, amountIn);
        MinecraftForge.EVENT_BUS.post(event);
        return event;
    }

    public static GuardEvent onGuard(EntityLivingBase blockerIn, boolean isJGIn, boolean isGuardIn, DamageSource sourceIn, float amountIn)
    {
        GuardEvent event = new GuardEvent(blockerIn, isJGIn, isGuardIn, sourceIn, amountIn);
        MinecraftForge.EVENT_BUS.post(event);
        return event;
    }

    public static void onGuardMelee(EntityLivingBase blockerIn, EntityLivingBase attackerIn, boolean isJGIn, DamageSource sourceIn, float amountIn)
    {
        GuardMeleeEvent event = new GuardMeleeEvent(blockerIn, attackerIn, isJGIn, sourceIn, amountIn);
        MinecraftForge.EVENT_BUS.post(event);
    }
}
