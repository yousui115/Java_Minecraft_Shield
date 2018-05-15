package yousui115.shield;

import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import yousui115.shield.network.PacketHandler;

@Mod(modid = Shield.MOD_ID, version = Shield.VERSION)
public class Shield
{
    public static final String MOD_ID = "shield";
    public static final String MOD_DOMAIN = "yousui115." + MOD_ID;
    public static final String VERSION = "M1122_F2555_v10";

    public static Logger log;

    //■インスタント
    @Mod.Instance(MOD_ID)
    public static Shield INSTANCE;

    //■ぷろきしー
    @SidedProxy(clientSide = MOD_DOMAIN + ".client.ClientProxy", serverSide = MOD_DOMAIN + ".CommonProxy")
    public static CommonProxy proxy;

    /**
     * ■初期化処理 (前処理)
     * @param event
     */
    @EventHandler
    public void preinit(FMLPreInitializationEvent event)
    {
        log = event.getModLog();

        //■パケットの登録
        PacketHandler.register();
    }

    /**
     * ■初期化処理 (本処理)
     * @param event
     */
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.registerKeyBinding();

        proxy.registerEvent();
    }


}
