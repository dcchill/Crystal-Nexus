package net.crystalnexus.client.renderer;

import net.crystalnexus.block.entity.HemochanterBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/** Delegates directly to Mojang's enchanting-table renderer. */
public final class HemochanterRenderer implements BlockEntityRenderer<HemochanterBlockEntity> {
    private final EnchantTableRenderer book;
    public HemochanterRenderer(BlockEntityRendererProvider.Context context) { book = new EnchantTableRenderer(context); }
    @Override public void render(HemochanterBlockEntity blockEntity, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        book.render(blockEntity.book(), partialTick, pose, buffer, light, overlay);
    }
}
