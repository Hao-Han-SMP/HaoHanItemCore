package vn.haohan.itemcore.block;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.haohan.itemcore.internal.block.CustomBlockPacketListener;
import vn.haohan.itemcore.internal.block.CustomBlockVisibilityManager;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CustomBlockPacketListenerTest {

    private CustomBlockVisibilityManager visibilityManager;
    private CustomBlockPacketListener packetListener;
    private Player mockPlayer;

    @BeforeEach
    public void setUp() {
        visibilityManager = mock(CustomBlockVisibilityManager.class);
        packetListener = new CustomBlockPacketListener(visibilityManager, Logger.getLogger("TestListener"));
        mockPlayer = mock(Player.class);
    }

    @Test
    public void testIgnoresNonChunkPackets() {
        PacketSendEvent event = mock(PacketSendEvent.class);
        when(event.getPacketType()).thenReturn(PacketType.Play.Server.PING);
        when(event.getPlayer()).thenReturn(mockPlayer);

        packetListener.onPacketSend(event);

        verifyNoInteractions(visibilityManager);
    }

    @Test
    public void testNullEventHandledGracefully() {
        assertDoesNotThrow(() -> packetListener.onPacketSend(null));
        verifyNoInteractions(visibilityManager);

        PacketSendEvent event = mock(PacketSendEvent.class);
        when(event.getPacketType()).thenReturn(null);
        assertDoesNotThrow(() -> packetListener.onPacketSend(event));
        verifyNoInteractions(visibilityManager);
    }

    @Test
    public void testNonPlayerHandledGracefully() {
        PacketSendEvent event = mock(PacketSendEvent.class);
        when(event.getPacketType()).thenReturn(PacketType.Play.Server.CHUNK_DATA);
        when(event.getPlayer()).thenReturn(new Object()); // Not a Player

        assertDoesNotThrow(() -> packetListener.onPacketSend(event));
        verifyNoInteractions(visibilityManager);
    }
}

