package mandarin.card.supporter.holder.moderation

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu

class InventoryWipeHolder(author: Message, channelID: String, message: Message) : ComponentHolder(author, channelID, message) {
    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "user" -> {
                if (event !is EntitySelectInteractionEvent)
                    return

                val user = event.mentions.users.first().idLong

                registerPopUp(event, "Are you sure you want to wipe user <@$user>'s inventory? This cannot be undone", LangID.EN)

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    CardData.inventories.remove(user)

                    CardBot.saveCardData()

                    e.deferEdit()
                        .setContent("Successfully wiped <@$user>'s inventory!")
                        .setComponents()
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()
                }, LangID.EN))
            }
            "close" -> {
                expired = true

                event.deferEdit()
                    .setContent("Moderation tool closed")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            }
        }
    }

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onBack(event: GenericComponentInteractionCreateEvent, child: Holder) {
        applyResult(event)
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        return "Please select user to wipe their inventory"
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(
            EntitySelectMenu.create("user", EntitySelectMenu.SelectTarget.USER)
                .setPlaceholder("Select User To Wipe Inventory")
                .setRequiredRange(1, 1)
                .build()
        ))

        result.add(ActionRow.of(
            Button.danger("close", "Close").withEmoji(EmojiStore.CROSS)
        ))

        return result
    }
}