package com.zhufucdev.shaver

import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.entity.EntityStatuses
import net.minecraft.entity.ai.goal.EatGrassGoal
import net.minecraft.entity.passive.SheepEntity
import net.minecraft.util.DyeColor

class EatGrassGoal(private val mob: SheepEntity) : EatGrassGoal(mob) {
    private var timer: Int = 0
    private val completed get() = mob.shaverGrade() >= GRASS_COUNT / SHAVER_COUNT
    override fun canStart(): Boolean {
        if (!isShaving) return false
        if (completed)
            return false
        return canEatGrass()
    }

    override fun start() {
        timer = getTickCount(40)
        mob.world.sendEntityStatus(mob, EntityStatuses.SET_SHEEP_EAT_GRASS_TIMER_OR_PRIME_TNT_MINECART)
        mob.navigation.stop()
    }

    override fun stop() {
        timer = 0
    }

    override fun shouldContinue(): Boolean {
        return timer > 0
    }

    override fun tick() {
        timer = maxOf(0, timer - 1)
        if (timer != getTickCount(4)) return

        val world = mob.world
        if (canEatGrass()) {
            world.setBlockState(grassPos, correspondingConcrete().defaultState, Block.NOTIFY_LISTENERS)
            ServerState.state.shaverGrades[mob.uuid] = mob.shaverGrade() + 1

            if (completed) {
                ShaverCompleteCallback.EVENT.invoker().complete(mob.color, mob)
            }
        }
        mob.onEatingGrass()
    }

    private val grassPos
        get() = GoToGrassGoal.targetBlock[mob.uuid] ?: mob.blockPos.down()

    private fun canEatGrass(): Boolean {
        return grassPos.isWithinDistance(mob.pos, 5.0)
                && mob.world.getBlockState(grassPos).isOf(Blocks.GRASS_BLOCK)
    }

    private fun correspondingConcrete(): Block =
        when (mob.color) {
            DyeColor.BLACK -> Blocks.BLACK_CONCRETE
            DyeColor.BLUE -> Blocks.BLUE_CONCRETE
            DyeColor.RED -> Blocks.RED_CONCRETE
            DyeColor.BROWN -> Blocks.BROWN_CONCRETE
            DyeColor.PURPLE -> Blocks.PURPLE_CONCRETE
            DyeColor.LIME -> Blocks.LIME_CONCRETE
            DyeColor.MAGENTA -> Blocks.MAGENTA_CONCRETE
            DyeColor.WHITE -> Blocks.WHITE_CONCRETE
            else -> Blocks.DIRT
        }
}