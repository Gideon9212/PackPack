package mandarin.card.supporter.holder

import mandarin.card.CardBot
import mandarin.card.supporter.*
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.holder.modal.CardAmountSelectHolder
import mandarin.card.supporter.holder.modal.CatFoodHolder
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import kotlin.math.min

class SuggestInventoryHolder(
    author: Message,
    channelID: String,
    message: Message,
    private val targetMember: Member,
    private val suggestionMessage: Message,
    private val session: TradingSession,
    private val inventory: Inventory
) : ComponentHolder(author, channelID, message) {
    private val index = session.member.indexOf(author.author.idLong)
    private val suggestion = session.suggestion[index]
    private val backup = suggestion.copy()

    private val cards = ArrayList<Card>(inventory.cards.keys.sortedWith(CardComparator()))

    private var page = 0
    private var tier = CardData.Tier.NONE
    private var banner = intArrayOf(-1, -1)

    private var confirmed = false

    init {
        filterCards()
    }

    override fun clean() {

    }

    override fun onExpire(id: String?) {
        if (!confirmed) {
            message.editMessage("Suggestion has been canceled")
                .setAllowedMentions(ArrayList())
                .setComponents()
                .mentionRepliedUser(false)
                .queue()

            suggestionMessage.delete().queue()
        }
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        val member = event.member ?: return

        when(event.componentId) {
            "prev" -> {
                page--

                applyResult(event)
            }
            "prev10" -> {
                page -= 10

                applyResult(event)
            }
            "next" -> {
                page++

                applyResult(event)
            }
            "next10" -> {
                page += 10

                applyResult(event)
            }
            "confirm" -> {
                expired = true

                suggestion.paste(backup)

                event.deferEdit().queue()

                message.delete().queue()

                val opposite = (2 - index) / 2

                val contents = if (!suggestion.touched) {
                    "Suggestion applied"
                } else {
                    var result = "Suggestion has been edited"

                    if (session.agreed.any { a -> a }) {
                        result += ", so now users have to re-confirm again"
                    }

                    result
                }

                session.agreed[0] = false
                session.agreed[1] = false

                session.approved = false

                Command.replyToMessageSafely(event.messageChannel, contents, suggestionMessage) { a -> a }

                event.messageChannel.sendMessage("<@${session.member[opposite]}>, please check suggestion above").queue()

                suggestion.touched = true
                confirmed = true

                if (!session.confirmNotified && session.suggestion.all { s -> s.touched }) {
                    event.messageChannel.sendMessage("<@${session.member[0]}>, <@${session.member[1]}>, now both users can confirm each other's trade suggestions").queue()

                    session.confirmNotified = true

                    val g = event.guild ?: return

                    session.needApproval(g, {
                        event.messageChannel.sendMessage("Pinging <@&${CardData.dealer}> for approval because this trade contains above 200k cf or above tier 3 cards").queue()
                    }, { })
                }

                CardBot.saveCardData()

                expire(authorMessage.author.id)
            }
            "category" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                val value = event.values[0]

                banner = if (value == "all") {
                    intArrayOf(-1, -1)
                } else {
                    val data = value.split("-")

                    intArrayOf(data[1].toInt(), data[2].toInt())
                }

                page = 0

                filterCards()

                applyResult(event)
            }
            "tier" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                val value = event.values[0]

                tier = if (value == "all") {
                    CardData.Tier.NONE
                } else {
                    CardData.Tier.entries[value.replace("tier", "").toInt()]
                }

                banner[0] = -1
                banner[1] = -1

                page = 0

                filterCards()

                applyResult(event)
            }
            "card" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                val selectedID = event.values[0].toInt()

                val card = cards[selectedID]

                if (card.unitID == -56) {
                    event.deferReply()
                        .setContent("Don't you dare \uD83D\uDC41\uFE0F, you can't trade it")
                        .setEphemeral(true)
                        .queue()

                    return
                }

                val realAmount = (inventory.cards[card] ?: 0) - (backup.cards[card] ?: 0)

                if (realAmount >= 2) {
                    val input = TextInput.create("amount", "Amount of Cards", TextInputStyle.SHORT)
                        .setPlaceholder("Put amount up to $realAmount")
                        .build()

                    val modal = Modal.create("select", "Select Amount of Cards")
                        .addActionRow(input)
                        .build()

                    event.replyModal(modal).queue()

                    connectTo(CardAmountSelectHolder(authorMessage, channelID, message) { amount ->
                        val filteredAmount = min(CardData.MAX_CARD_TYPE - backup.cards.size, min(amount, realAmount))

                        backup.cards[card] = (backup.cards[card] ?: 0) + filteredAmount

                        filterCards()

                        if (cards.size <= page * SearchHolder.PAGE_CHUNK && page > 0) {
                            page--
                        }

                        suggestionMessage
                            .editMessage(backup.suggestionInfo(member))
                            .mentionRepliedUser(false)
                            .setAllowedMentions(ArrayList())
                            .queue()

                        applyResult()
                    })
                } else {
                    backup.cards[card] = (backup.cards[card] ?: 0) + 1

                    event.deferReply().setContent("Suggested : ${cards[selectedID].simpleCardInfo()}").setEphemeral(true).queue()

                    filterCards()

                    suggestionMessage
                        .editMessage(backup.suggestionInfo(member))
                        .mentionRepliedUser(false)
                        .setAllowedMentions(ArrayList())
                        .queue()

                    if (cards.size <= page * SearchHolder.PAGE_CHUNK && page > 0) {
                        page--
                    }

                    applyResult()
                }
            }
            "cf" -> {
                val input = TextInput.create("cf", "Cat Food", TextInputStyle.SHORT)
                    .setPlaceholder("Decide amount of cat foods that will be traded")
                    .setRequired(true)
                    .build()

                val modal = Modal.create("cf", "Cat Food Suggestion")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                StaticStore.putHolder(authorMessage.author.id, CatFoodHolder(authorMessage, channelID, message, suggestionMessage, backup))
            }
            "dupe" -> {
                cards.filter { c -> (inventory.cards[c] ?: 0) - (backup.cards[c] ?: 0) > 1 }.forEach { c ->
                    if (backup.cards.size >= CardData.MAX_CARD_TYPE)
                        return@forEach

                    val realAmount = (inventory.cards[c] ?: 0) - (backup.cards[c] ?: 0)

                    backup.cards[c] = (backup.cards[c] ?: 0) + realAmount - 1
                }

                event.deferReply().setContent("Successfully added duplicated! Check the result above").setEphemeral(true).queue()

                suggestionMessage
                    .editMessage(backup.suggestionInfo(member))
                    .mentionRepliedUser(false)
                    .setAllowedMentions(ArrayList())
                    .queue()

                applyResult()
            }
            "reset" -> {
                backup.cards.clear()
                backup.catFood = 0

                suggestionMessage
                    .editMessage(backup.suggestionInfo(member))
                    .mentionRepliedUser(false)
                    .setAllowedMentions(ArrayList())
                    .queue()

                page = 0

                filterCards()

                applyResult()

                event.deferReply().setContent("Successfully cleared suggestion!").setEphemeral(true).queue()
            }
            "cancel" -> {
                expired = true

                event.deferEdit()
                    .setContent("Suggestion has been canceled")
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .setComponents()
                    .queue()

                expire(authorMessage.author.id)
            }
        }
    }

    private fun filterCards() {
        cards.clear()

        if (tier != CardData.Tier.NONE) {
            if (banner[0] == -1) {
                cards.addAll(inventory.cards.keys.filter { c -> c.tier == tier })
            } else {
                cards.addAll(inventory.cards.keys.filter { c -> c.tier == tier && c.unitID in CardData.bannerData[tier.ordinal][banner[1]] })
            }
        } else {
            if (banner[0] == -1) {
                cards.addAll(inventory.cards.keys)
            } else {
                cards.addAll(inventory.cards.keys.filter { c -> c.unitID in CardData.bannerData[banner[0]][banner[1]] })
            }
        }

        cards.removeIf { c ->
            val amount = inventory.cards[c] ?: 0

            amount - (backup.cards[c] ?: 0) <= 0
        }

        val member = authorMessage.member

        if (member == null || !CardData.canTradeT0(member) || !CardData.canTradeT0(targetMember)) {
            cards.removeIf { c -> c.tier == CardData.Tier.SPECIAL }
        }

        cards.sortWith(CardComparator())
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent(getText())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .setComponents(assignComponents())
            .queue()
    }

    private fun applyResult() {
        message.editMessage(getText())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .setComponents(assignComponents())
            .queue()
    }

    private fun assignComponents() : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val member = authorMessage.member

        val tierCategoryElements = ArrayList<SelectOption>()

        tierCategoryElements.add(SelectOption.of("All", "all"))

        CardData.tierCategoryText.forEachIndexed { index, text ->
            val emoji = EmojiStore.getCardEmoji(
                when (index) {
                    0 -> null
                    1 -> CardPack.CardType.T1
                    2 -> CardPack.CardType.T2
                    3 -> CardPack.CardType.T3
                    4 -> CardPack.CardType.T4
                    else -> throw IllegalStateException("E/CardModifyHolder::assignComponents - Invalid tier index $index")
                }
            )

            if (index == CardData.Tier.SPECIAL.ordinal) {
                if (member != null && CardData.canTradeT0(member) && CardData.canTradeT0(targetMember)) {
                    tierCategoryElements.add(SelectOption.of(text, "tier${index}").withEmoji(emoji))
                }
            } else {
                tierCategoryElements.add(SelectOption.of(text, "tier${index}").withEmoji(emoji))
            }
        }

        val tierCategory = StringSelectMenu.create("tier")
            .addOptions(tierCategoryElements)
            .setPlaceholder("Filter Cards by Tiers")

        if (tier == CardData.Tier.NONE) {
            tierCategory.setDefaultOptions(tierCategoryElements[0])
        } else {
            val option = tierCategoryElements.find { option -> option.value == "tier${tier.ordinal}" }

            tierCategory.setDefaultOptions(option)
        }

        rows.add(ActionRow.of(tierCategory.build()))

        if (tier == CardData.Tier.NONE || CardData.bannerCategoryText[tier.ordinal].isNotEmpty()) {
            val bannerCategoryElements = ArrayList<SelectOption>()

            bannerCategoryElements.add(SelectOption.of("All", "all"))

            if (tier == CardData.Tier.NONE) {
                CardData.bannerCategoryText.forEachIndexed { index, array ->
                    array.forEachIndexed { i, a ->
                        bannerCategoryElements.add(SelectOption.of(a, "category-$index-$i"))
                    }
                }
            } else {
                CardData.bannerCategoryText[tier.ordinal].forEachIndexed { i, a ->
                    bannerCategoryElements.add(SelectOption.of(a, "category-${tier.ordinal}-$i"))
                }
            }

            val bannerCategory = StringSelectMenu.create("category")
                .addOptions(bannerCategoryElements)
                .setPlaceholder("Filter Cards by Banners")

            val id = if (tier == CardData.Tier.NONE) {
                "category-${banner[0]}-${banner[1]}"
            } else {
                "category-${tier.ordinal}-${banner[1]}"
            }

            val option = bannerCategoryElements.find { e -> e.value == id }

            if (option != null)
                bannerCategory.setDefaultOptions(option)

            rows.add(ActionRow.of(bannerCategory.build()))
        }

        val dataSize = cards.size

        val cardCategoryElements = ArrayList<SelectOption>()

        if (cards.isEmpty()) {
            cardCategoryElements.add(SelectOption.of("a", "-1"))
        } else {
            for(i in page * SearchHolder.PAGE_CHUNK until min(dataSize, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                cardCategoryElements.add(SelectOption.of(cards[i].simpleCardInfo(), i.toString()))
            }
        }

        val cardCategory = StringSelectMenu.create("card")
            .addOptions(cardCategoryElements)
            .setPlaceholder(
                if (backup.cards.size == CardData.MAX_CARD_TYPE)
                    "You can't suggest more than ${CardData.MAX_CARD_TYPE} cards!"
                else if (cards.isEmpty())
                    "No Cards To Select"
                else
                    "Select Card To Suggest"
            )
            .setDisabled(backup.cards.size >= CardData.MAX_CARD_TYPE || cards.isEmpty())
            .build()

        rows.add(ActionRow.of(cardCategory))

        var totPage = dataSize / SearchHolder.PAGE_CHUNK

        if (dataSize % SearchHolder.PAGE_CHUNK != 0)
            totPage++

        if (dataSize > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            if(totPage > 10) {
                if(page - 10 < 0) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS))
                }
            }

            if(page - 1 < 0) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS))
            }

            if(page + 1 >= totPage) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT).asDisabled())
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))
            }

            if(totPage > 10) {
                if(page + 10 >= totPage) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT).asDisabled())
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
                }
            }

            rows.add(ActionRow.of(buttons))
        }

        val possibleCards = cards.filter { c -> (inventory.cards[c] ?: 0) - (session.suggestion[index].cards[c] ?: 0) > 1 }

        val confirmButtons = ArrayList<Button>()

        confirmButtons.add(Button.primary("confirm", "Suggest"))
        confirmButtons.add(Button.secondary("dupe", "Add Duplicated").withDisabled(session.suggestion[index].cards.size >= CardData.MAX_CARD_TYPE || possibleCards.isEmpty()))
        confirmButtons.add(Button.secondary("cf", "Suggest Cat Foods").withEmoji(EmojiStore.ABILITY["CF"]))
        confirmButtons.add(Button.danger("reset", "Clear Suggestions"))
        confirmButtons.add(Button.danger("cancel", "Cancel"))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }

    private fun getText() : String {
        val authorMention = authorMessage.author.asMention

        val builder = StringBuilder("Inventory of ${authorMention}\n\n```md\n")

        if (cards.isNotEmpty()) {
            for (i in page * SearchHolder.PAGE_CHUNK until min((page + 1) * SearchHolder.PAGE_CHUNK, cards.size)) {
                builder.append("${i + 1}. ${cards[i].cardInfo()}")

                val amount = (inventory.cards[cards[i]] ?: 0) - (backup.cards[cards[i]] ?: 0)

                if (amount >= 2) {
                    builder.append(" x$amount\n")
                } else {
                    builder.append("\n")
                }
            }
        } else {
            builder.append("No Cards Found")
        }

        builder.append("```")

        return builder.toString()
    }
}