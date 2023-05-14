package com.zhufucdev.shaver

import net.minecraft.client.MinecraftClient
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.Vec3i
import net.minecraft.world.PersistentState
import net.minecraft.world.World
import java.util.UUID

class ServerState : PersistentState() {
    lateinit var shaverScope: BlockPosRange
    val shaverGrades = mutableMapOf<UUID, Int>()

    override fun writeNbt(nbt: NbtCompound): NbtCompound {
        shaverGrades.forEach { (t, u) ->
            nbt.putInt(t.toString(), u)
        }
        nbt.putIntArray(
            "scope",
            intArrayOf(
                shaverScope.least.x,
                shaverScope.least.y,
                shaverScope.least.z,
                shaverScope.most.x,
                shaverScope.most.z
            )
        )
        return nbt
    }

    companion object {
        private fun createFromNbt(nbt: NbtCompound): ServerState {
            return ServerState().apply {
                shaverGrades.putAll(nbt.keys.filter { it != "scope" }.associate { UUID.fromString(it) to nbt.getInt(it) })
                shaverScope = nbt.getIntArray("scope").let {
                    BlockPosRange(Vec3i(it[0], it[1], it[2]), Vec3i(it[3], it[1], it[4]))
                }
            }
        }

        val state: ServerState by lazy(MinecraftClient.getInstance().server) {
            val server = MinecraftClient.getInstance().server ?: error("server not up")
            server.getWorld(World.OVERWORLD)!!.persistentStateManager.getOrCreate(
                ::createFromNbt,
                { ServerState() },
                "shaver:state"
            )
        }
    }
}