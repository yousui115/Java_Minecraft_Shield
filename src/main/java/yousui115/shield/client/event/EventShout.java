package yousui115.shield.client.event;

import net.minecraft.item.ItemShield;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yousui115.shield.Shield;
import yousui115.shield.network.MsgShout;
import yousui115.shield.network.PacketHandler;

@SideOnly(Side.CLIENT)
public class EventShout
{
    @SubscribeEvent
    public void doShout(TickEvent.PlayerTickEvent event)
    {
        if (event.side != Side.CLIENT ||
            event.phase != Phase.START)
        { return; }

        //■入力情報の取得
        Shield.proxy.stackInputShout();

        //■「盾をオフハンドに持っている」かつ「アクション中ではない」かつ「挑発ボタン押下」時に挑発する
        if (event.player.getHeldItemOffhand().getItem() instanceof ItemShield &&
            event.player.isHandActive() == false &&
            event.player.isSwingInProgress == false &&
            Shield.proxy.isKeyPushShout() == true)
        {
            //■パケットの送信
            PacketHandler.INSTANCE.sendToServer(new MsgShout());

            event.player.swingArm(EnumHand.MAIN_HAND);
        }
    }
}
