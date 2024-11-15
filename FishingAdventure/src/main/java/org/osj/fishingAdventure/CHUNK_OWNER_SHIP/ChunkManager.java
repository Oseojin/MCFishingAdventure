package org.osj.fishingAdventure.CHUNK_OWNER_SHIP;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.osj.fishingAdventure.CHUNK_OWNER_SHIP.CHAT.MessageManager;
import org.osj.fishingAdventure.FishingAdventure;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class ChunkManager implements Listener
{
    public static HashMap<Player, Chunk> purchaseWaitList = new HashMap<>();
    public static HashMap<Player, Chunk> removeWaitList = new HashMap<>();
    public static HashMap<Player, Chunk> currentPlayerChunk = new HashMap<>();
    FileConfiguration chunkConfig = FishingAdventure.getConfigManager().getConfig("chunkownership");
    private final HashMap<UUID, List<Long>> chunkMasterDataMap = new HashMap<>();
    private final HashMap<UUID, List<String>> chunkAllowedDataMap = new HashMap<>();

    public ChunkManager()
    {
        if(chunkConfig.getConfigurationSection("chunks.master.") != null)
        {
            List<String> configUUIDKeyList = chunkConfig.getConfigurationSection("chunks.master.").getKeys(true).stream().toList();
            configUUIDKeyList.forEach((key) -> {
                chunkMasterDataMap.put(UUID.fromString(key), chunkConfig.getLongList("chunks.master." + key));
            });
        }
        if(chunkConfig.getConfigurationSection("chunks.allow.") != null)
        {
            List<String> configFriendKeyList = chunkConfig.getConfigurationSection("chunks.allow.").getKeys(true).stream().toList();
            configFriendKeyList.forEach((key) -> {
                chunkAllowedDataMap.put(UUID.fromString(key), chunkConfig.getStringList("chunks.allow." + key));
            });
        }
    }

    @EventHandler
    public void onPlayerEnterChunk(PlayerMoveEvent event)
    {
        Player player = event.getPlayer();
        Chunk chunk = player.getChunk();

        if(currentPlayerChunk.get(player) != null && currentPlayerChunk.get(player).equals(chunk))
        {
            return;
        }
        currentPlayerChunk.put(player, chunk);

        if(!isOwnerless(chunk.getChunkKey()))
        {
            Player masterPlayer = FishingAdventure.getServerInstance().getServer().getPlayer(whosChunk(chunk.getChunkKey()));
            MessageManager.SendTitle(player, masterPlayer.getName(), "", TextColor.color(0, 255, 0), TextColor.color(0, 0 ,0));
        }
    }

    public void addMyChunk(UUID uuid, Chunk chunk)
    {
        boolean isOwnerless = isOwnerless(chunk.getChunkKey());
        if(isOwnerless)
        {
            if(chunkMasterDataMap.get(uuid) == null)
            {
                List<Long> newChunkList = new LinkedList<>();
                newChunkList.add(chunk.getChunkKey());
                chunkMasterDataMap.put(uuid, newChunkList);
            }
            else
            {
                chunkMasterDataMap.get(uuid).add(chunk.getChunkKey());
            }

            chunkConfig.set("chunks.master." + uuid, chunkMasterDataMap.get(uuid));
            FishingAdventure.getConfigManager().saveConfig("chunkownership");
        }
    }

    public void removeMyChunk(UUID uuid, Chunk chunk)
    {
        chunkMasterDataMap.get(uuid).remove(chunk.getChunkKey());
        chunkConfig.set("chunks.master." + uuid, chunkMasterDataMap.get(uuid));
        FishingAdventure.getConfigManager().saveConfig("chunkownership");
    }

    public void addFriendChunk(UUID friend, UUID master)
    {
        Player player = FishingAdventure.getServerInstance().getServer().getPlayer(master);
        if(chunkAllowedDataMap.get(friend) == null)
        {
            List<String> newFriendList = new LinkedList<>();
            newFriendList.add(master.toString());
            chunkAllowedDataMap.put(friend, newFriendList);
        }
        else if(chunkAllowedDataMap.get(friend).contains(master.toString()))
        {
            MessageManager.SendChatForm(player);
            MessageManager.SendChatContent(player, "이미 허용된 플레이어 입니다.", TextColor.color(255, 0, 0));
            MessageManager.SendChatForm(player);
            return;
        }
        else
        {
            chunkAllowedDataMap.get(friend).add(master.toString());
        }

        chunkConfig.set("chunks.allow." + friend, chunkAllowedDataMap.get(friend));
        FishingAdventure.getConfigManager().saveConfig("chunkownership");
    }

    public void removeFriendChunk(UUID friend, UUID master)
    {
        Player player = FishingAdventure.getServerInstance().getServer().getPlayer(master);
        if(chunkAllowedDataMap.get(friend) == null || !chunkAllowedDataMap.get(friend).contains(master.toString()))
        {
            MessageManager.SendChatForm(player);
            MessageManager.SendChatContent(player, "추가되어 있지 않은 플레이어입니다!", TextColor.color(255, 0, 0));
            MessageManager.SendChatForm(player);
            return;
        }

        chunkAllowedDataMap.get(friend).remove(master.toString());
        chunkConfig.set("chunks.allow." + friend, chunkAllowedDataMap.get(friend));
        FishingAdventure.getConfigManager().saveConfig("chunkownership");
    }

    public boolean isContainMasterMap(UUID uuid)
    {
        return chunkMasterDataMap.containsKey(uuid);
    }
    public boolean isMyChunk(UUID uuid, Chunk chunk)
    {
        return chunkMasterDataMap.containsKey(uuid) && chunkMasterDataMap.get(uuid).contains(chunk.getChunkKey());
    }

    public boolean isMyFriendChunk(UUID friendUUID, UUID masterUUID)
    {
        return chunkAllowedDataMap.containsKey(friendUUID) && chunkAllowedDataMap.get(friendUUID).contains(masterUUID.toString());
    }

    public boolean isOwnerless(Long chunkKey)
    {
        for(UUID key : chunkMasterDataMap.keySet())
        {
            if(chunkMasterDataMap.get(key).contains(chunkKey))
            {
                return false;
            }
        }
        return true;
    }
    public UUID whosChunk(Long chunkKey)
    {
        for(UUID key : chunkMasterDataMap.keySet())
        {
            if(chunkMasterDataMap.get(key).contains(chunkKey))
            {
                return key;
            }
        }
        return null;
    }
}