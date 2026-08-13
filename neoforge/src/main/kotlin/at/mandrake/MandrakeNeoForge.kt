package at.mandrake

import net.minecraft.ChatFormatting
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.SmithingTemplateItem
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.event.LootTableLoadEvent
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.RegisterEvent
import java.util.function.Supplier

@Mod(Mandrake.MOD_ID)
class MandrakeNeoForge(modBus: IEventBus) {

    private val items = DeferredRegister.createItems(Mandrake.MOD_ID)

    private val enderEyeTemplate: DeferredItem<SmithingTemplateItem> = items.register("ender_eye_smithing_template",
        Supplier { SmithingTemplateItem(
            Component.translatable("item.mandrake.ender_eye_smithing_template.applies_to"),
            Component.translatable("item.mandrake.ender_eye_smithing_template.ingredients"),
            Component.translatable("item.mandrake.ender_eye_smithing_template.upgrade_description").withStyle(ChatFormatting.GRAY),
            Component.translatable("item.mandrake.ender_eye_smithing_template.base_slot_description"),
            Component.translatable("item.mandrake.ender_eye_smithing_template.additions_slot_description"),
            listOf(ResourceLocation.withDefaultNamespace("item/empty_slot_emerald")),
            listOf(ResourceLocation.withDefaultNamespace("item/empty_slot_redstone_dust"))
        )}
    )

    init {
        items.register(modBus)

        modBus.addListener { event: RegisterEvent ->
            if (event.registryKey == Registries.ITEM) {
                Mandrake.ENDER_EYE_SMITHING_TEMPLATE = enderEyeTemplate.get()
            }
        }

        modBus.addListener(this::onCreativeTabContents)
        NeoForge.EVENT_BUS.addListener(this::onLootTableLoad)
    }

    private fun onCreativeTabContents(event: BuildCreativeModeTabContentsEvent) {
        when (event.tabKey) {
            CreativeModeTabs.INGREDIENTS -> event.accept(enderEyeTemplate.get())
        }
    }

    private fun onLootTableLoad(event: LootTableLoadEvent) {
        if (event.name == ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward_ominous")) {
            val pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(Mandrake.ENDER_EYE_SMITHING_TEMPLATE))
                .`when`(LootItemRandomChanceCondition.randomChance(0.1f))
            event.table.addPool(pool.build())
        } else if (event.name == ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward")) {
            val pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(Mandrake.ENDER_EYE_SMITHING_TEMPLATE))
                .`when`(LootItemRandomChanceCondition.randomChance(0.025f))
            event.table.addPool(pool.build())
        }
    }
}
