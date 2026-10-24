package vn.haohan.itemcore.internal.block;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.sound.Sound;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class CustomBlockSoundPacketListenerTest {

    @Test
    void testRecentPlacementTracking() {
        CustomBlockSoundPacketListener listener = new CustomBlockSoundPacketListener();
        assertFalse(listener.isRecentPlacement(10, 64, -20));

        listener.markRecentPlacement(10, 64, -20);
        assertTrue(listener.isRecentPlacement(10, 64, -20));

        listener.clearRecentPlacement(10, 64, -20);
        assertFalse(listener.isRecentPlacement(10, 64, -20));
    }

    @Test
    void testRecentBreakTracking() {
        CustomBlockSoundPacketListener listener = new CustomBlockSoundPacketListener();
        assertFalse(listener.isRecentBreak(15, 70, -30));

        listener.markRecentBreak(15, 70, -30);
        assertTrue(listener.isRecentBreak(15, 70, -30));

        listener.clearRecentBreak(15, 70, -30);
        assertFalse(listener.isRecentBreak(15, 70, -30));
    }

    @Test
    void testCustomBreakSoundIsNotCancelledAtRecentBreak() {
        CustomBlockSoundPacketListener listener = new CustomBlockSoundPacketListener();
        listener.markRecentBreak(10, 64, 20);

        // Sound effect with "minecraft:block.stone.break"
        PacketSendEvent event = Mockito.mock(PacketSendEvent.class);
        Mockito.when(event.getPacketType()).thenReturn(PacketType.Play.Server.SOUND_EFFECT);

        Sound sound = Mockito.mock(Sound.class);
        Mockito.when(sound.getName()).thenReturn(new ResourceLocation("minecraft", "block.stone.break"));

        WrapperPlayServerSoundEffect wrapper = Mockito.mock(WrapperPlayServerSoundEffect.class);
        Mockito.when(wrapper.getPosition()).thenReturn(new Vector3d(10.5, 64.5, 20.5));
        Mockito.when(wrapper.getSound()).thenReturn(sound);

        listener.handleSoundEffectPacket(event, wrapper);
        Mockito.verify(event, Mockito.never()).setCancelled(true);
    }

    @Test
    void testCustomWoodBreakSoundAllowedIsNotCancelled() {
        CustomBlockSoundPacketListener listener = new CustomBlockSoundPacketListener();
        listener.markRecentBreak(10, 64, 20, "haohan:block.wood_crate.break");

        PacketSendEvent event = Mockito.mock(PacketSendEvent.class);
        Mockito.when(event.getPacketType()).thenReturn(PacketType.Play.Server.SOUND_EFFECT);

        Sound sound = Mockito.mock(Sound.class);
        Mockito.when(sound.getName()).thenReturn(new ResourceLocation("haohan", "block.wood_crate.break"));

        WrapperPlayServerSoundEffect wrapper = Mockito.mock(WrapperPlayServerSoundEffect.class);
        Mockito.when(wrapper.getPosition()).thenReturn(new Vector3d(10.5, 64.5, 20.5));
        Mockito.when(wrapper.getSound()).thenReturn(sound);

        listener.handleSoundEffectPacket(event, wrapper);
        Mockito.verify(event, Mockito.never()).setCancelled(true);
    }

    @Test
    void testVanillaWoodAndNoteBlockSoundsAreCancelledAtRecentBreak() {
        CustomBlockSoundPacketListener listener = new CustomBlockSoundPacketListener();
        listener.markRecentBreak(10, 64, 20);

        // 1. Vanilla wood break sound
        PacketSendEvent event1 = Mockito.mock(PacketSendEvent.class);
        Mockito.when(event1.getPacketType()).thenReturn(PacketType.Play.Server.SOUND_EFFECT);

        Sound sound1 = Mockito.mock(Sound.class);
        Mockito.when(sound1.getName()).thenReturn(new ResourceLocation("minecraft", "block.wood.break"));

        WrapperPlayServerSoundEffect wrapper1 = Mockito.mock(WrapperPlayServerSoundEffect.class);
        Mockito.when(wrapper1.getPosition()).thenReturn(new Vector3d(10.5, 64.5, 20.5));
        Mockito.when(wrapper1.getSound()).thenReturn(sound1);

        listener.handleSoundEffectPacket(event1, wrapper1);
        Mockito.verify(event1).setCancelled(true);

        // 2. NoteBlock sound
        PacketSendEvent event2 = Mockito.mock(PacketSendEvent.class);
        Mockito.when(event2.getPacketType()).thenReturn(PacketType.Play.Server.SOUND_EFFECT);

        Sound sound2 = Mockito.mock(Sound.class);
        Mockito.when(sound2.getName()).thenReturn(new ResourceLocation("minecraft", "block.note_block.harp"));

        WrapperPlayServerSoundEffect wrapper2 = Mockito.mock(WrapperPlayServerSoundEffect.class);
        Mockito.when(wrapper2.getPosition()).thenReturn(new Vector3d(10.5, 64.5, 20.5));
        Mockito.when(wrapper2.getSound()).thenReturn(sound2);

        listener.handleSoundEffectPacket(event2, wrapper2);
        Mockito.verify(event2).setCancelled(true);
    }

    @Test
    void testEntitySoundEffectWoodCancelledAtRecentBreak() {
        CustomBlockSoundPacketListener listener = new CustomBlockSoundPacketListener();
        listener.markRecentBreak(10, 64, 20);

        PacketSendEvent event = Mockito.mock(PacketSendEvent.class);
        Mockito.when(event.getPacketType()).thenReturn(PacketType.Play.Server.ENTITY_SOUND_EFFECT);

        Sound sound = Mockito.mock(Sound.class);
        Mockito.when(sound.getName()).thenReturn(new ResourceLocation("minecraft", "block.wood.break"));

        com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntitySoundEffect wrapper =
                Mockito.mock(com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntitySoundEffect.class);
        Mockito.when(wrapper.getSound()).thenReturn(sound);

        listener.handleEntitySoundEffectPacket(event, wrapper);
        Mockito.verify(event).setCancelled(true);
    }
}
