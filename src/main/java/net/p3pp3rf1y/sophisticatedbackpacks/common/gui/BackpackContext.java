package net.p3pp3rf1y.sophisticatedbackpacks.common.gui;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedbackpacks.SophisticatedBackpacks;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.api.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.NoopBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.network.SyncClientInfoMessage;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedbackpacks.util.WorldHelper;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public abstract class BackpackContext {
	public abstract Optional<IBackpackWrapper> getParentBackpackWrapper(Player player);

	public abstract boolean shouldLockBackpackSlot(Player player);

	public abstract IBackpackWrapper getBackpackWrapper(Player player);

	public abstract int getBackpackSlotIndex();

	public abstract BackpackContext getSubBackpackContext(int subBackpackSlotIndex);

	public abstract BackpackContext getParentBackpackContext();

	public abstract ContextType getType();

	public void toBuffer(FriendlyByteBuf packetBuffer) {
		getType().toBuffer(packetBuffer);
		addToBuffer(packetBuffer);
	}

	public abstract void addToBuffer(FriendlyByteBuf packetBuffer);

	public abstract boolean canInteractWith(Player player);

	public BlockPos getBackpackPosition(Player playerEntity) {
		return playerEntity.blockPosition();
	}

	public Component getDisplayName(Player player) {
		return getBackpackWrapper(player).getBackpack().getHoverName();
	}

	public abstract void onUpgradeChanged(Player player);

	public static BackpackContext fromBuffer(FriendlyByteBuf buffer) {
		ContextType type = ContextType.fromBuffer(buffer);
		switch (type) {
			case BLOCK_BACKPACK:
				return Block.fromBuffer(buffer);
			case BLOCK_SUB_BACKPACK:
				return BlockSubBackpack.fromBuffer(buffer);
			case ITEM_SUB_BACKPACK:
				return ItemSubBackpack.fromBuffer(buffer);
			case ITEM_BACKPACK:
			default:
				return Item.fromBuffer(buffer);
		}
	}

	public enum ContextType {
		BLOCK_BACKPACK(0, false),
		BLOCK_SUB_BACKPACK(1, true),
		ITEM_BACKPACK(2, false),
		ITEM_SUB_BACKPACK(3, true);

		private final int id;
		private final boolean isSubBackpack;

		ContextType(int id, boolean isSubBackpack) {
			this.id = id;
			this.isSubBackpack = isSubBackpack;
		}

		public void toBuffer(FriendlyByteBuf buffer) {
			buffer.writeShort(id);
		}

		private static final Map<Integer, ContextType> ID_CONTEXTS;

		public boolean isSubBackpack() {
			return isSubBackpack;
		}

		static {
			ImmutableMap.Builder<Integer, ContextType> builder = new ImmutableMap.Builder<>();
			for (ContextType value : ContextType.values()) {
				builder.put(value.id, value);
			}
			ID_CONTEXTS = builder.build();
		}

		public static ContextType fromBuffer(FriendlyByteBuf buffer) {
			return ID_CONTEXTS.getOrDefault((int) buffer.readShort(), ContextType.ITEM_BACKPACK);
		}
	}

	public static class Item extends BackpackContext {
		protected final String handlerName;
		protected final int backpackSlotIndex;

		public Item(String handlerName, int backpackSlotIndex) {
			this.handlerName = handlerName;
			this.backpackSlotIndex = backpackSlotIndex;
		}

		@Override
		public Optional<IBackpackWrapper> getParentBackpackWrapper(Player player) {
			return Optional.empty();
		}

		@Override
		public boolean shouldLockBackpackSlot(Player player) {
			return SophisticatedBackpacks.PROXY.getPlayerInventoryProvider().getPlayerInventoryHandler(player, handlerName).map(PlayerInventoryHandler::isVisibleInGui).orElse(false);
		}

		@Override
		public IBackpackWrapper getBackpackWrapper(Player player) {
			return SophisticatedBackpacks.PROXY.getPlayerInventoryProvider().getPlayerInventoryHandler(player, handlerName)
					.map(h -> h.getStackInSlot(player, backpackSlotIndex).getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElse(NoopBackpackWrapper.INSTANCE))
					.orElse(NoopBackpackWrapper.INSTANCE);
		}

		@Override
		public void onUpgradeChanged(Player player) {
			if (!player.level.isClientSide && handlerName.equals(PlayerInventoryProvider.MAIN_INVENTORY)) {
				IBackpackWrapper backpackWrapper = getBackpackWrapper(player);
				PacketHandler.sendToClient((ServerPlayer) player, new SyncClientInfoMessage(backpackSlotIndex, backpackWrapper.getRenderInfo().getNbt(), backpackWrapper.getColumnsTaken()));
			}
		}

		@Override
		public int getBackpackSlotIndex() {
			return backpackSlotIndex;
		}

		@Override
		public BackpackContext getSubBackpackContext(int subBackpackSlotIndex) {
			return new ItemSubBackpack(handlerName, backpackSlotIndex, subBackpackSlotIndex);
		}

		@Override
		public BackpackContext getParentBackpackContext() {
			return this;
		}

		@Override
		public ContextType getType() {
			return ContextType.ITEM_BACKPACK;
		}

		public static BackpackContext fromBuffer(FriendlyByteBuf packetBuffer) {
			return new BackpackContext.Item(packetBuffer.readUtf(), packetBuffer.readInt());
		}

		@Override
		public void addToBuffer(FriendlyByteBuf packetBuffer) {
			packetBuffer.writeUtf(handlerName);
			packetBuffer.writeInt(backpackSlotIndex);
		}

		@Override
		public boolean canInteractWith(Player player) {
			return true;
		}
	}

	public static class ItemSubBackpack extends Item {
		private final int subBackpackSlotIndex;
		@Nullable
		private IBackpackWrapper parentWrapper;

		public ItemSubBackpack(String handlerName, int backpackSlotIndex, int subBackpackSlotIndex) {
			super(handlerName, backpackSlotIndex);
			this.subBackpackSlotIndex = subBackpackSlotIndex;
		}

		@Override
		public Optional<IBackpackWrapper> getParentBackpackWrapper(Player player) {
			if (parentWrapper == null) {
				parentWrapper = super.getBackpackWrapper(player);
			}
			return Optional.of(parentWrapper);
		}

		@Override
		public IBackpackWrapper getBackpackWrapper(Player player) {
			return getParentBackpackWrapper(player).map(parent -> parent.getInventoryHandler().getStackInSlot(subBackpackSlotIndex).getCapability(CapabilityBackpackWrapper.getCapabilityInstance())
					.orElse(NoopBackpackWrapper.INSTANCE)).orElse(NoopBackpackWrapper.INSTANCE);
		}

		public static BackpackContext fromBuffer(FriendlyByteBuf packetBuffer) {
			return new BackpackContext.ItemSubBackpack(packetBuffer.readUtf(), packetBuffer.readInt(), packetBuffer.readInt());
		}

		@Override
		public void addToBuffer(FriendlyByteBuf packetBuffer) {
			super.addToBuffer(packetBuffer);
			packetBuffer.writeInt(subBackpackSlotIndex);
		}

		@Override
		public BackpackContext getParentBackpackContext() {
			return new BackpackContext.Item(handlerName, backpackSlotIndex);
		}

		@Override
		public ContextType getType() {
			return ContextType.ITEM_SUB_BACKPACK;
		}

		@Override
		public Component getDisplayName(Player player) {
			return new TextComponent("... > " + super.getDisplayName(player).getString());
		}

		@Override
		public void onUpgradeChanged(Player player) {
			//noop
		}
	}

	public static class Block extends BackpackContext {
		protected final BlockPos pos;

		public Block(BlockPos pos) {
			this.pos = pos;
		}

		@Override
		public BlockPos getBackpackPosition(Player playerEntity) {
			return pos;
		}

		@Override
		public void onUpgradeChanged(Player player) {
			if (!player.level.isClientSide) {
				WorldHelper.getTile(player.level, pos, BackpackBlockEntity.class).ifPresent(BackpackBlockEntity::refreshRenderState);
			}
		}

		@Override
		public Optional<IBackpackWrapper> getParentBackpackWrapper(Player player) {
			return Optional.empty();
		}

		@Override
		public boolean shouldLockBackpackSlot(Player player) {
			return false;
		}

		@Override
		public IBackpackWrapper getBackpackWrapper(Player player) {
			return WorldHelper.getTile(player.level, pos, BackpackBlockEntity.class).map(BackpackBlockEntity::getBackpackWrapper).orElse(NoopBackpackWrapper.INSTANCE);
		}

		@Override
		public int getBackpackSlotIndex() {
			return -1;
		}

		@Override
		public BackpackContext getSubBackpackContext(int subBackpackSlotIndex) {
			return new BlockSubBackpack(pos, subBackpackSlotIndex);
		}

		@Override
		public BackpackContext getParentBackpackContext() {
			return this;
		}

		public static BackpackContext fromBuffer(FriendlyByteBuf packetBuffer) {
			return new BackpackContext.Block(BlockPos.of(packetBuffer.readLong()));
		}

		@Override
		public void addToBuffer(FriendlyByteBuf packetBuffer) {
			packetBuffer.writeLong(pos.asLong());
		}

		@Override
		public boolean canInteractWith(Player player) {
			return player.level.getBlockEntity(pos) instanceof BackpackBlockEntity
					&& (player.distanceToSqr((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D) <= 64.0D);
		}

		@Override
		public ContextType getType() {
			return ContextType.BLOCK_BACKPACK;
		}
	}

	public static class BlockSubBackpack extends Block {
		private final int subBackpackSlotIndex;
		@Nullable
		private IBackpackWrapper parentWrapper;

		public BlockSubBackpack(BlockPos pos, int subBackpackSlotIndex) {
			super(pos);
			this.subBackpackSlotIndex = subBackpackSlotIndex;
		}

		@Override
		public Optional<IBackpackWrapper> getParentBackpackWrapper(Player player) {
			if (parentWrapper == null) {
				parentWrapper = super.getBackpackWrapper(player);
			}
			return Optional.of(parentWrapper);
		}

		@Override
		public IBackpackWrapper getBackpackWrapper(Player player) {
			return getParentBackpackWrapper(player).map(parent -> parent.getInventoryHandler().getStackInSlot(subBackpackSlotIndex).getCapability(CapabilityBackpackWrapper.getCapabilityInstance())
					.orElse(NoopBackpackWrapper.INSTANCE)).orElse(NoopBackpackWrapper.INSTANCE);
		}

		public static BackpackContext fromBuffer(FriendlyByteBuf packetBuffer) {
			return new BackpackContext.BlockSubBackpack(BlockPos.of(packetBuffer.readLong()), packetBuffer.readInt());
		}

		@Override
		public void addToBuffer(FriendlyByteBuf packetBuffer) {
			super.addToBuffer(packetBuffer);
			packetBuffer.writeInt(subBackpackSlotIndex);
		}

		@Override
		public BackpackContext getParentBackpackContext() {
			return new BackpackContext.Block(pos);
		}

		@Override
		public ContextType getType() {
			return ContextType.BLOCK_SUB_BACKPACK;
		}

		@Override
		public Component getDisplayName(Player player) {
			return new TextComponent("... > " + super.getDisplayName(player).getString());
		}

		@Override
		public void onUpgradeChanged(Player player) {
			//noop
		}
	}
}
