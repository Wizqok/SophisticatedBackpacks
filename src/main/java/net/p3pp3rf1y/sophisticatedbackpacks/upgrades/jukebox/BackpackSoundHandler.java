package net.p3pp3rf1y.sophisticatedbackpacks.upgrades.jukebox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.network.PacketHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BackpackSoundHandler {
	private static final int SOUND_STOP_CHECK_INTERVAL = 10;

	private BackpackSoundHandler() {}

	private static final Map<UUID, SoundInstance> backpackSounds = new ConcurrentHashMap<>();
	private static long lastPlaybackChecked = 0;

	public static void playBackpackSound(UUID backpackUuid, SoundInstance sound) {
		stopBackpackSound(backpackUuid);
		backpackSounds.put(backpackUuid, sound);
		Minecraft.getInstance().getSoundManager().play(sound);
	}

	public static void stopBackpackSound(UUID backpackUuid) {
		if (backpackSounds.containsKey(backpackUuid)) {
			Minecraft.getInstance().getSoundManager().stop(backpackSounds.remove(backpackUuid));
			PacketHandler.sendToServer(new SoundStopNotificationMessage(backpackUuid));
		}
	}

	public static void tick(TickEvent.WorldTickEvent event) {
		if (!backpackSounds.isEmpty() && lastPlaybackChecked < event.world.getGameTime() - SOUND_STOP_CHECK_INTERVAL) {
			lastPlaybackChecked = event.world.getGameTime();
			backpackSounds.entrySet().removeIf(entry -> {
				if (!Minecraft.getInstance().getSoundManager().isActive(entry.getValue())) {
					PacketHandler.sendToServer(new SoundStopNotificationMessage(entry.getKey()));
					return true;
				}
				return false;
			});
		}
	}

	public static void playBackpackSound(SoundEvent soundEvent, UUID backpackUuid, BlockPos pos) {
		playBackpackSound(backpackUuid, SimpleSoundInstance.forRecord(soundEvent, pos.getX(), pos.getY(), pos.getZ()));
	}

	public static void playBackpackSound(SoundEvent soundEvent, UUID backpackUuid, int entityId) {
		ClientLevel world = Minecraft.getInstance().level;
		if (world == null) {
			return;
		}

		Entity entity = world.getEntity(entityId);
		if (!(entity instanceof LivingEntity)) {
			return;
		}
		playBackpackSound(backpackUuid, new EntityBoundSoundInstance(soundEvent, SoundSource.RECORDS, 2, 1, entity));
	}

	@SuppressWarnings({"unused", "java:S1172"}) // needs to be here for addListener to recognize which event this method should be subscribed to
	public static void onWorldUnload(WorldEvent.Unload evt) {
		backpackSounds.clear();
		lastPlaybackChecked = 0;
	}
}
