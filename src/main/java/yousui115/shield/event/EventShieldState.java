package yousui115.shield.event;

import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.BannerPattern;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import yousui115.shield.Util;

public class EventShieldState
{
    /**
     * ■ブロック模様の盾はシールドブレイクを防ぐ
     * TODO:柄と武器の組み合わせをリストで持つと、柔軟に対応できるはず（今は斧のみだが）
     * @param event
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void doDisableShieldCancel(GuardMeleeEvent event)
    {
        //■ノーマルガードのみ処理する
        if (event.isJG == true) { return; }

        ItemStack shield = event.blocker.getActiveItemStack();
        if (Util.isEmptyStack(shield) == false && shield.getItem() instanceof ItemShield)
        {
            //■
            if (Util.hasPattern(shield, BannerPattern.BRICKS) == true)
            {
                event.canDisableShield = false;
            }
        }
    }
}
