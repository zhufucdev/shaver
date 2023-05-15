package com.zhufucdev.shaver

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resource.ResourceManager
import net.minecraft.util.Identifier

object ResourceHandler : SimpleSynchronousResourceReloadListener {
    var mapping: Map<Int, String>? = null
        private set

    override fun reload(manager: ResourceManager) {
        val nametable =
            manager.findResources("nametable") { it.path.endsWith(".csv") }.values.firstOrNull()?.inputStream?.reader()
        mapping = nametable?.readText()?.split("\n")
            ?.associate { it.split(",").let { row -> row.first().toInt() to row[1] } }
        nametable?.close()
    }

    override fun getFabricId(): Identifier {
        return Identifier("shaver", "nametable")
    }
}