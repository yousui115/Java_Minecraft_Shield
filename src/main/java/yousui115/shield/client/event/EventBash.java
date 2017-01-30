package yousui115.shield.client.event;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Timer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

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
                    //TODO 良い感じに動作してるけど、もっとスマートに組めるはず。要仕様見直し+リファクタリング

                    //■
                    float partialTicks = 1.0f;
                    Timer timer = (Timer)ObfuscationReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), 20);
                    partialTicks = timer.renderPartialTicks;

                    //■クロスヘア上のレンジ
                    double range = (double)Minecraft.getMinecraft().playerController.getBlockReachDistance();

                    //■TODO レンジ長を送り、サーバ側でかき集めて一括処理するべきか。悩ましい。
                    //■レンジ範囲内のEntityをかき集める。
                    List<Entity> entities = player.worldObj.getEntitiesInAABBexcluding(
                                                player,
                                                player.getEntityBoundingBox().expand(range, range, range),
                                                Predicates.and(EntitySelectors.NOT_SPECTATING,
                                                new Predicate<Entity>()
                                                {
                                                    public boolean apply(@Nullable Entity target)
                                                    {
                                                        return target != null && target.canBeCollidedWith();
                                                    }
                                                }));

                    //■レイトレースを3本に増やす。(差はoffset度)
                    float offset = 35;
                    for (int idx = 0; idx < 3; idx++)
                    {
                        //■レイトレース(Y軸回転)補正
                        float offsetYaw = (idx - 1) * offset;

                        //■レイトレース(草ブロック等は無視するように設定)
                        RayTraceResult result = rayTrace(player, range, 1.0F, offsetYaw);

                        //■視点
                        Vec3d posEye = player.getPositionEyes(partialTicks);

                        //■ブロックがあれば、ブロックまでの距離を取得
                        if (result != null && result.typeOfHit == RayTraceResult.Type.BLOCK)
                        {
                            range = result.hitVec.distanceTo(posEye);
                        }

                        //■視線
                        Vec3d vecLook = getLook(player, 1.0F, offsetYaw);
                        //■視線(レンジ)
                        Vec3d vecRange = posEye.addVector(vecLook.xCoord * range, vecLook.yCoord * range, vecLook.zCoord * range);


                        double d2 = range;
                        Entity pointedEntity = null;
                        Vec3d vec3d3 = null;
                        for (int j = 0; j < entities.size(); ++j)
                        {
                            Entity entity1 = (Entity)entities.get(j);
                            double expand = 0.5;//(double)entity1.getCollisionBorderSize();
                            AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expandXyz(expand);
                            RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(posEye, vecRange);

                            if (axisalignedbb.isVecInside(posEye))
                            {
                                if (d2 >= 0.0D)
                                {
                                    pointedEntity = entity1;
                                    vec3d3 = raytraceresult == null ? posEye : raytraceresult.hitVec;
                                    d2 = 0.0D;
                                }
                            }
                            else if (raytraceresult != null)
                            {
                                double d3 = posEye.distanceTo(raytraceresult.hitVec);

                                if (d3 < d2 || d2 == 0.0D)
                                {
                                    if (entity1.getLowestRidingEntity() == player.getLowestRidingEntity() && !player.canRiderInteract())
                                    {
                                        if (d2 == 0.0D)
                                        {
                                            pointedEntity = entity1;
                                            vec3d3 = raytraceresult.hitVec;
                                        }
                                    }
                                    else
                                    {
                                        pointedEntity = entity1;
                                        vec3d3 = raytraceresult.hitVec;
                                        d2 = d3;
                                    }
                                }
                            }
                        }

                        RayTraceResult objectMouseOver = null;
                        if (pointedEntity != null
//                            && flag
                            && posEye.distanceTo(vec3d3) > 3.0D)
                        {
                            pointedEntity = null;
                            objectMouseOver = new RayTraceResult(RayTraceResult.Type.MISS, vec3d3, (EnumFacing)null, new BlockPos(vec3d3));
                        }

                        if (pointedEntity != null && (d2 < range || objectMouseOver == null))
                        {
                            objectMouseOver = new RayTraceResult(pointedEntity, vec3d3);

//                            if (pointedEntity instanceof EntityLivingBase || pointedEntity instanceof EntityItemFrame)
//                            {
//                                this.mc.pointedEntity = this.pointedEntity;
//                            }
                        }







                        //■パケット送信
                        if (pointedEntity != null)
                        {
                            PacketHandler.INSTANCE.sendToServer(new MsgPowerBash(range, Shield.proxy.getPower(), 1, pointedEntity.getEntityId()));
                        }
                    }
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
     * ■Entity.rayTrace参考
     * @param living
     * @param partialTick
     * @return
     */
    @Nullable
    private static RayTraceResult rayTrace(EntityLivingBase living, double range, float partialTicks, float offsetYaw)
    {
        //■プレイヤー位置
        Vec3d posLivEye = living.getPositionEyes(partialTicks);
        //■プレイヤー視線
        Vec3d vecLivLook = getLook(living, partialTicks, offsetYaw);
        //■プレイヤー視線(レンジ)
        Vec3d vecLivRange = posLivEye.addVector(vecLivLook.xCoord * range, vecLivLook.yCoord * range, vecLivLook.zCoord * range);
        //■草とかに攻撃が吸われないように。
        return living.worldObj.rayTraceBlocks(posLivEye, vecLivRange, false, true, true);
    }

    /**
     * ■EntityLivingBaseをパｋ参考。
     * @param living
     * @param partialTicks
     * @return
     */
    protected static Vec3d getLook(EntityLivingBase living, float partialTicks, float offsetYaw)
    {
        if (partialTicks == 1.0F)
        {
            return getVectorForRotation(living.rotationPitch, living.rotationYawHead + offsetYaw);
        }
        else
        {
            float f = living.prevRotationPitch + (living.rotationPitch - living.prevRotationPitch) * partialTicks;
            float f1 = living.prevRotationYawHead + (living.rotationYawHead - living.prevRotationYawHead) * partialTicks;
            return getVectorForRotation(f, f1 + offsetYaw);
        }
    }

    protected static final Vec3d getVectorForRotation(float pitch, float yaw)
    {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3d((double)(f1 * f2), (double)f3, (double)(f * f2));
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
