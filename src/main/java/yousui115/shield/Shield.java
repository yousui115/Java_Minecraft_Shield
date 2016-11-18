package yousui115.shield;

import java.util.Iterator;
import java.util.UUID;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantment.Rarity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = Shield.MODID, version = Shield.VERSION)
public class Shield
{
    public static final String MODID = "shield";
    public static final String VERSION = "M1102_F2099_v1";

    public static Enchantment enchGuard;

    /*************************  INIT  ******************************/

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        //■イベントの登録
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new AnvilEventHooks());

        //■エンチャントの生成・登録
        enchGuard = new EnchantmentGuard(Rarity.COMMON, EnumEnchantmentType.BREAKABLE, new EntityEquipmentSlot[] {EntityEquipmentSlot.MAINHAND});
        enchGuard.setName("guard");
        Enchantment.REGISTRY.register(220, new ResourceLocation("guard"), enchGuard);

        //■ガード性能１の盾は、レシピから作成できる
        ItemStack shield = new ItemStack(Items.SHIELD);
        shield.addEnchantment(enchGuard, enchGuard.getMinLevel());
        GameRegistry.addRecipe(shield,
                "#I#",
                "###",
                " # ",
                '#', Blocks.PLANKS,
                'I', Blocks.IRON_BLOCK
        );
    }

    /*************************  EVENT_BUS  ******************************/

    /**
     * ■爆発判定に巻き込まれた際のイベント
     * @param event イベントコンテナ
     * @param event.world
     * @param event.explosion 爆発本体(Entityに非ず)
     * @param event.entityList 爆発に巻き込まれたEntity群
     */
    @SubscribeEvent
    public void doExplosionGuard(ExplosionEvent.Detonate event)
    {
        for (Iterator itr = event.getAffectedEntities().iterator(); itr.hasNext(); )
        {
            Object obj = itr.next();

            //■EntityLivingBase へ置換
            if (!(obj instanceof EntityLivingBase)) { continue; }
            EntityLivingBase blocker = (EntityLivingBase)obj;

            //■ガード可能か否か(ガード不可攻撃、ガード方向の判定）
            DamageSource source = DamageSource.causeExplosionDamage(event.getExplosion());
            if (!canBlockDamageSource(source, blocker, event.getExplosion().getPosition())) { continue; }

            //■
            if (isJustGuard(blocker, true))
            {
                //▼爆発をジャストガードした時の処理

            }
            else if (isGuard(blocker) && getEnchGuardLevel_UsingItem(blocker) != 0)
            {
                //▼爆発をガード性能付きアイテムでガードした時の処理(ジャストガード除く)

                //■爆発を通常ガードした時の処理
                //  Explosion.doExplosionA() のほぼコピペ
                Explosion expl = event.getExplosion();
                double explosionX = expl.getPosition().xCoord;
                double explosionY = expl.getPosition().yCoord;
                double explosionZ = expl.getPosition().zCoord;
                float explSize = ObfuscationReflectionHelper.getPrivateValue(Explosion.class, expl, 8);

                float f3 = explSize * 2.0f;
                double d12 = blocker.getDistance(explosionX, explosionY, explosionZ) / (double)f3;

                if (d12 <= 1.0D)
                {
                    double d5 = blocker.posX - explosionX;
                    double d7 = blocker.posY + (double)blocker.getEyeHeight() - explosionY;
                    double d9 = blocker.posZ - explosionZ;
                    double d13 = (double)MathHelper.sqrt_double(d5 * d5 + d7 * d7 + d9 * d9);

                    if (d13 != 0.0D)
                    {
                        d5 = d5 / d13;
                        d7 = d7 / d13;
                        d9 = d9 / d13;
                        double d14 = (double)blocker.worldObj.getBlockDensity(expl.getPosition(), blocker.getEntityBoundingBox());
                        double d10 = (1.0D - d12) * d14;

                        //■ノックバックはあるが、上には吹っ飛ばない。
                        int level = getEnchGuardLevel_UsingItem(blocker);
                        if (level == 1)
                        {
                            //■ノックバック耐性上昇 の剥奪
                            IAttributeInstance iattributeinstance = blocker.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
                            iattributeinstance.removeModifier(modifierGuardKnockback1);
                        }

                        //■ダメージ処理
                        float damage = (float)((int)((d10 * d10 + d10) / 2.0D * 7.0D * (double)f3 + 1.0D));
                        //TNT用
                        if (level == 1)
                        {
                            //▼ガード性能１
                            damage -= 2.0f;
                        }
                        else if (level == 2)
                        {
                            //▼ガード性能２
                            damage -= 5.0f;
                        }

                        blocker.attackEntityFrom(DamageSource.causeExplosionDamage(expl), damage);

                        double d11 = 1.0D;

                        //■エンチャント：爆発耐性 でのノックバック耐性
                        if (blocker instanceof EntityLivingBase)
                        {
                            d11 = EnchantmentProtection.getBlastDamageReduction((EntityLivingBase)blocker, d10);
                        }

                        //■ノックバックはあるが、上には吹っ飛ばない。
                        if (level == 1)
                        {
                            blocker.motionX += d5 * d11;
                            blocker.motionZ += d9 * d11;
                        }
                    }
                }
            }
            else
            {
                continue;
            }

            //■ブロッキングしてる生物 なので、従来の処理を走らせないようにリストから削除
            itr.remove();
        }
    }

    //■「ノックバック耐性上昇」パラメータ
    private static final UUID UUID_GuardKnockback1 = UUID.fromString("85a28cfd-b83a-6877-28bd-5026c894a324");//TODO:このハッシュ値、適当だけどいいのかにゃー？
    private static final AttributeModifier modifierGuardKnockback1 = (new AttributeModifier(UUID_GuardKnockback1, "Guard Knockback Amount 1", 1.0, 0)).setSaved(false);

    /**
     * ■ガード時、ノックバック耐性上昇
     * @param event
     */
    @SubscribeEvent
    public void applyGuardKnockback(LivingEvent.LivingUpdateEvent event)
    {
        EntityLivingBase living = event.getEntityLiving();

        //「ノックバック耐性」属性 の取得
        IAttributeInstance iattributeinstance = living.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);

        //■ノックバック耐性上昇 の剥奪
        iattributeinstance.removeModifier(modifierGuardKnockback1);

        //■ノックバック耐性上昇 を付与するか否か
        if (isGuard(living))
        {
            //■ガード性能が付与されたアイテムがアクティブか否か
            if (getEnchGuardLevel_UsingItem(living) != 0)
            {
                //▼ガード性能
                iattributeinstance.applyModifier(modifierGuardKnockback1);
            }
        }
    }

    /**
     * ■ジャストガード時 に行う処理
     * @param event
     */
    @SubscribeEvent
    public void doJustGuard(LivingAttackEvent event)
    {
        EntityLivingBase defender = event.getEntityLiving();
        DamageSource source = event.getSource();

        //■ガード可能か否か(ガード不可攻撃、ガード状態、ガード方向の判定）
        if (!canBlockDamageSource(source, defender, null)) { return; }

        //■ジャストガードしたか否か
        if (isJustGuard(defender, true))
        {
            //■ジャストガードが発生したので、呼び出し元の後処理を行わせない。
            event.setCanceled(true);

            //■ジャストガード時の処理
            if (source.getSourceOfDamage() != null &&
                source.getSourceOfDamage().isEntityAlive() &&
                source.getSourceOfDamage() instanceof EntityLivingBase)
            {
                //■アタッカーをノックバック(大)させる
                EntityLivingBase attacker = (EntityLivingBase)source.getSourceOfDamage();

                attacker.knockBack(defender, 1.5F, defender.posX - attacker.posX, defender.posZ - attacker.posZ);
            }
        }
    }

    /**
     * ■被ダメージ時、ガード性能によりダメージ軽減
     * @param event
     */
    @SubscribeEvent
    public void doHurt(LivingHurtEvent event)
    {
        //■
        EntityLivingBase blocker = event.getEntityLiving();

        //■ガード可能か否か(ガード不可攻撃、ガード方向の判定）
        if (!canBlockDamageSource(event.getSource(), blocker, null)) { return; }

        //■「使用中」アイテムの「ガード性能」レベルを取得
        int level = getEnchGuardLevel_UsingItem(blocker);
        float amount = event.getAmount();

        //TODO 要バランス調整
        if (!event.getSource().isExplosion())
        {
            //爆発に関してはdoExplosionGuard()でダメージ軽減済み
            //TODO 盾のダメージ軽減処理は乗算(*0.33F)の為、処理位置により結果が変わる。
            //     本来はここで一括して処理したいが、いかんせん、それが出来る作りじゃないので妥協。
            if (level == 1)
            {
                //▼ガード性能１
                amount -= 2.0f;
            }
            else if (level == 2)
            {
                //▼ガード性能２
                amount -= 5.0f;
            }
        }

        event.setAmount(amount < 0 ? 0 : amount);
    }


    //■「ガード時歩行速度上昇」パラメータ
    private static double amount_walk = 3.00000001192092896D;
    private static final UUID guardWalkSpeedUUID = UUID.fromString("9be6f9f0-c286-5fb1-974e-baec497a8033");
    private static final AttributeModifier guardWalkSpeedModifier = (new AttributeModifier(guardWalkSpeedUUID, "Guard walk speed", amount_walk, 2)).setSaved(false);

    /**
     * ■ガード時の移動速度調整
     * @param event
     */
    @SubscribeEvent
    public void changeGuardMoveSpeed(LivingEvent.LivingUpdateEvent event)
    {
        EntityLivingBase living = event.getEntityLiving();

        //■プレイヤー以外はアイテムがアクティブでもスピードダウンしないっぽい
        if (living instanceof EntityPlayer)
        {
            IAttributeInstance iattributeinstance = living.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);

            //■「ガード時歩行速度上昇」 の剥奪
            iattributeinstance.removeModifier(guardWalkSpeedModifier);

            //TODO 満腹度でスピードを調整しているが、エンチャント等にすべきか？
            boolean canChangeSpeed = true;
            if (living instanceof EntityPlayer)
            {
                //■満腹度が15以下なら走れない(Max 20)
                canChangeSpeed = ((EntityPlayer)living).getFoodStats().getFoodLevel() > 15 ? true : false;
            }

            if (canChangeSpeed && isGuard(living))
            {
                //■「ガード時歩行速度上昇」 の付与
                iattributeinstance.applyModifier(guardWalkSpeedModifier);
            }
        }
    }

    /**
     * ■ガード時のFOVの設定
     * @param event
     */
    @SubscribeEvent
    public void onFOVUpdateEvent(FOVUpdateEvent event)
    {
        if(event.getEntity() == null) return;
        IAttributeInstance attri = event.getEntity().getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);

        if (attri != null && attri.hasModifier(guardWalkSpeedModifier))
        {
            float amounts = 0;

            for (AttributeModifier modi : attri.getModifiers())
            {
                if (modi.equals(guardWalkSpeedModifier)) { continue; }
                amounts += (float)modi.getAmount();
            }

            //TODO: FOV値に上限があれば、それも考慮しないといけないと思います。
            event.setNewfov(1f + amounts * 0.5f);
        }
    }

    /***************************  Util  *********************************/

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

            if (activeItem.getItemUseAction(activeStack) == EnumAction.BLOCK)
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
    public static boolean isJustGuard(EntityLivingBase living, boolean isSound)
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
        if (!living.worldObj.isRemote && isJG && isSound)
        {
            living.worldObj.playSound(null, living.posX, living.posY, living.posZ, SoundEvents.ENTITY_BLAZE_HURT, SoundCategory.HOSTILE, 1.0f, 1.5f);
            //living.playSound(SoundEvents.block_anvil_break, 1.0f, 1.0f);
        }

        return isJG;
    }

    /**
     * ■使用中アイテム の ガード性能レベル を取得
     * @param living
     * @return 0:恩恵無し 1:ガード性能１ 2:ガード性能２
     */
    public static int getEnchGuardLevel_UsingItem(EntityLivingBase living)
    {
        int level = 0;

        //■現在使ってるアイテムを取得
        ItemStack activeStack = living.getActiveItemStack();

        if (isGuard(living))
        {
            level = EnchantmentHelper.getEnchantmentLevel(enchGuard, activeStack);
        }

        return level;
    }

    /**
     * ■ダメージソース がガード可能か否か (ブロック不可・ガード状態・ガード方向の調査)
     *   (ノックバック耐性の剥奪も処理している)
     *   (EntityLivingBase.canBlockDamageSource() をパｋ参考にしました)
     * @param damageSourceIn
     * @param blocker
     * @return
     */
    public static boolean canBlockDamageSource(DamageSource damageSourceIn, EntityLivingBase blocker, Vec3d vec3dIn)
    {
        if (!damageSourceIn.isUnblockable() && isGuard(blocker))
        {
            Vec3d vec3d = damageSourceIn.getDamageLocation();

            //TNTの爆発をガード可能にする処理
            if (vec3dIn != null) { vec3d = vec3dIn; }

            if (vec3d != null)
            {
                Vec3d vec3d1 = blocker.getLook(1.0F);
                Vec3d vec3d2 = vec3d.subtractReverse(new Vec3d(blocker.posX, blocker.posY, blocker.posZ)).normalize();
                vec3d2 = new Vec3d(vec3d2.xCoord, 0.0D, vec3d2.zCoord);

                //内積を使って、ガード範囲(180度)内か否かの算出を行う。
                if (vec3d2.dotProduct(vec3d1) < 0.0D)
                {
                    return true;
                }
            }
        }

        //■ノックバック耐性上昇 の剥奪
        //  (メソッド名から見て、ここに含むべきではないかなぁ。でも、false なら剥奪処理は必ず行うしなぁ)
        IAttributeInstance iattributeinstance = blocker.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
        iattributeinstance.removeModifier(modifierGuardKnockback1);

        return false;
    }
}
