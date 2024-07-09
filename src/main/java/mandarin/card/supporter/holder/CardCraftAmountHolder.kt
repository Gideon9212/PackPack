package mandarin.card.supporter.holder

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.filter.BannerFilter
import mandarin.card.supporter.holder.modal.CraftAmountHolder
import mandarin.card.supporter.log.TransactionLogger
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.FileUpload
import kotlin.math.min

class CardCraftAmountHolder(author: Message, channelID: String, message: Message, private val craftMode: CardData.CraftMode) : ComponentHolder(author, channelID, message) {
    private val inventory = Inventory.getInventory(author.author.idLong)

    private var amount = 1

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {

        when(event.componentId) {
            "cancel" -> {
                event.deferEdit()
                    .setContent("Canceled craft")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            }
            "back" -> {
                goBack(event)
            }
            "add" -> {
                amount++

                applyResult(event)
            }
            "reduce" -> {
                amount--

                applyResult(event)
            }
            "amount" -> {
                val input = TextInput.create("amount", "Amount", TextInputStyle.SHORT)
                    .setPlaceholder("Define amount of card that will be crafted")
                    .setRequired(true)
                    .setValue(amount.toString())
                    .build()

                val modal = Modal.create("amount", "Amount of Card")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                StaticStore.putHolder(authorMessage.author.id, CraftAmountHolder(authorMessage, channelID, message) { a ->
                    amount = a

                    applyResult()
                })
            }
            "craft" -> {
                val emoji = when(craftMode) {
                    CardData.CraftMode.T2 -> EmojiStore.getCardEmoji(CardPack.CardType.T2)
                    CardData.CraftMode.SEASONAL -> EmojiStore.getCardEmoji(CardPack.CardType.SEASONAL)
                    CardData.CraftMode.COLLAB -> EmojiStore.getCardEmoji(CardPack.CardType.COLLABORATION)
                    CardData.CraftMode.T3 -> EmojiStore.getCardEmoji(CardPack.CardType.T3)
                    CardData.CraftMode.T4 -> EmojiStore.getCardEmoji(CardPack.CardType.T4)
                }

                val name = (emoji?.formatted ?: "") + " " + when(craftMode) {
                    CardData.CraftMode.T2 -> "Regular Tier 2 [Uncommon]"
                    CardData.CraftMode.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
                    CardData.CraftMode.COLLAB -> "Collaboration Tier 2 [Uncommon]"
                    CardData.CraftMode.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
                    CardData.CraftMode.T4 -> "Tier 4 [Legend Rare]"
                }

                registerPopUp(event, "Are you sure you want to spend ${EmojiStore.ABILITY["SHARD"]?.formatted} ${craftMode.cost * amount} on crafting $amount $name card${if (amount >= 2) "s" else ""}?", LangID.EN)

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    val result = rollCards()

                    displayRollResult(e, result)

                    inventory.addCards(result)
                    inventory.platinumShard -= amount * craftMode.cost

                    CardBot.saveCardData()

                    TransactionLogger.logCraft(authorMessage.author.idLong, amount, craftMode, result, amount.toLong() * craftMode.cost)
                }, LangID.EN))
            }
        }
    }

    override fun onConnected() {
        applyResult()
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        applyResult()
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

    private fun applyResult() {
        message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        val emoji = when(craftMode) {
            CardData.CraftMode.T2 -> EmojiStore.getCardEmoji(CardPack.CardType.T2)
            CardData.CraftMode.SEASONAL -> EmojiStore.getCardEmoji(CardPack.CardType.SEASONAL)
            CardData.CraftMode.COLLAB -> EmojiStore.getCardEmoji(CardPack.CardType.COLLABORATION)
            CardData.CraftMode.T3 -> EmojiStore.getCardEmoji(CardPack.CardType.T3)
            CardData.CraftMode.T4 -> EmojiStore.getCardEmoji(CardPack.CardType.T4)
        }

        val name = (emoji?.formatted ?: "") + " " + when(craftMode) {
            CardData.CraftMode.T2 -> "Regular Tier 2 [Uncommon]"
            CardData.CraftMode.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
            CardData.CraftMode.COLLAB -> "Collaboration Tier 2 [Uncommon]"
            CardData.CraftMode.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
            CardData.CraftMode.T4 -> "Tier 4 [Legend Rare]"
        }

        return "You are crafting $amount $name card${if (amount >= 2) "s" else ""}\n" +
                "\n" +
                "You can change the amount of card that will be crafted as well\n" +
                "\n" +
                "Required shard : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${craftMode.cost * amount}\n" +
                "Currently you have ${EmojiStore.ABILITY["SHARD"]?.formatted} ${inventory.platinumShard}" +
                if (amount * craftMode.cost > inventory.platinumShard) "\n\n**You can't craft cards because you don't have enough platinum shards!**" else ""
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(
            Button.secondary("reduce", "Reduce Amount").withDisabled(amount <= 1).withEmoji(Emoji.fromUnicode("➖")),
            Button.secondary("amount", "Set Amount"),
            Button.secondary("add", "Add Amount").withDisabled(amount >= 10).withEmoji(Emoji.fromUnicode("➕"))
        ))

        result.add(
            ActionRow.of(
                Button.success("craft", "Craft").withEmoji(Emoji.fromUnicode("\uD83E\uDE84")).withDisabled(amount * craftMode.cost > inventory.platinumShard),
                Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK),
                Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
            )
        )

        return result
    }

    private fun rollCards() : List<Card> {
        CardData.activatedBanners

        val cards = when(craftMode) {
            CardData.CraftMode.T2 -> CardData.cards.filter { c -> c.unitID > 0 && c.unitID in BannerFilter.Banner.TheAlimighties.getBannerData() || c.unitID in BannerFilter.Banner.GirlsAndMonsters.getBannerData() }
            CardData.CraftMode.SEASONAL -> CardData.cards.filter { c -> c.unitID > 0 && c.unitID in BannerFilter.Banner.Seasonal.getBannerData() }.filter { c -> CardData.activatedBanners.any { a -> c.unitID in CardData.bannerData[a.tier.ordinal][a.banner] } }
            CardData.CraftMode.COLLAB -> CardData.cards.filter { c -> c.unitID > 0 && c.unitID in BannerFilter.Banner.Collaboration.getBannerData() }.filter { c -> CardData.activatedBanners.any { a -> c.unitID in CardData.bannerData[a.tier.ordinal][a.banner] } }
            CardData.CraftMode.T3 -> CardData.cards.filter { c -> c.tier == CardData.Tier.ULTRA && c.unitID > 0 && c.unitID != 435 && c.unitID != 484 }
            CardData.CraftMode.T4 -> CardData.cards.filter { c -> c.tier == CardData.Tier.LEGEND && c.unitID > 0 }
        }

        val result = ArrayList<Card>()

        repeat(amount) {
            result.add(cards.random())
        }

        return result
    }

    private fun displayRollResult(event: GenericComponentInteractionCreateEvent, result: List<Card>) {
        val card = if (result.size <= 1)
            "card"
        else
            "cards"

        val builder = StringBuilder("### Craft Result [${result.size} $card in total]\n\n")

        for (card in result) {
            builder.append("- ")

            if (card.tier == CardData.Tier.ULTRA) {
                builder.append(Emoji.fromUnicode("✨").formatted).append(" ")
            } else if (card.tier == CardData.Tier.LEGEND) {
                builder.append(EmojiStore.ABILITY["LEGEND"]?.formatted).append(" ")
            }

            builder.append(card.cardInfo())

            if (!inventory.cards.containsKey(card)) {
                builder.append(" {**NEW**}")
            }

            if (card.tier == CardData.Tier.ULTRA) {
                builder.append(" ").append(Emoji.fromUnicode("✨").formatted)
            } else if (card.tier == CardData.Tier.LEGEND) {
                builder.append(" ").append(EmojiStore.ABILITY["LEGEND"]?.formatted)
            }

            builder.append("\n")
        }

        val initialEmbed = EmbedBuilder()

        initialEmbed.setDescription(builder.toString().trim())
            .setColor(StaticStore.rainbow.random())

        val newCards = result.toSet().filter { c -> !inventory.cards.containsKey(c) && !inventory.favorites.containsKey(c) }.sortedWith(CardComparator()).reversed()

        if (newCards.isNotEmpty()) {
            val links = ArrayList<String>()
            val files = ArrayList<FileUpload>()

            newCards.forEachIndexed { index, card ->
                val skin = inventory.equippedSkins[card]

                if (skin == null) {
                    files.add(FileUpload.fromData(card.cardImage, "card$index.png"))
                    links.add("attachment://card$index.png")
                } else {
                    skin.cache(event.jda, true)

                    links.add(skin.cacheLink)
                }
            }

            val embeds = ArrayList<MessageEmbed>()

            links.forEachIndexed { index, link ->
                if (index == 0) {
                    initialEmbed.setUrl("https://none.dummy").setImage(link)

                    embeds.add(initialEmbed.build())
                } else {
                    embeds.add(EmbedBuilder().setUrl("https://none.dummy").setImage(link).build())
                }
            }

            event.deferEdit()
                .setContent("")
                .setEmbeds(embeds)
                .setComponents()
                .setFiles(files)
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()

            return
        }

        val availableSkins = result.toSet()
            .filter { c -> inventory.equippedSkins.containsKey(c) }
            .map { c -> inventory.equippedSkins[c] }
            .filterNotNull()

        if (availableSkins.isEmpty()) {
            event.deferEdit()
                .setContent("")
                .setEmbeds(initialEmbed.build())
                .setComponents()
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()

            return
        }

        availableSkins.forEach { s -> s.cache(authorMessage.jda, true) }

        val cachedLinks = availableSkins.subList(0, min(availableSkins.size, Message.MAX_EMBED_COUNT))
            .filter { skin -> skin.cacheLink.isNotEmpty() }
            .map { skin -> skin.cacheLink }

        val embeds = ArrayList<MessageEmbed>()

        cachedLinks.forEachIndexed { index, link ->
            if (index == 0) {
                initialEmbed.setUrl(link).setImage(link)

                embeds.add(initialEmbed.build())
            } else {
                embeds.add(EmbedBuilder().setUrl(cachedLinks[0]).setImage(link).build())
            }
        }

        event.deferEdit()
            .setContent("")
            .setEmbeds(embeds)
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }
}