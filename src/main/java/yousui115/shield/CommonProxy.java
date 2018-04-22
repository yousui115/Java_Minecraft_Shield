package yousui115.shield;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import yousui115.shield.event.EventGuardMove;
import yousui115.shield.event.EventGuardSplash;
import yousui115.shield.event.EventGuardState;
import yousui115.shield.event.EventShieldState;

public class CommonProxy
{
    public void stackInputShout() {}
    public boolean isKeyUpShout() { return false; }
    public boolean isKeyPushShout() { return false; }
    public void resetStackShout() {}




    /**
     * ■1tickに1度しか呼んではいけない。
     * @param isPush
     */
    public void addPushAttack(){}
    /**
     * ■攻撃ボタンをリリースした瞬間
     * @return
     */
    public boolean isReleaseAttack() { return false; }
    /**
     * ■入力保持のリセット
     */
    public void resetAttack() { }

//    public boolean isBashing() { return false; }

    public int getNumAttackTick() { return 0; }
    public int getPower() { return 0; }
    public int getNumAttackLength() { return 0; }

    /* ================================ register ================================ */
    public void registerKeyBinding(){}

    public void registerEvent()
    {
        MinecraftForge.EVENT_BUS.register(new EventGuardMove());
        MinecraftForge.EVENT_BUS.register(new EventGuardState());
        MinecraftForge.EVENT_BUS.register(new EventGuardSplash());
        MinecraftForge.EVENT_BUS.register(new EventShieldState());
    }

    /* ================================ getter ================================ */
    @Nullable
    public Minecraft getMinecraft() { return null; }
    @Nullable
    public EntityPlayer getThePlayer() { return null; }
}
