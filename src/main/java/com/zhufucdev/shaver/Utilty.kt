package com.zhufucdev.shaver

import net.minecraft.entity.mob.MobEntity

fun MobEntity.shaverGrade() = ServerState.state.shaverGrades.getOrDefault(uuid, 0)