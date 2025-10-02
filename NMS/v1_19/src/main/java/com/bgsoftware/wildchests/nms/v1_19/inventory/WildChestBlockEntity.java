package com.bgsoftware.wildchests.nms.v1_19.inventory;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_19_R3.CraftParticle;

public class WildChestBlockEntity extends com.bgsoftware.wildchests.nms.v1_19.inventory.AbstractWildChestBlockEntity {

    public WildChestBlockEntity(Chest chest, ServerLevel serverLevel, BlockPos blockPos) {
        super(chest, serverLevel, blockPos);
    }

    @Override
    protected void sendParticles(Particle particle, double x, double y, double z) {
        this.serverLevel.sendParticles(null, CraftParticle.toNMS(particle),
                x, y, z, 0, 0.0, 0.0, 0.0, 1.0, false);
    }

}
