package com.zhufucdev.shaver

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import kotlinx.coroutines.*
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.loader.impl.util.log.Log
import net.fabricmc.loader.impl.util.log.LogCategory
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.EntityType
import net.minecraft.entity.FallingBlockEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.passive.SheepEntity
import net.minecraft.resource.ResourceType
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i

var coroutine = CoroutineScope(Dispatchers.Default)

var isShaving = false
val shaveResult = mutableMapOf<Vec3i, DyeColor>()
val shavers = mutableListOf<SheepEntity>()

const val GRASS_COUNT = 40
const val SHAVER_COUNT = 8

private var platformBuilt = false

val DISPLAY_BUILT_ID = Identifier("shaver", "display_built")
val RESULT_SHOWN_SHEEP_SHEARED_ID = Identifier("shaver", "result_shown")

fun init() {
    CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
        dispatcher.register(literal<ServerCommandSource?>("shaver")
            .then(literal<ServerCommandSource?>("start").executes {
                if (platformBuilt) {
                    isShaving = true
                    it.source.sendMessage(Text.literal("Watch it"))
                    1
                } else {
                    it.source.sendError(Text.literal("Platform not built yet. Couldn't start"))
                    0
                }
            })
            .executes {
                val sender = it.source.entity
                if (sender is ServerPlayerEntity) {
                    coroutine.launch {
                        buildPlatform(sender)
                        platformBuilt = true
                    }
                }
                1
            })
    }

    ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(ResourceHandler)

    SheepShearCallback.EVENT.register { player, sheep ->
        if (!shavers.contains(sheep)) return@register ActionResult.PASS
        shavers.forEach { shaver ->
            shaver.isSheared = true

            val result = getResult(shaver.color) {
                ResourceHandler.mapping?.get(it + 241) ?: "${it + 241}"
            }
            shaver.customName = result
            Log.info(LogCategory.GENERAL, "${shaver.color.getName()}: ${result.string}")
        }

        val world = sheep.world
        if (world is ServerWorld) {
            val shearer = world.players.first { it.uuid == player.uuid }
            ServerPlayNetworking.send(shearer, RESULT_SHOWN_SHEEP_SHEARED_ID, PacketByteBufs.create())
        }
        ActionResult.PASS
    }

    ServerLifecycleEvents.SERVER_STOPPING.register {
        isShaving = false
        shaveResult.clear()
        shavers.clear()
        coroutine.cancel()
    }

    ServerLifecycleEvents.SERVER_STARTING.register {
        coroutine = CoroutineScope(Dispatchers.Default)
    }

    var completed = 0
    ShaverCompleteCallback.EVENT.register(object : ShaverCompleteCallback {
        override fun complete(color: DyeColor, shaver: SheepEntity) {
            val world = shaver.world

            val range = ServerState.state.shaverScope
            val x = (range.least.x + range.most.x) / 2
            val y = range.least.y + 5
            val z = range.least.z + completed

            val concretes = shaveResult.filterValues { it == color }.keys.map { original ->
                val blockPos = BlockPos(original)
                val originalState = world.getBlockState(blockPos)
                FallingBlockEntity.spawnFromBlock(world, blockPos, originalState)
                world.setBlockState(blockPos, Blocks.BARRIER.defaultState)

                originalState
            }

            var countdown = 20
            ServerTickEvents.START_SERVER_TICK.register {
                if (countdown >= 0)
                    countdown--
                if (countdown == 0) {
                    concretes.forEach { concrete ->
                        val baseline1 = BlockPos(x, y, z)
                        buildDisplay(baseline1, concrete, shavers.first { it.color == color })
                    }
                }
            }

            completed++
        }
    })
}

