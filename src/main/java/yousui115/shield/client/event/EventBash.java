package yousui115.shield.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
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
                    double range = (double)Minecraft.getMinecraft().playerController.getBlockReachDistance();
                    float tick = 1.0f;
                    Timer timer = (Timer)ObfuscationReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), 20);
                    tick = timer.renderPartialTicks;

//                    RayTraceResult result = player.rayTrace(range, 1.0f);
                    Vec3d posPlyEye = player.getPositionEyes(1.0f);
                    Vec3d vecPlyLook = player.getLook(1.0f);
                    Vec3d vecPlyRange = posPlyEye.addVector(vecPlyLook.xCoord * range, vecPlyLook.yCoord * range, vecPlyLook.zCoord * range);
                    RayTraceResult result = player.worldObj.rayTraceBlocks(posPlyEye, vecPlyRange, false, true, true);


                    if (result != null && result.typeOfHit == RayTraceResult.Type.BLOCK)
                    {
                        range = result.hitVec.distanceTo(player.getPositionEyes(tick));
                    }
                    PacketHandler.INSTANCE.sendToServer(new MsgPowerBash(range, Shield.proxy.getPower(), 1));
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
        IAttributeInstance attri = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);

//        if (player.getActiveHand() == event.getHand()
//            && attri.hasModifier(Util.guardSprintSpeedModifier))
//        {
//            //■push
//            GlStateManager.pushMatrix();
//
//            //float fcos = MathHelper.abs(event.getSwingProgress() - 0.5f);
//            GlStateManager.translate(0.3f, 0.3f, 0f);
//            Minecraft.getMinecraft().getItemRenderer().renderItemInFirstPerson((AbstractClientPlayer)player, event.getPartialTicks(), event.getInterpolatedPitch(), event.getHand(), event.getSwingProgress(), event.getItemStack(), event.getEquipProgress());
//
//            //■pop
//            GlStateManager.popMatrix();
//
//            event.setCanceled(true);
//
//        }
//        else
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
