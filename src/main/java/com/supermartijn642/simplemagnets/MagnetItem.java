package com.supermartijn642.simplemagnets;

import com.supermartijn642.simplemagnets.packets.magnet.PacketItemInfo;
import com.supermartijn642.simplemagnets.packets.magnet.PacketToggleMagnetMessage;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Created 7/7/2020 by SuperMartijn642
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public abstract class MagnetItem extends Item {

    public MagnetItem(String registryName){
        super(new Properties().tab(SimpleMagnets.GROUP).stacksTo(1));
        this.setRegistryName(registryName);
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn){
        ItemStack stack = playerIn.getItemInHand(handIn);
        toggleMagnet(playerIn, stack);
        return new ActionResult<>(ActionResultType.SUCCESS, stack);
    }

    public static void toggleMagnet(PlayerEntity player, ItemStack stack){
        if(!player.level.isClientSide && stack.getItem() instanceof MagnetItem){
            boolean active = stack.getOrCreateTag().contains("active") && stack.getOrCreateTag().getBoolean("active");
            stack.getOrCreateTag().putBoolean("active", !active);
            // let the client decide whether to show the toggle message and play a sound
            SimpleMagnets.CHANNEL.sendToPlayer(player, new PacketToggleMagnetMessage(active));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected){
        CompoundNBT tag = stack.getOrCreateTag();
        if(tag.contains("active") && tag.getBoolean("active")){
            if(this.canPickupItems(tag)){
                int r = this.getRangeItems(tag);
                AxisAlignedBB area = new AxisAlignedBB(entityIn.getCommandSenderWorldPosition().add(-r, -r, -r), entityIn.getCommandSenderWorldPosition().add(r, r, r));

                List<ItemEntity> items = worldIn.getEntities(EntityType.ITEM, area,
                    item -> item.isAlive() && (!worldIn.isClientSide || item.tickCount > 1) &&
                        (item.getThrower() == null || !item.getThrower().equals(entityIn.getUUID()) || !item.hasPickUpDelay()) &&
                        !item.getItem().isEmpty() && !item.getPersistentData().contains("PreventRemoteMovement") && this.canPickupStack(tag, item.getItem())
                );
                items.forEach(item -> item.setPos(entityIn.getX(), entityIn.getY(), entityIn.getZ()));
                // Directly add items to the player's inventory when ItemPhysic is installed
                if(!worldIn.isClientSide && entityIn instanceof PlayerEntity && ModList.get().isLoaded("itemphysic"))
                    items.forEach(item -> playerTouch(item, (PlayerEntity)entityIn));
            }

            if(!worldIn.isClientSide && this.canPickupXp(tag) && entityIn instanceof PlayerEntity){
                int r = this.getRangeXp(tag);
                AxisAlignedBB area = new AxisAlignedBB(entityIn.getCommandSenderWorldPosition().add(-r, -r, -r), entityIn.getCommandSenderWorldPosition().add(r, r, r));

                PlayerEntity player = (PlayerEntity)entityIn;
                List<ExperienceOrbEntity> orbs = worldIn.getEntitiesOfClass(ExperienceOrbEntity.class, area);
                orbs.forEach(orb -> {
                    orb.throwTime = 0;
                    player.takeXpDelay = 0;
                    orb.playerTouch(player);
                });
            }
        }
    }

    /**
     * Copied from {@link ItemEntity#playerTouch(PlayerEntity)}. Use this when ItemPhysic is installed to still pick up items.
     */
    private static void playerTouch(ItemEntity itemEntity, PlayerEntity player){
        if(!itemEntity.level.isClientSide){
            if(itemEntity.hasPickUpDelay()) return;
            ItemStack itemstack = itemEntity.getItem();
            Item item = itemstack.getItem();
            int i = itemstack.getCount();

            int hook = net.minecraftforge.event.ForgeEventFactory.onItemPickup(itemEntity, player);
            if(hook < 0) return;

            ItemStack copy = itemstack.copy();
            if(!itemEntity.hasPickUpDelay() && (itemEntity.getOwner() == null || itemEntity.lifespan - itemEntity.getAge() <= 200 || itemEntity.getOwner().equals(player.getUUID())) && (hook == 1 || i <= 0 || player.inventory.add(itemstack))){
                copy.setCount(copy.getCount() - itemstack.getCount());
                net.minecraftforge.fml.hooks.BasicEventHooks.firePlayerItemPickupEvent(player, itemEntity, copy);
                player.take(itemEntity, i);
                if(itemstack.isEmpty()){
                    itemEntity.remove();
                    itemstack.setCount(i);
                }

                player.awardStat(Stats.ITEM_PICKED_UP.get(item), i);
            }
        }
    }

    protected abstract boolean canPickupItems(CompoundNBT tag);

    protected abstract boolean canPickupStack(CompoundNBT tag, ItemStack stack);

    protected abstract boolean canPickupXp(CompoundNBT tag);

    protected abstract int getRangeItems(CompoundNBT tag);

    protected abstract int getRangeXp(CompoundNBT tag);

    @Override
    public boolean isFoil(ItemStack stack){
        return stack.getOrCreateTag().contains("active") && stack.getOrCreateTag().getBoolean("active");
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn){
        tooltip.add(this.getTooltip());
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
    }

    protected abstract ITextComponent getTooltip();

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking e){
        if(!e.getPlayer().level.isClientSide && e.getTarget() instanceof ItemEntity && ((ItemEntity)e.getTarget()).getThrower() != null)
            SimpleMagnets.CHANNEL.sendToPlayer(e.getPlayer(), new PacketItemInfo((ItemEntity)e.getTarget()));
    }
}
