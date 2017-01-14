package yousui115.shield;

import java.util.UUID;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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

        if (activeStack != null && living.isHandActive())
        {
            //■アイテムを持っていて、使用中である。
            Item activeItem = activeStack.getItem();

            if (!isBashing(living) && activeItem.getItemUseAction(activeStack) == EnumAction.BLOCK)
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

        if (isGuard(living))
        {
            //■右クリックを押した瞬間～4tickまでは「ジャストガード」
            isJG = activeStack.getItem().getMaxItemUseDuration(activeStack) - living.getItemInUseCount() < 5;
        }

        //■音を鳴らす
        if (!living.worldObj.isRemote && isJG)
        {
            living.worldObj.playSound(null, living.posX, living.posY, living.posZ, SoundEvents.ENTITY_BLAZE_HURT, SoundCategory.HOSTILE, 1.0f, 1.5f);
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
     * ■使用中アイテム の ガード性能レベル を取得
     * @param living
     * @return 0:恩恵無し 1:ガード性能１ 2:ガード性能２
     */
//    public static int getEnchGuardLevel_UsingItem(EntityLivingBase living)
//    {
//        int level = 0;
//
//        //■現在使ってるアイテムを取得
//        ItemStack activeStack = living.getActiveItemStack();
//
//        if (isGuard(living))
//        {
//            level = EnchantmentHelper.getEnchantmentLevel(enchGuard, activeStack);
//        }
//
//        return level;
//    }


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
        if (!damageSourceIn.isUnblockable() && isGuard(blocker))
        {
            Vec3d posDamageSource = damageSourceIn.getDamageLocation();

            //TNTの爆発をガード可能にする処理
            if (posExplosion != null) { posDamageSource = posExplosion; }

            if (posDamageSource != null)
            {
                Vec3d blockerLook = blocker.getLook(1.0F);
                Vec3d attackerLook = posDamageSource.subtractReverse(blocker.getPositionVector()).normalize();
                attackerLook = new Vec3d(attackerLook.xCoord, 0.0D, attackerLook.zCoord);

                //内積を使って、ガード範囲(180度)内か否かの算出を行う。
                if (attackerLook.dotProduct(blockerLook) < 0.0D)
                {
                    return true;
                }
            }
        }

        //■ノックバック耐性上昇 の剥奪
        //  (メソッド名から見て、ここに含むべきではないかなぁ。でも、false なら剥奪処理は必ず行うしなぁ)
//        IAttributeInstance iattributeinstance = blocker.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
//        iattributeinstance.removeModifier(modifierGuardKnockback1);

        return false;
    }

    //■シールドのダメージ処理
    public static void damageShield(EntityLivingBase living, float damage)
    {
        if (damage >= 3.0F)// && living.getActiveItemStack().getItem() == Items.SHIELD)
        {
            int i = 1 + MathHelper.floor_float(damage);
            living.getActiveItemStack().damageItem(i, living);

            if (living.getActiveItemStack().func_190926_b())
            {
                EnumHand enumhand = living.getActiveHand();

                if (living instanceof EntityPlayer)
                {
                    net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem((EntityPlayer)living, living.getActiveItemStack(), enumhand);
                }

                if (enumhand == EnumHand.MAIN_HAND)
                {
                    living.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.field_190927_a);
                }
                else
                {
                    living.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, ItemStack.field_190927_a);
                }

                living.resetActiveHand();
                living.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.8F, 0.8F + living.worldObj.rand.nextFloat() * 0.4F);
            }
        }
    }

    public static void tameAIDonmov(EntityLiving target, int power)
    {
        boolean is = false;
        int tick = power * 20;

        for (EntityAITasks.EntityAITaskEntry entry : target.tasks.taskEntries)
        {
            if (entry.action instanceof EntityAIDonMov)
            {
                EntityAIDonMov don = (EntityAIDonMov)entry.action;
                don.tick = tick;
                is = true;
            }
        }
        if (!is)
        {
            EntityAIDonMov ai = new EntityAIDonMov(target);
            ai.tick = tick;
            target.tasks.addTask(0, ai);
        }
    }
}
