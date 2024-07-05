package mandarin.card.supporter.holder.skin

import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Skin
import mandarin.card.supporter.holder.CardCostPayHolder
import mandarin.card.supporter.pack.CardPayContainer
import mandarin.card.supporter.pack.SpecificCardCost
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.utils.FileUpload

class SkinPurchasePayHolder(author: Message, channelID: String, private var message: Message, private val skin: Skin) : ComponentHolder(author, channelID, message) {
    val inventory = Inventory.getInventory(author.author.idLong)

    private val containers = Array(skin.cost.cardsCosts.size) {
        CardPayContainer(skin.cost.cardsCosts[it])
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "purchase" -> {
                var futureWarn = false
                val warn = if (!inventory.cards.containsKey(skin.card)) {
                    true
                } else {
                    val currentCards = inventory.cards[skin.card] ?: 0
                    val paidCards = containers.sumOf { container -> container.pickedCards.count { c -> c == skin.card } }

                    futureWarn = true
                    currentCards - paidCards <= 0
                }

                if (warn) {
                    val content = if (futureWarn) {
                        "After payment, you won't own any of this card. Are you sure you want to purchase this skin? This cannot be undone"
                    } else {
                        "You don't own this card currently. Are you sure you want to purchase this skin? This cannot be undone"
                    }

                    registerPopUp(event, content, LangID.EN)

                    connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                        skin.purchase(inventory, containers)

                        e.deferReply()
                            .setContent("Successfully purchased skin : ${skin.name}! You can equip skin in `cd.cards` command!")
                            .setEphemeral(true)
                            .queue()

                        goBack()
                    }, LangID.EN))
                } else {
                    skin.purchase(inventory, containers)

                    event.deferReply()
                        .setContent("Successfully purchased skin : ${skin.name}! You can equip skin in `cd.cards` command!")
                        .setEphemeral(true)
                        .queue()

                    goBack()
                }
            }
            "cost" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val index = event.values[0].toInt()
                val container = containers[index]

                connectTo(event, CardCostPayHolder(authorMessage, channelID, message, container, containers))
            }
            "back" -> {
                if (containers.any { container -> container.pickedCards.isNotEmpty() }) {
                    StaticStore.removeHolder(authorMessage.author.id, this)

                    registerPopUp(event, "Are you sure you want to go back? All your selected cards will be cleared", LangID.EN)

                    connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                        e.deferEdit().queue()

                        goBack()
                    }, LangID.EN))
                } else {
                    event.deferEdit().queue()

                    goBack()
                }
            }
        }
    }

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onBack(event: GenericComponentInteractionCreateEvent, child: Holder?) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        var builder = event.deferEdit()
            .setContent(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)

        if (event.message.attachments.isEmpty()) {
            builder = builder.setFiles(FileUpload.fromData(skin.file))
        }

        builder.queue()
    }

    private fun applyResult() {
        message = updateMessageStatus(message)

        var builder = message.editMessage(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)

        if (message.attachments.isEmpty()) {
            builder = builder.setFiles(FileUpload.fromData(skin.file))
        }

        builder.queue()
    }

    private fun getContent() : String {
        val builder = StringBuilder()

        builder.append(skin.displayInfo(false))

        if (!skin.cost.affordable(inventory)) {
            builder.append("\n\nYou can't afford this skin. Check reason below :\n\n").append(skin.cost.getReason(inventory))
        } else if (containers.any { container -> !container.paid() }) {
            builder.append("\n\nTo purchase this skin, you have to pay all card costs. You can check if you paid for each card cost or not by checking each list's descriptions\n" +
                    "\n" +
                    "Additionally, cat foods and platinum shards will be paid automatically")
        } else {
            builder.append("\n\nClick `Purchase` button to pay purchase the skin")
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        if (containers.isNotEmpty()) {
            val options = ArrayList<SelectOption>()

            containers.forEachIndexed { index, container ->
                options.add(
                    SelectOption.of(if (container.cost is SpecificCardCost) container.cost.simpleCostName() else container.cost.getCostName(), index.toString())
                        .withDescription(if (container.paid()) "Ready to pay" else "Need to be paid")
                )
            }

            result.add(ActionRow.of(StringSelectMenu.create("cost").addOptions(options).setPlaceholder("Select cost to pay cards").setDisabled(!skin.cost.affordable(
                inventory
            )).build()))
        }

        val buttons = ArrayList<Button>()

        buttons.add(
            Button.success("purchase", "Purchase")
                .withDisabled(!skin.cost.affordable(inventory) || containers.any { container -> !container.paid() })
                .withEmoji(EmojiStore.ABILITY["CF"])
        )

        buttons.add(Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK))

        result.add(ActionRow.of(buttons))

        return result
    }
}