package com.zhufucdev.shaver.client

import com.zhufucdev.shaver.DISPLAY_BUILT_ID
import com.zhufucdev.shaver.RESULT_SHOWN_SHEEP_SHEARED_ID
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.particle.ParticleTypes
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents

fun init() {
    ClientPlayNetworking.registerGlobalReceiver(DISPLAY_BUILT_ID) { client, _, buf, _ ->
        val pos = buf.readBlockPos().toCenterPos()
        client.world?.addParticle(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1.0, 1.0, 1.0)
    }
    ClientPlayNetworking.registerGlobalReceiver(RESULT_SHOWN_SHEEP_SHEARED_ID) { client, _, _, _ ->
        client.player?.playSound(
            SoundEvents.ENTITY_SHEEP_SHEAR,
            SoundCategory.AMBIENT,
            10F,
            1F
        )
    }
}