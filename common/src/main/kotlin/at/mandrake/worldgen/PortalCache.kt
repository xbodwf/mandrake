package at.mandrake.worldgen

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.level.saveddata.SavedData

class PortalCache : SavedData() {
    data class Entry(
        val overMin: BlockPos,
        val overMax: BlockPos,
        val coreMin: BlockPos,
        val coreMax: BlockPos
    )

    private val entries = ArrayList<Entry>()

    fun store(overMin: BlockPos, overMax: BlockPos, coreMin: BlockPos, coreMax: BlockPos) {
        entries.add(Entry(overMin, overMax, coreMin, coreMax))
        setDirty()
    }

    fun findEntry(pos: BlockPos): Entry? {
        for (entry in entries) {
            if (pos.x in entry.overMin.x..entry.overMax.x &&
                pos.z in entry.overMin.z..entry.overMax.z &&
                pos.y == entry.overMin.y
            ) return entry
            if (pos.x in entry.coreMin.x..entry.coreMax.x &&
                pos.z in entry.coreMin.z..entry.coreMax.z &&
                pos.y == entry.coreMin.y
            ) return entry
            if (pos.y == entry.overMin.y &&
                (pos.x == entry.overMin.x - 1 || pos.x == entry.overMax.x + 1 ||
                 pos.z == entry.overMin.z - 1 || pos.z == entry.overMax.z + 1) &&
                pos.x in entry.overMin.x - 1..entry.overMax.x + 1 &&
                pos.z in entry.overMin.z - 1..entry.overMax.z + 1 &&
                !isCorner(pos, entry.overMin.x - 1, entry.overMax.x + 1, entry.overMin.z - 1, entry.overMax.z + 1)
            ) return entry
            if (pos.y == entry.coreMin.y &&
                (pos.x == entry.coreMin.x - 1 || pos.x == entry.coreMax.x + 1 ||
                 pos.z == entry.coreMin.z - 1 || pos.z == entry.coreMax.z + 1) &&
                pos.x in entry.coreMin.x - 1..entry.coreMax.x + 1 &&
                pos.z in entry.coreMin.z - 1..entry.coreMax.z + 1 &&
                !isCorner(pos, entry.coreMin.x - 1, entry.coreMax.x + 1, entry.coreMin.z - 1, entry.coreMax.z + 1)
            ) return entry
        }
        return null
    }

    private fun isCorner(pos: BlockPos, minX: Int, maxX: Int, minZ: Int, maxZ: Int): Boolean {
        return (pos.x == minX && pos.z == minZ) || (pos.x == minX && pos.z == maxZ) ||
               (pos.x == maxX && pos.z == minZ) || (pos.x == maxX && pos.z == maxZ)
    }

    fun removeEntry(entry: Entry) {
        entries.remove(entry)
        setDirty()
    }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val list = ListTag()
        for (entry in entries) {
            val t = CompoundTag()
            t.put("OverMin", writePos(entry.overMin))
            t.put("OverMax", writePos(entry.overMax))
            t.put("CoreMin", writePos(entry.coreMin))
            t.put("CoreMax", writePos(entry.coreMax))
            list.add(t)
        }
        tag.put("Portals", list)
        return tag
    }

    companion object {
        private const val DATA_NAME = "mandrake_portal_cache"

        fun getOrCreate(overworld: ServerLevel): PortalCache {
            val factory = SavedData.Factory(
                { PortalCache() },
                { tag, _ -> load(tag) },
                DataFixTypes.LEVEL
            )
            return overworld.dataStorage.computeIfAbsent(factory, DATA_NAME)
        }

        private fun load(tag: CompoundTag): PortalCache {
            val cache = PortalCache()
            val list = tag.getList("Portals", Tag.TAG_COMPOUND.toInt())
            for (i in 0 until list.size) {
                val t = list.getCompound(i)
                cache.entries.add(Entry(
                    readPos(t.getCompound("OverMin")),
                    readPos(t.getCompound("OverMax")),
                    readPos(t.getCompound("CoreMin")),
                    readPos(t.getCompound("CoreMax"))
                ))
            }
            return cache
        }

        private fun writePos(pos: BlockPos): CompoundTag {
            val t = CompoundTag()
            t.putInt("X", pos.x)
            t.putInt("Y", pos.y)
            t.putInt("Z", pos.z)
            return t
        }

        private fun readPos(tag: CompoundTag): BlockPos =
            BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"))
    }
}
