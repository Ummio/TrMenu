package trplugins.menu.api.receptacle.vanilla.window

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack
import taboolib.module.nms.*
import taboolib.platform.util.isAir
import trplugins.menu.api.receptacle.vanilla.window.StaticInventory.staticInventory
import trplugins.menu.api.receptacle.vanilla.window.StaticInventory.inventoryView

class NMSImpl : NMS() {

    private val emptyItemStack = ItemStack(Material.AIR)
    private val version = MinecraftVersion.major
    private val windowIds = HashMap<String, Int>()

    private val Player.windowId get() = windowIds[name] ?: 119

    override fun windowId(player: Player, create: Boolean): Int {
        if (createWindowId() && create) {
            val id = nmsProxy<Int>("getContainerCounter", player) as Int
            nmsProxy<Unit>("setContainerCounter", player, id)
            windowIds[player.name] = id
        }
        return player.windowId
    }

    override fun sendWindowsClose(player: Player, windowId: Int) {
        if (player.useStaticInventory()) {
            StaticInventory.close(player)
        } else {
            windowIds.remove(player.name)
            nmsProxy<Unit>("sendCloseWindow", player, windowId)
        }
    }

    override fun sendWindowsItems(player: Player, windowId: Int, items: Array<ItemStack?>) {
        when {
            player.useStaticInventory() -> {
                val inventory = player.staticInventory!!
                items.forEachIndexed { index, item ->
                    if (index >= inventory.size) {
                        return
                    }
                    inventory.setItem(index, item)
                }
            }
            else -> {
                nmsProxy<Unit>("sendWindowItems", player, windowId, items.map { it as Any? }.toTypedArray())
            }
        }
    }

    override fun sendWindowsOpen(player: Player, windowId: Int, type: WindowLayout, title: String) {
        when {
            player.useStaticInventory() -> {
                StaticInventory.open(player, type, title)
            }
            else -> {
                nmsProxy<Unit>("sendOpenWindow", player, windowId, type.vanillaId, title)
            }
        }
    }

    override fun sendWindowsSetSlot(player: Player, windowId: Int, slot: Int, itemStack: ItemStack?, stateId: Int) {
        when {
            player.useStaticInventory() -> {
                if (windowId == -1 && slot == -1) {
                    player.itemOnCursor.type = Material.AIR
                } else {
                    val inventory = player.staticInventory!!
                    if (slot >= 0 && slot < inventory.size) {
                        inventory.setItem(slot, itemStack)
                    }
                }
            }
            else -> {
                nmsProxy<Unit>("sendSetSlot", player, windowId, slot, itemStack as Any?, stateId)
            }
        }
    }

    override fun sendWindowsUpdateData(player: Player, windowId: Int, id: Int, value: Int) {
        when {
            player.useStaticInventory() -> {
                val inventory = player.staticInventory!!
                val view = player.inventoryView!!
                val property = getInventoryProperty(inventory.type, id) ?: return
                view.setProperty(property, value)
            }
            else -> {
                nmsProxy<Unit>("sendWindowData", player, windowId, id, value)
            }
        }
    }

    private fun getInventoryProperty(type: InventoryType, id: Int): InventoryView.Property? {
        return InventoryView.Property.entries.find { (it.type == type || (it.type == InventoryType.FURNACE && type == InventoryType.BLAST_FURNACE)) && it.id == id }
    }
}