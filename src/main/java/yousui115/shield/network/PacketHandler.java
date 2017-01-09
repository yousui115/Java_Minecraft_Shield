package yousui115.shield.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import yousui115.shield.Shield;

public class PacketHandler
{
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Shield.MOD_ID);

    public static void register()
    {
        /*IMesssageHandlerクラスとMessageクラスの登録。
        *第三引数：MessageクラスのMOD内での登録ID。256個登録できる
        *第四引数：送り先指定。クライアントかサーバーか、Side.CLIENT Side.SERVER*/
        INSTANCE.registerMessage(  MessageBashTargetHandler.class,   MessageBashTarget.class, 0, Side.SERVER);
    }
}
