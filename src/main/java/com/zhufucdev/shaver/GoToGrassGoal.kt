package com.zhufucdev.shaver

import net.minecraft.block.Blocks
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.passive.SheepEntity
import net.minecraft.util.math.BlockPos
import java.util.*
import kotlin.math.roundToInt
import kotlin.random.Random

class GoToGrassGoal(private val mob: SheepEntity, private val scope: BlockPosRange) : Goal() {
    companion object {
        val targetBlock = mutableMapOf<UUID, BlockPos>()
    }

    private var arrived = false

    init {
        controls = EnumSet.of(Control.MOVE)
    }

    override fun canStart(): Boolean {
        if (arrived) {
            val target = targetBlock[mob.uuid]
            if (target != null) {
                if (!mob.world.getBlockState(target).isOf(Blocks.GRASS_BLOCK)) {
                    arrived = false
                    targetBlock.remove(mob.uuid)
                } else {
                    return false
                }
            }
        }

        return isShaving && filterGrasses().isNotEmpty()
                && mob.shaverGrade() < GRASS_COUNT / SHAVER_COUNT
    }

    override fun shouldContinue(): Boolean {
        if (mob.shaverGrade() >= GRASS_COUNT / SHAVER_COUNT) {
            return false
        }
        if (!isShaving) return false
        if (targetBlock[mob.uuid]?.let { mob.world.getBlockState(it).isOf(Blocks.GRASS_BLOCK) } == false) return false

        return !mob.navigation.isIdle
    }

    override fun start() {
        val grasses = filterGrasses()
        val index = ((grasses.size - 1) * Random.nextFloat()).roundToInt()
        val target = grasses[index]
        shaveResult[target] = mob.color
        targetBlock[mob.uuid] = target
        val centerBlock = target.toCenterPos()
        mob.navigation.startMovingTo(centerBlock.x, target.y + 1.0, centerBlock.z, 1.1)
    }

    private fun filterGrasses(): List<BlockPos> {
        val grasses = mutableListOf<BlockPos>()
        for (x in scope.least.x..scope.most.x) {
            for (z in scope.least.z..scope.most.z) {
                val pos = BlockPos(x, scope.least.y, z)
                if (mob.world.getBlockState(pos).isOf(Blocks.GRASS_BLOCK)) {
                    if (!shaveResult.containsKey(pos)) {
                        grasses.add(pos)
                    } else if (shaveResult[pos] == mob.color) {
                        return listOf(pos)
                    }
                }
            }
        }
        return grasses
    }

    override fun stop() {
        mob.navigation.stop()
        arrived = true
    }
}