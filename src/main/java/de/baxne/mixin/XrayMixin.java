package de.baxne.mixin;

import de.baxne.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class XrayMixin {
    /** Xray: nur Erze etc. rendern, alles andere ausblenden. */
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void baxne$xray(BlockState state, BlockState neighbor, Direction dir, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.xray) return;
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        cir.setReturnValue(Config.xrayBlocks.contains(path));
    }
}
