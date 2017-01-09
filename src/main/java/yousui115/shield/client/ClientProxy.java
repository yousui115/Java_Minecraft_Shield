package yousui115.shield.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yousui115.shield.CommonProxy;
import yousui115.shield.client.event.EventFOVChange;
import yousui115.shield.client.event.EventShieldBash;


//TODO 便利だからといって突っ込み過ぎ。
//     クラス分けしましょう（するとは言ってない）

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy
{
    //TODO intとかでbit演算した方がスマート(左シフトかな)
    private boolean isPushAttack[] = new boolean[20];

    /**
     * ■1tickに1度しか呼んではいけない。
     * @param isPush
     */
    @Override
    public void addPushAttack(boolean isPush)
    {
        for (int idx = isPushAttack.length - 2; idx >= 0; idx--)
        {
            isPushAttack[idx + 1] = isPushAttack[idx];
        }
        isPushAttack[0] = isPush;
    }

    /**
     * ■攻撃ボタンをリリースした瞬間
     * @return
     */
    @Override
    public boolean isReleaseAttack()
    {
        return isPushAttack[0] == false && isPushAttack[1] == true;
    }

    /**
     * ■入力保持のリセット
     */
    @Override
    public void resetAttack()
    {
        for(int idx = 0; idx < isPushAttack.length; idx++)
        {
            isPushAttack[idx] = false;
        }
    }

//    /**
//     * ■バッシュ中か否か
//     */
//    @Override
//    public boolean isBashing()
//    {
//        if (getThePlayer() != null
//            && getThePlayer().isActiveItemStackBlocking()
////            && isReleaseAttack()
//            && getThePlayer().isSwingInProgress)
//        {
//            return true;
//        }
//
//        return false;
//    }

    /**
     * ■入力時間に応じたバッシュパワーの取得
     * @return
     */
    @Override
    public int getNumAttackTick()
    {
        int power = 0;

        for (boolean is : isPushAttack) { power += (is == true ? 1 : 0); }

        return power;
    }

    /**
     * ■バッシュパワー(1:ノーマルバッシュ 2:パワーバッシュ)
     */
    @Override
    public int getPower()
    {
        return getNumAttackTick() < isPushAttack.length - 1 ? 1 : 2;
    }

    @Override
    public int getNumAttackLength() { return isPushAttack.length; }

    /* ================================ れじすたー ================================ */
    @Override
    public void registerEvent()
    {
        //■共通イベントの登録
        super.registerEvent();

        //■クライアントイベントの登録
        MinecraftForge.EVENT_BUS.register(new EventShieldBash());
        MinecraftForge.EVENT_BUS.register(new EventFOVChange());
    }

    /* ================================ その他 ================================ */
    @Nullable
    @Override
    public EntityPlayer getThePlayer() { return ((Minecraft)Minecraft.getMinecraft()).thePlayer; }
}
