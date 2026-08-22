package at.xbce

import at.xbce.entity.FakeIronGolemEntity
import at.xbce.entity.FakeSnowGolemEntity
import at.xbce.entity.FakeVillagerEntity
import at.xbce.item.RevealSpyglassItem
import net.minecraft.ChatFormatting
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
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
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.RegisterEvent
import java.util.function.Supplier

@Mod(XBCE.MOD_ID)
class XBCENeoForge(modBus: IEventBus) {

    private val items = DeferredRegister.createItems(XBCE.MOD_ID)
    private val entities = DeferredRegister.create(Registries.ENTITY_TYPE, XBCE.MOD_ID)

    private val revealSpyglass: DeferredItem<RevealSpyglassItem> = items.register("reveal_spyglass",
        Supplier { RevealSpyglassItem(Item.Properties().stacksTo(1)) }
    )

    private val enderEyeTemplate: DeferredItem<SmithingTemplateItem> = items.register("ender_eye_smithing_template",
        Supplier { SmithingTemplateItem(
            Component.translatable("item.xbce.ender_eye_smithing_template.applies_to"),
            Component.translatable("item.xbce.ender_eye_smithing_template.ingredients"),
            Component.translatable("item.xbce.ender_eye_smithing_template.upgrade_description").withStyle(ChatFormatting.GRAY),
            Component.translatable("item.xbce.ender_eye_smithing_template.base_slot_description"),
            Component.translatable("item.xbce.ender_eye_smithing_template.additions_slot_description"),
            listOf(ResourceLocation.withDefaultNamespace("item/empty_slot_emerald")),
            listOf(ResourceLocation.withDefaultNamespace("item/empty_slot_redstone_dust"))
        )}
    )

    private val fakeSnowGolem = entities.register("fake_snow_golem",
        Supplier { EntityType.Builder.of({ type, level -> FakeSnowGolemEntity(type, level) }, MobCategory.MONSTER)
            .sized(0.7f, 1.9f)
            .clientTrackingRange(8)
            .build("fake_snow_golem")
        }
    )

    private val fakeVillager = entities.register("fake_villager",
        Supplier { EntityType.Builder.of({ type, level -> FakeVillagerEntity(type, level) }, MobCategory.CREATURE)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(8)
            .build("fake_villager")
        }
    )

    private val fakeIronGolem = entities.register("fake_iron_golem",
        Supplier { EntityType.Builder.of({ type, level -> FakeIronGolemEntity(type, level) }, MobCategory.CREATURE)
            .sized(1.4f, 2.7f)
            .clientTrackingRange(10)
            .build("fake_iron_golem")
        }
    )

    init {
        items.register(modBus)
        entities.register(modBus)

        modBus.addListener { event: RegisterEvent ->
            when (event.registryKey) {
                Registries.ITEM -> {
                    XBCE.ENDER_EYE_SMITHING_TEMPLATE = enderEyeTemplate.get()
                    XBCE.REVEAL_SPYGLASS = revealSpyglass.get()
                }
                Registries.ENTITY_TYPE -> {
                    XBCE.FAKE_SNOW_GOLEM = fakeSnowGolem.get()
                    XBCE.FAKE_VILLAGER = fakeVillager.get()
                    XBCE.FAKE_IRON_GOLEM = fakeIronGolem.get()
                }
            }
        }

        modBus.addListener(this::onCreativeTabContents)
        modBus.addListener(this::onEntityAttributeCreation)
        modBus.addListener(at.xbce.client.XBCENeoForgeClient::onRegisterRenderers)
        modBus.addListener(at.xbce.client.XBCENeoForgeClient::onRegisterLayerDefinitions)
        NeoForge.EVENT_BUS.addListener(this::onLootTableLoad)
        NeoForge.EVENT_BUS.addListener { event: net.neoforged.neoforge.event.entity.living.LivingDeathEvent ->
            at.xbce.XBCEAdvancements.onDeath(event.entity, event.source)
        }
    }

    private fun onCreativeTabContents(event: BuildCreativeModeTabContentsEvent) {
        when (event.tabKey) {
            CreativeModeTabs.INGREDIENTS -> event.accept(enderEyeTemplate.get())
            CreativeModeTabs.TOOLS_AND_UTILITIES -> event.accept(revealSpyglass.get())
        }
    }

    private fun onEntityAttributeCreation(event: EntityAttributeCreationEvent) {
        event.put(XBCE.FAKE_SNOW_GOLEM, FakeSnowGolemEntity.createAttributes().build())
        event.put(XBCE.FAKE_VILLAGER, FakeVillagerEntity.createAttributes().build())
        event.put(XBCE.FAKE_IRON_GOLEM, FakeIronGolemEntity.createAttributes().build())
    }

    private fun onLootTableLoad(event: LootTableLoadEvent) {
        if (event.name == ResourceLocation.withDefaultNamespace("chests/ancient_city")) {
            val pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(XBCE.ENDER_EYE_SMITHING_TEMPLATE))
                .`when`(LootItemRandomChanceCondition.randomChance(0.08f))
            event.table.addPool(pool.build())
        } else if (event.name == ResourceLocation.withDefaultNamespace("chests/ancient_city_ice_box")) {
            val pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(XBCE.ENDER_EYE_SMITHING_TEMPLATE))
                .`when`(LootItemRandomChanceCondition.randomChance(0.08f))
            event.table.addPool(pool.build())
        }
    }
}
