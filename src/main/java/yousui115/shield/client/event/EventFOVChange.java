package yousui115.shield.client.event;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yousui115.shield.Shield;
import yousui115.shield.Util;

@SideOnly(Side.CLIENT)
public class EventFOVChange
{
    /**
     * ■ガード歩行時のFOVの設定
     *   ModifiableAttributeInstance.computeValue(), AbstractClientPlayer.getFovModifier()をパｋ参考に実装。
     *   (違和感の無いFOVを心がけようとした結果、こんな実装に。てか、FOV値を直接弄られると競合するが知らん。)
     * @param event
     */
    @SubscribeEvent
    public void walkBlockingFOV(FOVUpdateEvent event)
    {
        if(event.getEntity() == null) return;
        IAttributeInstance attri = event.getEntity().getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);

        float f = event.getNewfov();

        if (attri != null && attri.hasModifier(Util.modifierGuardWalkSpeed))
        {
            double d0 = attri.getBaseValue();

            for (AttributeModifier modi0 : attri.getModifiersByOperation(0))
            {
                if (!modi0.equals(Util.modifierGuardWalkSpeed))
                {
                    d0 += modi0.getAmount();
                }
            }

            double d1 = d0;

            for (AttributeModifier modi1 : attri.getModifiersByOperation(1))
            {
                d1 += d0 * modi1.getAmount();
            }

            for (AttributeModifier modi2 : attri.getModifiersByOperation(2))
            {
                d1 *= 1.0D + modi2.getAmount();
            }

            f = 1.0f;
            if (event.getEntity().capabilities.isFlying)
            {
                f *= 1.1F;
            }

            f = (float)((double)f * ((d1 / (double)event.getEntity().capabilities.getWalkSpeed() + 1.0D) / 2.0D));

            if (event.getEntity().capabilities.getWalkSpeed() == 0.0F || Float.isNaN(f) || Float.isInfinite(f))
            {
                f = 1.0F;
            }
        }

        //■パワーバッシュ溜め時のFOVの設定
        int border = Shield.proxy.getNumAttackLength() / 2;
        int tick = MathHelper.clamp(Shield.proxy.getNumAttackTick() - border, 0, border);
        event.setNewfov(f - (0.02f * (float)tick));

    }

}
