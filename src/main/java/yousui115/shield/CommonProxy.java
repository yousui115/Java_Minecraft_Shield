package yousui115.shield;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import yousui115.shield.event.EventGuardAction;
import yousui115.shield.event.EventGuardMove;

public class CommonProxy
{
    /**
     * ■1tickに1度しか呼んではいけない。
     * @param isPush
     */
    public void addPushAttack(boolean isPush){}
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
    public void registerEvent()
    {
        MinecraftForge.EVENT_BUS.register(new EventGuardMove());
        MinecraftForge.EVENT_BUS.register(new EventGuardAction());
    }

    /* ================================ getter ================================ */
    @Nullable
    public EntityPlayer getThePlayer() { return null; }
}
