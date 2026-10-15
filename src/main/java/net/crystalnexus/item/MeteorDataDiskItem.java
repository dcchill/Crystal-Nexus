package net.crystalnexus.item;

import net.crystalnexus.multiblock.RecordedMeteor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

public final class MeteorDataDiskItem extends Item {
    private static final String RECORD = "crystalnexus_meteor";

    public MeteorDataDiskItem() { super(new Properties().stacksTo(1)); }

    public static Optional<RecordedMeteor> read(ItemStack disk) {
        if (!(disk.getItem() instanceof MeteorDataDiskItem)) return Optional.empty();
        return RecordedMeteor.load(disk.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(RECORD));
    }

    public static void write(ItemStack disk, RecordedMeteor meteor) {
        if (!(disk.getItem() instanceof MeteorDataDiskItem)) throw new IllegalArgumentException("Not a meteor disk");
        CustomData.update(DataComponents.CUSTOM_DATA, disk, tag -> tag.put(RECORD, meteor.save()));
    }

}
