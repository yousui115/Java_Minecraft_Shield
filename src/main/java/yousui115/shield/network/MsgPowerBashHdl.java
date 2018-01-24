package yousui115.shield.network;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import yousui115.shield.Util;
import yousui115.shield.event.BashEvent;
import yousui115.shield.event.ShieldHooks;

public class MsgPowerBashHdl implements IMessageHandler<MsgPowerBash, IMessage>
{
    /**
     * ■Client -> Server
     */
    @Override
    public IMessage onMessage(MsgPowerBash msg, MessageContext ctx)
    {
        //■サーバのプレイヤー
        EntityPlayer player = ctx.getServerHandler().player;
        if (player == null) { return null; }

        //■パワーバッシュ！
        boolean isHit = this.doPowerBash(player, msg.getTick(), msg.getRange(), msg.getOffsetYaw(), msg.getPower(), msg.getAmount());

        if (isHit)
        {
            //■盾にダメージ
            ItemStack active = player.getActiveItemStack();
            if (!Util.isEmptyStack(active))
            {
                player.getActiveItemStack().damageItem(msg.getPower(), player);

                //■パワーバッシュ音
                player.getEntityWorld().playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_BLAZE_HURT, player.getSoundCategory(), 1.0F, 1.0F);
            }
        }

        return null;
    }

    /**
     * ■
     */
    private boolean doPowerBash(EntityLivingBase attacker, float tickIn, double rangeIn[], float offsetIn, int powerIn, int amountIn)
    {
        boolean isSound = false;

        //■
        float partialTicks = tickIn;

        //■攻撃が届く範囲
        double rangeAttacker = 0;
        for (double d : rangeIn)
        {
            rangeAttacker = d > rangeAttacker ? d : rangeAttacker;
        }

        //■範囲内のEntityをかき集める。(どの方向を向いてても良い様にexpandXyz(range))
        List<Entity> entities = attacker.getEntityWorld().getEntitiesInAABBexcluding(
                                    attacker,
                                    attacker.getEntityBoundingBox().grow(rangeAttacker),
                                    new Predicate<Entity>()
                                    {
                                        public boolean apply(@Nullable Entity target)
                                        {
                                            return target != null && target.canBeCollidedWith();
                                        }
                                    });
//                                    }));

        //■レイトレースを(rangeIn.length)本に増やす。(差はoffset度)
        float offset = offsetIn;
        for (int idx = 0; idx < rangeIn.length; idx++)
        {
            //■基本レンジ
            rangeAttacker = rangeIn[idx];

            //■レイトレース(Y軸回転)補正
            float offsetYaw = (idx - (rangeIn.length / 2)) * offset;

            //■視点
            Vec3d posAttackerEye = attacker.getPositionEyes(partialTicks);

            //■レンジ(クロスヘア上のレンジ長最大点)
            Vec3d posAttackerRange = posAttackerEye.add(Util.getLook(attacker, 1.0F, offsetYaw).scale(rangeAttacker));

            //■前にかき集めたEntityリストから対象をピックアップする
            Iterator<Entity> itr = entities.iterator();
            while(itr.hasNext())
            {
                //■リストからEntityを取得
                //TODO:被害を受けるかどうかはまだ判らないからsuspect?
                Entity victim = itr.next();

                //■Entityの当たり判定を拡張
                double expand = 0.5;//(double)entity1.getCollisionBorderSize();
//                AxisAlignedBB aabbVictim = victim.getEntityBoundingBox().expandXyz(expand);
                AxisAlignedBB aabbVictim = victim.getEntityBoundingBox().grow(expand);

                //■視線と上記当たり判定が交差するか否か(intercept = 遮る)
                RayTraceResult resultVictim = aabbVictim.calculateIntercept(posAttackerEye, posAttackerRange);

                //■バッシュが当たるならtrue
                boolean isHit = false;

                //▼被害者の中に居る(ボートの同乗者はこっち)
//                if (aabbVictim.isVecInside(posAttackerEye))
                if (aabbVictim.contains(posAttackerEye))
                {
                    if (!victim.isRidingSameEntity(attacker) || attacker.canRiderInteract())
                    {
                        isHit = true;
                    }
                }
                //▼視線上に居る
                else if (resultVictim != null)
                {
                    //■プレイヤーと被害者との距離
                    double distancePV = posAttackerEye.distanceTo(resultVictim.hitVec);

                    //■距離が近い
                    if (distancePV < 3.0D)
                    {
                        //▼victimとplayerの最下層乗り物Entityが違う or なんやら
                        //  (乗って無いなら自分自身が帰る)
                        //  (例えば、victimが馬で、playerがその馬に乗ってるなら、馬=馬でtrueが帰る)
                        if (!victim.isRidingSameEntity(attacker) || attacker.canRiderInteract())
                        {
                            isHit = true;
                        }
                    }
                }

                //■パワーバッシュがヒットしている。
                if (isHit)
                {
                    //■イベント
                    BashEvent event = ShieldHooks.onBashAttack(attacker, victim, powerIn, amountIn);
                    if (Util.isEventCanceled(event)) { continue; }

                    //■パラメータの書き換え
                    amountIn = event.amount;
                    powerIn  = event.power;

                    //■一体にでも攻撃が当たればSEが鳴る
                    isSound = true;

                    //■ダメージソースとダメージの設定
                    if (attacker instanceof EntityPlayer)
                    {
                        victim.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer)attacker), amountIn);
                    }
                    else
                    {
                        victim.attackEntityFrom(DamageSource.causeMobDamage(attacker), amountIn);
                    }

                    //■ノックバックの設定
                    if (victim instanceof EntityLivingBase)
                    {
                        ((EntityLiving)victim).knockBack(attacker, 0.4f,
                                (double)MathHelper.sin(attacker.rotationYaw * 0.017453292F),
                                (double)(-MathHelper.cos(attacker.rotationYaw * 0.017453292F)));

                        //■行動不能(AI)の設定
                        if (victim instanceof EntityLiving)
                        {
                            Util.tameAIDonmov((EntityLiving)victim, powerIn);
                        }
                        else if (victim instanceof EntityPlayer)
                        {
                            //TODO
                        }
                    }

                    //■リストから削除
                    itr.remove();

                } //if (isHit)

            } //while (itr.hasNext())
        } //for (rangeIn[])

        return isSound;
    }
}
