package yousui115.shield.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yousui115.shield.Shield;
import yousui115.shield.Util;
import yousui115.shield.network.MessageBashTarget;
import yousui115.shield.network.PacketHandler;

@SideOnly(Side.CLIENT)
public class EventShieldBash
{
    /**
     * ■EntityLivingBase.onUpdate()の最初に呼ばれる。
     * @param event
     */
    @SubscribeEvent
    public void doShieldBash(LivingEvent.LivingUpdateEvent event)
    {
        //■プレイヤーのみ処理を行う
        if (!(event.getEntityLiving() instanceof EntityPlayer)) { return; }
        EntityPlayer player = (EntityPlayer)(event.getEntityLiving());

        //▼1.バッシュ中である。
        if (Util.isBashing(player))
        {
            //■バッシュ中は攻撃ボタンの入力を無視+リセット
            Shield.proxy.resetAttack();
        }
        //▼2.ガード中である。(Not バッシュ)
        else if (player.isActiveItemStackBlocking())
        {
            //■攻撃ボタン入力の保持
            boolean isPushAttack = Minecraft.getMinecraft().gameSettings.keyBindAttack.isKeyDown();
            Shield.proxy.addPushAttack(isPushAttack);

            //▼攻撃ボタン押下->解放の瞬間
            if (Shield.proxy.isReleaseAttack())
            {
                //■腕を振る(バッシュ開始)
                player.swingArm(player.getActiveHand());

                Entity target = Minecraft.getMinecraft().objectMouseOver.entityHit;
                if (target instanceof EntityLivingBase)
                {
                    PacketHandler.INSTANCE.sendToServer(new MessageBashTarget(Minecraft.getMinecraft().objectMouseOver.entityHit, Shield.proxy.getPower(), 1));
                }
            }
        }
        //▼3.バッシュもガードもしていない。
        else
        {
            Shield.proxy.resetAttack();
        }
    }

    /**
     * ■バッシュアニメーション
     * @param event
     */
    @SubscribeEvent
    public void renderSwingShield(RenderSpecificHandEvent event)
    {
        //Minecraft.getMinecraft().getItemRenderer().
        EntityPlayer player = Shield.proxy.getThePlayer();
        if (player == null) { return; }

        if (player.getActiveHand() == event.getHand()
//            && player.isActiveItemStackBlocking()
//            && event.getSwingProgress() > 0.0001f)
              && Util.isBashing(player))
        {
            //■push
            GlStateManager.pushMatrix();

            float fcos = MathHelper.abs(event.getSwingProgress() - 0.5f);
            GlStateManager.translate(0f, 0f, fcos - 0.5f);
            Minecraft.getMinecraft().getItemRenderer().renderItemInFirstPerson((AbstractClientPlayer)player, event.getPartialTicks(), event.getInterpolatedPitch(), event.getHand(), event.getSwingProgress(), event.getItemStack(), event.getEquipProgress());

            //■pop
            GlStateManager.popMatrix();

            event.setCanceled(true);
        }
    }
}
