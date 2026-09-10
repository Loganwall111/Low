package net.mcsm.extras.client;

import net.mcsm.extras.entity.StoryCharacterEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * 1.9.205 -- renders the Story Mode cast as player-shaped humanoids.
 *
 * Uses the vanilla PLAYER model layer (64x64 skin layout, walk + arm-swing
 * cycle from HumanoidModel) and layers the two acting channels on top:
 *   talking  -- head nods, right arm raised and gesturing, slight body lean
 *   laughing -- fast head bob, shoulders shaking, both arms drawn in
 * Skin = textures/entity/story/<character>.png; unknown names fall back to
 * jesse.png so a cast member can never render as the missing-texture cube.
 */
public class StoryCharacterRenderer extends HumanoidMobRenderer<StoryCharacterEntity, StoryCharacterRenderer.State, StoryCharacterRenderer.Model> {

    public static final class State extends HumanoidRenderState {
        public String character = "jesse";
        public boolean talking;
        public boolean laughing;
        public float actTime;
    }

    public static final class Model extends HumanoidModel<State> {
        public Model(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(State s) {
            super.setupAnim(s);
            float t = s.actTime;
            if (s.laughing) {
                float bob = Mth.sin(t * 1.1F);
                this.head.xRot += 0.18F + bob * 0.16F;
                this.body.xRot += 0.05F + bob * 0.04F;
                this.rightArm.xRot = -0.9F + bob * 0.25F;
                this.leftArm.xRot = -0.9F - bob * 0.25F;
                this.rightArm.zRot = -0.35F;
                this.leftArm.zRot = 0.35F;
                this.rightArm.yRot = 0.0F;
                this.leftArm.yRot = 0.0F;
            } else if (s.talking) {
                float nod = Mth.sin(t * 0.35F) * 0.10F + Mth.sin(t * 0.9F) * 0.05F;
                this.head.xRot += nod;
                this.head.zRot += Mth.sin(t * 0.22F) * 0.06F;
                // gesturing right arm: raised to chest height, waving forward/back
                float wave = Mth.sin(t * 0.6F);
                this.rightArm.xRot = -1.25F + wave * 0.35F;
                this.rightArm.yRot = -0.25F + wave * 0.15F;
                this.rightArm.zRot = -0.15F;
                // left arm relaxed, small sway
                this.leftArm.xRot *= 0.4F;
                this.leftArm.zRot = 0.08F + Mth.sin(t * 0.3F) * 0.04F;
                this.body.yRot += Mth.sin(t * 0.2F) * 0.04F;
            }
            this.hat.xRot = this.head.xRot;
            this.hat.yRot = this.head.yRot;
            this.hat.zRot = this.head.zRot;
        }
    }

    private static final Identifier FALLBACK = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/entity/story/jesse.png");

    public StoryCharacterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(StoryCharacterEntity e, State s, float partialTick) {
        super.extractRenderState(e, s, partialTick);
        s.character = e.getCharacter();
        s.talking = e.isTalking();
        s.laughing = e.isLaughing();
        s.actTime = e.tickCount + partialTick;
    }

    @Override
    public Identifier getTextureLocation(State s) {
        String c = s.character == null ? "jesse" : s.character;
        if (!c.matches("[a-z0-9_]+")) {
            return FALLBACK;
        }
        return Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/entity/story/" + c + ".png");
    }
}
