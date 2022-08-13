package com.bgsoftware.wildchests.nms.v1_18_R2.mappings.net.minecraft.server.network;

import com.bgsoftware.common.remaps.Remap;
import com.bgsoftware.wildchests.nms.v1_18_R2.mappings.MappedObject;
import net.minecraft.network.protocol.Packet;

public class PlayerConnection extends MappedObject<net.minecraft.server.network.PlayerConnection> {

    public PlayerConnection(net.minecraft.server.network.PlayerConnection handle) {
        super(handle);
    }

    @Remap(classPath = "net.minecraft.server.network.ServerGamePacketListenerImpl",
            name = "send",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public void send(Packet<?> packet) {
        handle.a(packet);
    }

}
