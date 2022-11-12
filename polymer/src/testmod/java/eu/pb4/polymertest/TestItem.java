package eu.pb4.polymertest;

import eu.pb4.polymer.api.item.SimplePolymerItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registries;
import net.minecraft.world.World;

public class TestItem extends SimplePolymerItem {
    private Random random = Random.create();
    public TestItem(Item.Settings settings, Item virtualItem) {
        super(settings, virtualItem);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        context.getWorld().setBlockState(context.getBlockPos(), Registries.BLOCK.getRandom(this.random).get().value().getDefaultState());
        return super.useOnBlock(context);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        user.sendMessage(Text.literal("Use!" + hand), false);
        return super.use(world, user, hand);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
