package mandarin.card.supporter.holder.moderation

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.CraftCostModifyHolder
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal

class CraftCostHolder(author: Message, channelID: String, message: Message) : ComponentHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val size = 2

    private var page = 0

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Craft cost config expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "prev" -> {
                page--

                applyResult(event)
            }
            "next" -> {
                page++

                applyResult(event)
            }
            "t2",
            "seasonal",
            "collab",
            "t3",
            "t4"-> {
                val craftMode = when(event.componentId) {
                    "t2" -> CardData.CraftMode.T2
                    "seasonal" -> CardData.CraftMode.SEASONAL
                    "collab" -> CardData.CraftMode.COLLAB
                    "t3" -> CardData.CraftMode.T3
                    else -> CardData.CraftMode.T4
                }

                val input = TextInput.create("cost", "Cost", TextInputStyle.SHORT)
                    .setPlaceholder("Define amount of platinum shards that will be spent upon craft")
                    .setRequired(true)
                    .setValue(craftMode.cost.toString())
                    .build()

                val modal = Modal.create("craftCost", "Cost of Crafting")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                StaticStore.putHolder(authorMessage.author.id, CraftCostModifyHolder(authorMessage, channelID, message, craftMode) {
                    applyResult()
                })
            }
            "confirm" -> {
                event.deferEdit()
                    .setContent("Confirmed modification!")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                end()
            }
        }
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult() {
        message.editMessage(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContent() : String {
        return "Select button to adjust the amount of ${EmojiStore.ABILITY["SHARD"]?.formatted} platinum shards that will be spent after crafting\n" +
                "\n" +
                "### Craft cost\n" +
                "\n" +
                "- Regular Tier 2 [Uncommon] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.CraftMode.T2.cost}\n" +
                "- Seasonal Tier 2 [Uncommon] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.CraftMode.SEASONAL.cost}\n" +
                "- Collaboration Tier 2 [Uncommon] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.CraftMode.COLLAB.cost}\n" +
                "- Tier 3 [Ultra Rare (Exclusives)] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.CraftMode.T3.cost}\n" +
                "- Tier 4 [Legend Rare] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.CraftMode.T4.cost}\n" +
                "\n" +
                "Page : ${page + 1} / $size"
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        when (page) {
            0 -> {
                result.add(ActionRow.of(Button.secondary("t2", "Regular Tier 2 [Uncommon]")))
                result.add(ActionRow.of(Button.secondary("seasonal", "Seasonal Tier 2 [Uncommon]")))
                result.add(ActionRow.of(Button.secondary("collab", "Collaboration Tier 2 [Uncommon]")))
            }
            1 -> {
                result.add(ActionRow.of(Button.secondary("t3", "Tier 3 [Ultra Rare (Exclusives)]")))
                result.add(ActionRow.of(Button.secondary("t4", "Tier 4 [Legend Rare]")))
            }
        }

        result.add(
            ActionRow.of(
                Button.secondary("prev", "Previous").withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0),
                Button.secondary("next", "Next").withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= size)
            )
        )

        result.add(ActionRow.of(Button.primary("confirm", "Confirm")))

        return result
    }
}