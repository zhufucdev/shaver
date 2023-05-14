package com.zhufucdev.shaver

import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.passive.SheepEntity
import net.minecraft.util.DyeColor

interface ShaverCompleteCallback {
    companion object {
        @JvmStatic
        val EVENT = EventFactory.createArrayBacked(ShaverCompleteCallback::class.java) { callbacks ->
            object : ShaverCompleteCallback {
                override fun complete(color: DyeColor, shaver: SheepEntity) {
                    callbacks.forEach {
                        it.complete(color, shaver)
                    }
                }
            }
        }
    }

    fun complete(color: DyeColor, shaver: SheepEntity)
}