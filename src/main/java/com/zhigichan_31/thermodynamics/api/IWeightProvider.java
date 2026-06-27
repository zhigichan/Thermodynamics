package com.zhigichan_31.thermodynamics.api;

import net.minecraft.world.entity.player.Player;

public interface IWeightProvider {
    // Возвращает чистый вес предметов из кастомных слотов
    float getTotalWeight(Player player);

    // Позволяет аддону напрямую влиять на множитель траты выносливости
    float getStaminaDrainModifier(Player player);
}
