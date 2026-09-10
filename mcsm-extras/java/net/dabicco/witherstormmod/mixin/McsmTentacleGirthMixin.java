package net.dabicco.witherstormmod.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.dabicco.witherstormmod.client.TentaclePhysics;
import net.dabicco.witherstormmod.entity.state.WitherStormRenderState;
import net.mcsm.extras.McsmExtrasConfig;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.9.204 -- Story Mode tentacle girth.
 *
 * The MCSM storm's tentacles are colossal heavy block limbs; the model's
 * chains are thin ropes.  After TentaclePhysics has posed every chain we fatten
 * the ROOT bone of each chain on its two cross axes.  Children inherit the
 * parent's scale, so scaling the root once thickens the whole limb uniformly
 * without compounding down the chain.
 */
@Mixin(value = TentaclePhysics.class, remap = false)
public abstract class McsmTentacleGirthMixin {

    private static final Map<ModelPart, List<ModelPart>> MCSM_ROOTS = new WeakHashMap<>();

    @Inject(method = "apply(Lnet/minecraft/client/model/geom/ModelPart;Lnet/dabicco/witherstormmod/entity/state/WitherStormRenderState;IF)V",
            at = @At("TAIL"), require = 0)
    private static void mcsm$girth(ModelPart root, WitherStormRenderState state, int namespace, float maxDeviation, CallbackInfo ci) {
        try {
            McsmExtrasConfig.load();
            float g = (float) McsmExtrasConfig.tentacleGirth;
            // early storm keeps thinner limbs; full girth from phase ~4.
            float phase = (float) state.phase;
            g = 1.0F + (g - 1.0F) * Mth.clamp((phase - 1.5F) / 2.5F, 0.35F, 1.0F);
            List<ModelPart> roots = MCSM_ROOTS.computeIfAbsent(root, McsmTentacleGirthMixin::mcsm$discover);
            for (ModelPart b : roots) {
                // The renderer resets every ModelPart to its base pose each
                // frame (TentaclePhysics.curlAndVanish relies on the same),
                // so a plain multiply here is per-frame and never compounds.
                b.xScale *= g;
                b.zScale *= g;
            }
        } catch (Throwable ignored) {
        }
    }

    private static List<ModelPart> mcsm$discover(ModelPart root) {
        List<ModelPart> out = new ArrayList<>();
        mcsm$collect(root, out);
        return out;
    }

    private static void mcsm$collect(ModelPart part, List<ModelPart> out) {
        for (ModelPart child : mcsm$children(part).values()) {
            int n = 0;
            ModelPart cur = child;
            for (; mcsm$children(cur).size() == 1; cur = mcsm$children(cur).values().iterator().next()) {
                n++;
            }
            n++;
            if (n >= 8) {
                out.add(child);
            }
            mcsm$collect(cur, out);
        }
    }

    private static Map<String, ModelPart> mcsm$children(ModelPart part) {
        return ((ModelPartAccessor) (Object) part).getChildren();
    }
}
