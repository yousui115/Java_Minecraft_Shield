package yousui115.shield;

import java.util.Map;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

//TODO 本来は、取り出す前のアウトプットに「ガード性能１」と出力させたいが、
//     そう実装しようとすると、いろいろと面倒な処理を書かないといけなくなるので
//     ピックアップ時にガード性能１に書き換えている。怠慢。
public class AnvilEventHooks
{
    @SubscribeEvent
    public void onAnvilRepairEvent(AnvilRepairEvent event)
    {
        //注意：Forgeの不具合により
        //      event.getOutput() に Left
        //      event.getLeft()   に Right
        //      event.getRight()  に Output が入っている。Forge1863現在。
        ItemStack left = event.getOutput();
        ItemStack right = event.getLeft();
        ItemStack output = event.getRight();

        //■リネームとかだとrightがnullなんじゃないかな。
        if (left == null || right == null || output == null) { return; }

        int enchLvLeft = EnchantmentHelper.getEnchantmentLevel(Shield.enchGuard, left);
        int enchLvRight = EnchantmentHelper.getEnchantmentLevel(Shield.enchGuard, right);

        if (enchLvLeft == 1 && enchLvRight == 1)
        {
            //▼left と right に ガード性能１がある場合
            //■ガード性能１同士を掛け合わせると、ガード性能２になってしまうのを防ぐ
            Map<Enchantment, Integer> mapEnch = EnchantmentHelper.getEnchantments(output);

            if (mapEnch.containsKey(Shield.enchGuard))
            {
                mapEnch.put(Shield.enchGuard, 1);
            }

            EnchantmentHelper.setEnchantments(mapEnch, output);
        }
    }
}
