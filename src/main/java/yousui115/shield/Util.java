package yousui115.shield;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.Event;
import scala.util.Random;
import yousui115.shield.ai.EntityAIAnger;
import yousui115.shield.ai.EntityAIDonMov;

//TODO 継ぎ接ぎだらけなので、余力があったらリファクタリングしましょう。

public class Util
{
    //■「ガード時歩行速度上昇」パラメータ
    public static final UUID UUID_guardWalkSpeed  = UUID.fromString("9be6f9f0-c286-5fb1-974e-baec497a8033");
    public static final AttributeModifier modifierGuardWalkSpeed = (new AttributeModifier(UUID_guardWalkSpeed, "Guard walk speed", 0.3d, 0)).setSaved(false);

    //■「被バッシュ後の移動不可」パラメータ
    public static final UUID UUID_donmov  = UUID.fromString("6fd1ce57-8e37-504d-f859-6262b644ef19");
    public static final AttributeModifier modifierDonmove = (new AttributeModifier(UUID_donmov, "donmov", -1.0d, 2)).setSaved(false);

    public static final Random rand = new Random();

    //■「ガード時スプリント」パラメータ
//    public static final UUID UUID_guardSprintSpeed  = UUID.fromString("9be6f9f0-c286-5fb1-974e-baec48888888");
//    public static final AttributeModifier guardSprintSpeedModifier = (new AttributeModifier(UUID_guardSprintSpeed, "Guard sprint speed", 0.4d, 2)).setSaved(false);

    //■「ノックバック耐性上昇」パラメータ
//    public static final UUID UUID_GuardKnockback1 = UUID.fromString("85a28cfd-b83a-6877-28bd-5026c8666666");//TODO:このハッシュ値、適当だけどいいのかにゃー？
//    public static final AttributeModifier modifierGuardKnockback1 = (new AttributeModifier(UUID_GuardKnockback1, "Guard Knockback Resistance 1", 1.0d, 0)).setSaved(false);

