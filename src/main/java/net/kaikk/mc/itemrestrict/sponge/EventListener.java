package net.kaikk.mc.itemrestrict.sponge;

import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.world.chunk.LoadChunkEvent;

import net.kaikk.mc.itemrestrict.ChunkIdentifier;

public class EventListener {
	private BetterItemRestrict instance;
	
	public EventListener(BetterItemRestrict instance) {
		this.instance = instance;
	}
	
	@Listener
	public void onPlayerJoin(ClientConnectionEvent.Join event) {
		instance.inventoryCheck(event.getTargetEntity());
	}
	
	@Listener(beforeModifications = true, order=Order.FIRST)
	public void onBlockPlace(ChangeBlockEvent.Place event) {
		Player player = event.getCause().first(Player.class).orElse(null);
		if (instance.check(player, event.getTransactions().get(0).getFinal().getState())) {
			event.getTransactions().get(0).setValid(false);
			event.setCancelled(true);
		}
	}
	
	@Listener(beforeModifications = true, order=Order.FIRST)
	public void onPlayerInteract(InteractEvent event) {
		Player player = event.getCause().first(Player.class).orElse(null);
		if (player != null && instance.checkHands(player)) {
			event.setCancelled(true);
			instance.inventoryCheck(player);
		}
		
		if (event instanceof InteractBlockEvent) {
			InteractBlockEvent blockEvent = (InteractBlockEvent) event;
			if (blockEvent.getTargetBlock() != BlockSnapshot.NONE && instance.check(player, blockEvent.getTargetBlock().getExtendedState())) {
				event.setCancelled(true);
				instance.xsapi.setBlock(blockEvent.getTargetBlock().getLocation().get(), BlockTypes.AIR);
			}
		}
	}
	
	@Listener(beforeModifications = true, order=Order.FIRST)
	public void onItemClick(ClickInventoryEvent event) {
		Player player = event.getCause().first(Player.class).orElse(null);
		if (instance.ownershipCheck(player, event.getCursorTransaction().getOriginal().createStack()) || instance.ownershipCheck(player, event.getCursorTransaction().getFinal().createStack())) {
			event.getCursorTransaction().setValid(false);
			event.setCancelled(true);
		}
	}
	
	@Listener(order=Order.POST)
	public void onChunkLoad(LoadChunkEvent event) {
		final ChunkIdentifier chunkId = new ChunkIdentifier(event.getTargetChunk().getWorld().getUniqueId(), event.getTargetChunk().getPosition().getX(), event.getTargetChunk().getPosition().getZ());
		if (!instance.checkedChunks.contains(chunkId)) {
			instance.chunksToCheck.add(chunkId);
		}
	}
	
}
