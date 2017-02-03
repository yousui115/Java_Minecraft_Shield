package yousui115.shield.client.event;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.Timer;
import net.minecraft.util.math.AxisAlignedBB;
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

                    //■攻撃が届く範囲
                    double rangePlayer = (double)Minecraft.getMinecraft().playerController.getBlockReachDistance();

                    //■TODO レンジを送り、サーバ側でかき集めて一括処理するべきか。悩ましい。
                    //■範囲内のEntityをかき集める。(どの方向を向いてても良い様にexpandXyz(range))
                    List<Entity> entities = player.worldObj.getEntitiesInAABBexcluding(
                                                player,
                                                player.getEntityBoundingBox().expandXyz(rangePlayer),
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
                        //■基本レンジ
                        rangePlayer = (double)Minecraft.getMinecraft().playerController.getBlockReachDistance();

                        //■レイトレース(Y軸回転)補正
                        float offsetYaw = (idx - 1) * offset;

                        //■レイトレース(草ブロック等は無視するように設定)
                        RayTraceResult resultBlock = Util.rayTrace(player, rangePlayer, 1.0F, offsetYaw);

                        //■視点
                        Vec3d posPlayerEye = player.getPositionEyes(partialTicks);

                        //■ブロックがあれば、ブロックまでの距離を取得
                        if (resultBlock != null && resultBlock.typeOfHit == RayTraceResult.Type.BLOCK)
                        {
                            rangePlayer = resultBlock.hitVec.distanceTo(posPlayerEye);
                        }

                        //■レンジ(クロスヘア上のレンジ長最大点)
//                        Vec3d posPlayerRange = getLook(player, 1.0F, offsetYaw).scale(rangePlayer);
                        Vec3d posPlayerRange = posPlayerEye.add(Util.getLook(player, 1.0F, offsetYaw).scale(rangePlayer));

//                        Vec3d vec3d1 = entity.getLook(1.0F);
//                        Vec3d vec3d2 = vec3d.addVector(vec3d1.xCoord * d0, vec3d1.yCoord * d0, vec3d1.zCoord * d0);

                        //■最寄のEntityまでの距離
                        double rangeNear = rangePlayer;
                        //■最寄のEntity
                        Entity target = null;
                        //■最寄のEntityの位置情報
                        Vec3d posTarget = null;

                        //■前にかき集めたEntityリストから対象をピックアップする
                        for (int j = 0; j < entities.size(); ++j)
                        {
                            //■リストからEntityを取得
                            //TODO:被害を受けるかどうかはまだ判らないからsuspect?
                            Entity victim = (Entity)entities.get(j);    //victim = 被害者(バッシュの)

                            //■Entityの当たり判定を拡張
                            double expand = 0.5;//(double)entity1.getCollisionBorderSize();
                            AxisAlignedBB aabbVictim = victim.getEntityBoundingBox().expandXyz(expand);

                            //■視線と上記当たり判定が交差するか否か(intercept = 遮る)
                            RayTraceResult resultVictim = aabbVictim.calculateIntercept(posPlayerEye, posPlayerRange);

                            //▼被害者の中に居る
                            if (aabbVictim.isVecInside(posPlayerEye))
                            {
                                //■プレイヤーと密着してる奴が最寄
                                if (rangeNear >= 0.0D)
                                {
                                    target = victim;
                                    posTarget = resultVictim == null ? posPlayerEye : resultVictim.hitVec;
                                    rangeNear = 0.0D;
                                }
                            }
                            //▼視線上に居る
                            else if (resultVictim != null)
                            {
                                //■プレイヤーと被害者との距離
                                double distancePV = posPlayerEye.distanceTo(resultVictim.hitVec);

                                //■「これまでの最寄Entityより近い」 もしくは 「最寄Entityとプレイヤーが密着」
                                //TODO:プレイヤーと密着してるEntityの処理後、クロスヘア上のEntityの処理を行うと入ってしまうのでは？
                                //     v4pre:独自で修正
//                                if (distancePV < rangeNear || rangeNear == 0.0D)

                                //■「これまでの最寄Entityより近い」
                                if (distancePV < rangeNear)
                                {
                                    //▼victimとplayerの最下層乗り物Entityが同一
                                    //  (乗って無いなら自分自身が帰る)
                                    //  (例えば、victimが馬で、playerがその馬に乗ってるなら、馬=馬でtrueが帰る)
                                    if (victim.isRidingSameEntity(player) && !player.canRiderInteract())
                                    {
                                        //■乗ってる馬やボートにバッシュ当てたくないので。
//                                        if (rangeNear == 0.0D)
//                                        {
//                                            target = victim;
//                                            posTarget = resultVictim.hitVec;
//                                        }
                                    }
                                    else
                                    {
                                        target = victim;
                                        posTarget = resultVictim.hitVec;
                                        rangeNear = distancePV;
                                    }
                                }
                            }
                        }

//                        RayTraceResult objectMouseOver = null;
                        if (target != null && posPlayerEye.distanceTo(posTarget) > 3.0D)
                        {
                            target = null;
//                            objectMouseOver = new RayTraceResult(RayTraceResult.Type.MISS, posTarget, (EnumFacing)null, new BlockPos(posTarget));
                        }

                        if (target != null) { entities.remove(target); }


//                        if (target != null && (rangeNear < rangePlayer || objectMouseOver == null))
//                        {
//                            objectMouseOver = new RayTraceResult(target, posTarget);

//                            if (pointedEntity instanceof EntityLivingBase || pointedEntity instanceof EntityItemFrame)
//                            {
//                                this.mc.pointedEntity = this.pointedEntity;
//                            }
//                        }







                        //■パケット送信
                        if (target != null)
                        {
                            PacketHandler.INSTANCE.sendToServer(new MsgPowerBash(rangePlayer, Shield.proxy.getPower(), 1, target.getEntityId()));
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
     * ■バッシュアニメーション
     * @param event
     */
    @SubscribeEvent
    public void renderSwingShield(RenderSpecificHandEvent event)
    {
        //Minecraft.getMinecraft().getItemRenderer().
        EntityPlayer player = Shield.proxy.getThePlayer();
        if (player == null) { return; }
//        IAttributeInstance attri = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);

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
