package yousui115.shield.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Timer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yousui115.shield.Shield;
import yousui115.shield.Util;
import yousui115.shield.network.MsgBash;
import yousui115.shield.network.MsgPowerBash;
import yousui115.shield.network.PacketHandler;

@SideOnly(Side.CLIENT)
public class EventBash
{
    /**
     * ■EntityLivingBase.onUpdate()の最初に呼ばれる。
     * @param event
     */
    @SubscribeEvent
    public void doBash(LivingEvent.LivingUpdateEvent event)
    {
        //■プレイヤーのみ処理を行う
        if (!(event.getEntityLiving() instanceof EntityPlayer)) { return; }
        EntityPlayer player = (EntityPlayer)(event.getEntityLiving());

        //▼ガード中である。(バッシュ中は入らない)
        if (Util.isGuard(player))
        {
            //■攻撃ボタン入力の保持
            Shield.proxy.addPushAttack();

            //▼攻撃ボタン押下->解放の瞬間
            if (Shield.proxy.isReleaseAttack())
            {
                //■腕を振る(バッシュ開始)
                player.swingArm(player.getActiveHand());

                //▼普通のバッシュは単体攻撃
                if (Shield.proxy.getPower() == 1)
                {
                    Entity target = Minecraft.getMinecraft().objectMouseOver.entityHit;
                    if (target instanceof EntityLivingBase)
                    {
                        PacketHandler.INSTANCE.sendToServer(new MsgBash(target, Shield.proxy.getPower(), 1));
                    }

                }
                //▼パワーバッシュは範囲攻撃(そして、草等のブロックを無視して攻撃が通る)
                else
                {
                    //■
                    float partialTicks = 1.0f;
                    Timer timer = (Timer)ObfuscationReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), 20);
                    partialTicks = timer.renderPartialTicks;

                    //■攻撃が届く範囲
                    double rangePlayer = (double)Minecraft.getMinecraft().playerController.getBlockReachDistance();

                    float offset = 35;
                    double range[] = { rangePlayer, rangePlayer, rangePlayer };

                    for (int idx = 0; idx < range.length; idx++)
                    {
                        //■基本レンジ
//                        rangePlayer = (double)Minecraft.getMinecraft().playerController.getBlockReachDistance();

                        //■レイトレース(Y軸回転)補正
                        float offsetYaw = (idx - 1) * offset;

                        //■レイトレース(草ブロック等は無視するように設定)
                        RayTraceResult resultBlock = Util.rayTrace(player, range[idx], 1.0F, offsetYaw);

                        //■視点
                        Vec3d posPlayerEye = player.getPositionEyes(partialTicks);

                        //■ブロックがあれば、ブロックまでの距離を取得
                        if (resultBlock != null && resultBlock.typeOfHit == RayTraceResult.Type.BLOCK)
                        {
                            range[idx] = resultBlock.hitVec.distanceTo(posPlayerEye);
                        }

                    }

                    //■パケットの送信
                    PacketHandler.INSTANCE.sendToServer(new MsgPowerBash(partialTicks, range, offset, Shield.proxy.getPower(), 1));

                }
            }
        }
        //▼バッシュ中、もしくは、ガードしていない
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