    /**
     * ■ガード中 か否か(0Tick～7200Tick)
     * @param living
     * @return
     */
    public static boolean isGuard(EntityLivingBase living)
    {
        //■現在使ってるアイテムを取得
        ItemStack activeStack = living.getActiveItemStack();

//        if (activeStack != null && living.isHandActive())
        if (!Util.isEmptyStack(activeStack) && living.isHandActive())
        {
            //■アイテムを持っていて、使用中である。
            Item activeItem = activeStack.getItem();

            if (!Util.isBashing(living) && activeItem.getItemUseAction(activeStack) == EnumAction.BLOCK)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * ■ジャストガード中 か否か(0Tick～4Tick)
     * @param living
     * @return
     */
    public static boolean isJustGuard(EntityLivingBase living)
    {
        boolean isJG = false;

        //■現在使ってるアイテムを取得
        ItemStack activeStack = living.getActiveItemStack();

        if (Util.isGuard(living))
        {
            //■右クリックを押した瞬間～4tickまでは「ジャストガード」
            isJG = activeStack.getItem().getMaxItemUseDuration(activeStack) - living.getItemInUseCount() < 5;
        }

        return isJG;
    }

    /**
     * ■バッシュ中か否か
     */
    public static boolean isBashing(EntityLivingBase living)
    {
        if (living != null
            && living.isActiveItemStackBlocking()
            && living.isSwingInProgress)
        {
            return true;
        }

        return false;
    }

    /**
     * ■ダメージソース がガード可能か否か (ブロック不可・ガード状態・ガード方向の調査)
     *   (ノックバック耐性の剥奪も処理している)
     *   (EntityLivingBase.canBlockDamageSource() をパｋ参考にしました)
     * @param damageSourceIn
     * @param blocker
     * @return
     */
    public static boolean canBlockDamageSource(DamageSource damageSourceIn, EntityLivingBase blocker, Vec3d posExplosion)
    {
        //■ガード可能 かつ ガード状態
        if (!damageSourceIn.isUnblockable() && Util.isGuard(blocker))
        {
            //■ダメージ発生源（位置）
            Vec3d posDS = damageSourceIn.getDamageLocation();

            //■TNTの爆発をガード可能にする処理
            if (posExplosion != null) { posDS = posExplosion; }

            //■ダメージ発生源が存在するなら、ガード判定へ洒落込む
            if (posDS != null)
            {
                //■ブロッカーの視線（ベクトル）
                Vec3d blockerLook = blocker.getLook(1.0F);
                //■アタッカーの視線
                Vec3d attackerLook = posDS.subtractReverse(blocker.getPositionVector()).normalize();

                //■まずは普通のガード判定を試みる。
                attackerLook = new Vec3d(attackerLook.x, 0.0D, attackerLook.z);

                //内積を使って、ガード範囲(180度)内か否かの算出を行う。
                if (attackerLook.dotProduct(blockerLook) < 0.0D)
                {
                    return true;
                }
                else
                {
                    //■黄昏のナーガのような、プレイヤーを突き抜けながら
                    //  ダメージを発生させるDSへの対処
                    if (!(damageSourceIn.getTrueSource() instanceof EntityLivingBase)) { return false; }

                    attackerLook = new Vec3d(damageSourceIn.getTrueSource().getLookVec().x,
                                             0.0D,
                                             damageSourceIn.getTrueSource().getLookVec().z);

                    //内積を使って、ガード範囲(180度)内か否かの算出を行う。
                    if (attackerLook.dotProduct(blockerLook) < 0.0D)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * ■シールドのダメージ処理
     * @param living
     * @param damage
     */
    public static void damageShield(EntityLivingBase living, float damage)
    {
        //■サーバのみ
        if (living.getEntityWorld().isRemote) { return; }

        if (damage >= 3.0F && living.getActiveItemStack().getItem().isShield(living.getActiveItemStack(), living))
        {
            ItemStack copyBeforeUse = living.getActiveItemStack().copy();

            //■オリジナルねーむ が付いていれば、1/10の確率でダメージかっと
            if (copyBeforeUse.hasDisplayName() == true && living.getEntityWorld().rand.nextInt(10) == 1)
            {
                return;
            }

            int i = 1 + MathHelper.floor(damage);
            living.getActiveItemStack().damageItem(i, living);

            if (living.getActiveItemStack().isEmpty())
            {
                EnumHand enumhand = living.getActiveHand();

                if (living instanceof EntityPlayer)
                {
                    net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem((EntityPlayer)living, copyBeforeUse, enumhand);
                }

                if (enumhand == EnumHand.MAIN_HAND)
                {
                    living.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }
                else
                {
                    living.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, ItemStack.EMPTY);
                }

                living.resetActiveHand();
                living.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.8F, 0.8F + living.world.rand.nextFloat() * 0.4F);
            }
        }
    }

    /**
     * ■行動不能AIの植え付け
     * @param target
     * @param power
     */
    public static void tameAIDonmov(EntityLiving target, int power)
    {
        boolean isLearning = false;
        int tick = power * 20;

        for (EntityAITasks.EntityAITaskEntry entry : target.tasks.taskEntries)
        {
            if (entry.action instanceof EntityAIDonMov)
            {
                EntityAIDonMov ai = (EntityAIDonMov)entry.action;
                ai.tick = tick;
                isLearning = true;
                break;
            }
        }
        if (!isLearning)
        {
            EntityAIDonMov ai = new EntityAIDonMov(target);
            ai.tick = tick;
            target.tasks.addTask(0, ai);
        }
    }

    /**
     * ■やんやん？
     * @param target
     * @param power
     */
    public static void tameAIAnger(EntityLiving anger, EntityLivingBase target)
    {
        boolean isLearning = false;

        for (EntityAITasks.EntityAITaskEntry entry : anger.targetTasks.taskEntries)
        {
            if (entry.action instanceof EntityAIAnger)
            {
                EntityAIAnger ai = (EntityAIAnger)entry.action;
                ai.setTarget(target);
                isLearning = true;
                break;
            }
        }
        if (!isLearning)
        {
            EntityAIAnger ai = new EntityAIAnger(anger, target);
            ai.setTarget(target);
            anger.targetTasks.addTask(0, ai);
        }
    }

    /**
     * ■Entity.rayTrace参考
     * @param living
     * @param partialTick
     * @return
     */
    @Nullable
    public static RayTraceResult rayTrace(EntityLivingBase living, double range, float partialTicks, float offsetYaw)
    {
        //■プレイヤー位置
        Vec3d posLivEye = living.getPositionEyes(partialTicks);
        //■プレイヤー視線
        Vec3d vecLivLook = Util.getLook(living, partialTicks, offsetYaw);
        //■プレイヤー視線(レンジ)
        Vec3d posLivRange = posLivEye.addVector(vecLivLook.x * range, vecLivLook.y * range, vecLivLook.z * range);
        //■草とかに攻撃が吸われないように。
        return living.getEntityWorld().rayTraceBlocks(posLivEye, posLivRange, false, true, true);
    }

    /**
     * ■EntityLivingBaseをパｋ参考。
     * @param living
     * @param partialTicks
     * @return
     */
    public static Vec3d getLook(EntityLivingBase living, float partialTicks, float offsetYaw)
    {
        if (partialTicks == 1.0F)
        {
            return Util.getVectorForRotation(living.rotationPitch, living.rotationYawHead + offsetYaw);
        }
        else
        {
            float f = living.prevRotationPitch + (living.rotationPitch - living.prevRotationPitch) * partialTicks;
            float f1 = living.prevRotationYawHead + (living.rotationYawHead - living.prevRotationYawHead) * partialTicks;
            return Util.getVectorForRotation(f, f1 + offsetYaw);
        }
    }

    private static final Vec3d getVectorForRotation(float pitch, float yaw)
    {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3d((double)(f1 * f2), (double)f3, (double)(f * f2));
    }

    /**
     * ■ItemBanner.appendHoverTextFromTileEntityTag を丸パｋ参考にした
     * @param stack
     * @param p_185054_1_
     */
    public static boolean hasPattern(ItemStack stack, BannerPattern patternIn)
    {
        //■ブロック情報
        NBTTagCompound nbttagcompound = stack.getSubCompound("BlockEntityTag");

        //■無地の盾じゃない！
        if (nbttagcompound != null && nbttagcompound.hasKey("Patterns"))
        {
            //■どんな模様かな？
            NBTTagList nbttaglist = nbttagcompound.getTagList("Patterns", 10);

            for (int i = 0; i < nbttaglist.tagCount() && i < 6; ++i)
            {
                //■リストの先頭から覗いてみよう。
                NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);

                //■色は？
                EnumDyeColor enumdyecolor = EnumDyeColor.byDyeDamage(nbttagcompound1.getInteger("Color"));

                //■柄は？
                BannerPattern bannerpattern = BannerPattern.byHash(nbttagcompound1.getString("Pattern"));

                //■
                if (bannerpattern == patternIn)
                {
                    return true;
//                    p_185054_1_.add(I18n.translateToLocal("item.banner." + bannerpattern.getFileName() + "." + enumdyecolor.getUnlocalizedName()));
                }
            }
        }

        return false;
    }

    public static boolean isEventCanceled(Event event)
    {
        return event.isCancelable() ? event.isCanceled() : false;
    }

    public static boolean isEmptyStack(ItemStack stackIn) { return stackIn == null || stackIn == ItemStack.EMPTY; }
}
