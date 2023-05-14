package com.zhufucdev.shaver.mixin;

import com.zhufucdev.shaver.EatGrassGoal;
import com.zhufucdev.shaver.GoToGrassGoal;
import com.zhufucdev.shaver.ServerState;
import com.zhufucdev.shaver.SheepShearCallback;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SheepEntity.class)
public class SheepEntityMixin {
    @Inject(method = "initGoals", at = @At("HEAD"), cancellable = true)
    void initGoals(CallbackInfo ci) {
        final var goalSelector = ((MobEntityAccessor) this).getGoalSelector();
        final var eatGrassGoal = new EatGrassGoal((SheepEntity) (Object) this);
        goalSelector.add(0, new GoToGrassGoal((SheepEntity) (Object) this, ServerState.Companion.getState().shaverScope));
        goalSelector.add(1, eatGrassGoal);
        goalSelector.add(2, new LookAroundGoal((SheepEntity) (Object) this));

        ((SheepEntityAccessor) this).setEatGrassGoal(eatGrassGoal);
        ci.cancel();
    }

    @Inject(at = @At(value = "HEAD"), method = "interactMob", cancellable = true)
    private void onShear(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ActionResult result = SheepShearCallback.EVENT.invoker().interact(player, (SheepEntity) (Object) this);

        if(result == ActionResult.FAIL) {
            cir.cancel();
        }
    }
}
