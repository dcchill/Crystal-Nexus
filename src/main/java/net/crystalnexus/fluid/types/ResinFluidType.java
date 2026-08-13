package net.crystalnexus.fluid.types;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.init.CrystalnexusModFluidTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Vector3f;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ResinFluidType extends FluidType {
    public ResinFluidType() {
        super(Properties.create().fallDistanceModifier(0F).canExtinguish(true).supportsBoating(true).canHydrate(true)
            .motionScale(0.007D).sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH));
    }

    @SubscribeEvent
    public static void registerFluidTypeExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL_TEXTURE = ResourceLocation.parse("crystalnexus:block/resin_still");
            private static final ResourceLocation FLOWING_TEXTURE = ResourceLocation.parse("crystalnexus:block/resin_flowing");

            @Override public ResourceLocation getStillTexture() { return STILL_TEXTURE; }
            @Override public ResourceLocation getFlowingTexture() { return FLOWING_TEXTURE; }
            @Override public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                    int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                return new Vector3f(0.2f, 0f, 0.2f);
            }
            @Override public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance,
                    float partialTick, float nearDistance, float farDistance, FogShape shape) {
                Entity entity = camera.getEntity();
                Level world = entity.level();
                RenderSystem.setShaderFogShape(FogShape.SPHERE);
                RenderSystem.setShaderFogStart(0f);
                RenderSystem.setShaderFogEnd(Math.min(48f, renderDistance));
            }
        }, CrystalnexusModFluidTypes.RESIN_TYPE.get());
    }
}