suspend fun buildPlatform(player: ServerPlayerEntity) {
    val baseline = player.blockPos.subtract(Vec3i(0, 5, 0))
    val world = player.world

    suspend fun forEach2D(block: suspend (Vec3i) -> Unit) {
        for (x in -2..2) {
            for (z in -4..3) {
                block(Vec3i(x, 0, z))
            }
        }
    }

    ServerState.state.shaverScope =
        BlockPosRange(baseline.add(Vec3i(2, 0, 3)), baseline.add(Vec3i(-2, 0, -4)))
    forEach2D {
        world.setBlockState(baseline.add(it), Blocks.GRASS_BLOCK.defaultState)
        for (y in 1..5) {
            world.setBlockState(baseline.add(it).up(y), Blocks.AIR.defaultState)
        }
        delay(50)
    }

    for (x in -2..2) {
        world.setBlockState(baseline.add(Vec3i(x, 1, -5)), Blocks.BARRIER.defaultState)
        world.setBlockState(baseline.add(Vec3i(x, 1, 4)), Blocks.BARRIER.defaultState)
    }
    for (z in -4..3) {
        world.setBlockState(baseline.add(Vec3i(-3, 1, z)), Blocks.BARRIER.defaultState)
        world.setBlockState(baseline.add(Vec3i(3, 1, z)), Blocks.BARRIER.defaultState)
    }

    fun spawnSheep(offset: Vec3i, color: DyeColor) {
        val sheep = EntityType.SHEEP.spawn(player.getWorld(), baseline.add(offset), SpawnReason.COMMAND) ?: return
        sheep.color = color
        shavers.add(sheep)
    }

    val sheepMeta = arrayOf(
        Vec3i(0, 1, 0) to DyeColor.BLACK,
        Vec3i(1, 1, 0) to DyeColor.BLUE,
        Vec3i(1, 1, 1) to DyeColor.RED,
        Vec3i(0, 1, 1) to DyeColor.BROWN,
        Vec3i(-1, 1, 1) to DyeColor.PURPLE,
        Vec3i(-1, 1, 0) to DyeColor.LIME,
        Vec3i(-1, 1, -1) to DyeColor.MAGENTA,
        Vec3i(0, 1, -1) to DyeColor.WHITE
    )
    sheepMeta.forEach { (o, c) ->
        spawnSheep(o, c)
        delay(200)
    }


}

private fun buildDisplay(baseline: BlockPos, concrete: BlockState, shaver: SheepEntity) {
    val world = shaver.world as ServerWorld
    world.setBlockState(baseline, concrete)
    world.players.forEach {
        val buf = PacketByteBufs.create()
        buf.writeBlockPos(baseline)
        ServerPlayNetworking.send(it, DISPLAY_BUILT_ID, buf)
    }
    world.playSound(
        null,
        baseline.x.toDouble(),
        baseline.y.toDouble(),
        baseline.z.toDouble(),
        SoundEvents.ENTITY_ARROW_HIT_PLAYER,
        SoundCategory.AMBIENT,
        10F,
        1F
    )

    shaver.setNoGravity(true)
    shaver.isInvulnerable = true
    val origin = shaver.pos
    val delta = baseline.toCenterPos().add(0.0, 0.5, 0.0).subtract(origin)
    var k = 0.0
    ServerTickEvents.END_SERVER_TICK.register {
        if (k <= 1) {
            k += 1.0 / 40
            val current = delta.multiply(k).add(origin)
            shaver.updatePosition(
                current.x,
                current.y,
                current.z
            )
        } else {
            val fin = origin.add(delta)
            shaver.updatePosition(fin.x, fin.y, fin.z)
        }
    }
}

private fun getResult(color: DyeColor, map: (Int) -> String = { "$it" }): Text {
    val least = ServerState.state.shaverScope.least
    val result = shaveResult.filterValues { it == color }.keys.joinToString {
        val ordered = it.subtract(least)
        map(ordered.x + ordered.z * GRASS_COUNT / SHAVER_COUNT)
    }

    return Text.literal(result).styled {
        it.withColor(color.signColor)
            .withBold(true)
    }
}