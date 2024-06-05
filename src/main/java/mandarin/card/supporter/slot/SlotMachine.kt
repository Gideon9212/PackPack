package mandarin.card.supporter.slot

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.CardBot
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.utils.FileUpload
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class SlotMachine {
    companion object {
        fun fromJson(obj: JsonObject) : SlotMachine {
            if (!StaticStore.hasAllTag(obj, "name", "uuid", "activate", "cooldown", "slotSize", "entryFee", "content")) {
                throw IllegalStateException("E/SlotMachine::fromJson - Invalid json data found")
            }

            val name = obj.get("name").asString
            val uuid = obj.get("uuid").asString

            val slotMachine = SlotMachine(name, uuid)

            slotMachine.activate = obj.get("activate").asBoolean

            slotMachine.cooldown = obj.get("cooldown").asLong
            slotMachine.slotSize = obj.get("slotSize").asInt
            slotMachine.entryFee = SlotEntryFee.fromJson(obj.getAsJsonObject("entryFee"))

            val arr = obj.getAsJsonArray("content")

            arr.forEach { e ->
                slotMachine.content.add(SlotContent.fromJson(e.asJsonObject))
            }

            if (obj.has("roles")) {
                obj.getAsJsonArray("roles").forEach { e ->
                    slotMachine.roles.add(e.asLong)
                }
            }

            return slotMachine
        }
    }

    var name: String

    var activate = false

    var cooldown = 0L
    var slotSize = 3
    var entryFee = SlotEntryFee()
        private set

    val content = ArrayList<SlotContent>()

    val roles = ArrayList<Long>()

    val valid: Boolean
        get() {
            if (entryFee.invalid)
                return false

            if (content.isEmpty())
                return false

            content.forEach { c ->
                if (c !is SlotCardContent)
                    return@forEach

                if (c.cardChancePairLists.isEmpty())
                    return false

                if (c.slot == 0)
                    return false

                c.cardChancePairLists.forEach { l ->
                    if (l.amount == 0)
                        return false

                    if (l.pairs.sumOf { p -> p.chance } != 100.0)
                        return false

                    l.pairs.forEach { p ->
                        if (p.cardGroup.extra.isEmpty() && p.cardGroup.types.isEmpty())
                            return false
                    }
                }
            }

            return true
        }

    val uuid: String

    constructor(name: String) {
        this.name = name

        uuid = "$name|${CardData.getUnixEpochTime()}"
    }

    private constructor(name: String, uuid: String) {
        this.name = name
        this.uuid = uuid
    }

    fun asText() : String {
        val builder = StringBuilder("## ")
            .append(name)
            .append("\nThis slot machine has ")
            .append(slotSize)
            .append(" slot")

        if (slotSize >= 2)
            builder.append("s")

        builder.append("\n### Entry Fee\n")
            .append(entryFee.asText())
            .append("\n### Contents\n")

        if (content.isEmpty()) {
            builder.append("- No Contents\n")
        } else {
            val emoji = when(entryFee.entryType) {
                SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
                SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
            }

            content.forEachIndexed { index, content ->
                builder.append(index + 1).append(". ").append(content.emoji?.formatted ?: EmojiStore.UNKNOWN.formatted)

                if (content !is SlotPlaceHolderContent) {
                    builder.append("x").append(content.slot)
                }

                when (content) {
                    is SlotCardContent -> {
                        builder.append(" [Card] : ").append(content.name.ifBlank { "None" }).append("\n")

                        content.cardChancePairLists.forEachIndexed { ind, list ->
                            builder.append("  - ").append(list.amount).append(" ")

                            if (list.amount >= 2) {
                                builder.append("Cards\n")
                            } else {
                                builder.append("Card\n")
                            }

                            list.pairs.forEachIndexed { i, pair ->
                                builder.append("    - ").append(CardData.df.format(pair.chance)).append("% : ").append(pair.cardGroup.getName())

                                if (i < list.pairs.size - 1)
                                    builder.append("\n")
                            }

                            if (ind < content.cardChancePairLists.size - 1)
                                builder.append("\n")
                        }
                    }
                    is SlotCurrencyContent -> {
                        when(content.mode) {
                            SlotCurrencyContent.Mode.FLAT -> builder.append(" [Flat] : ").append(emoji).append(" ").append(content.amount)
                            SlotCurrencyContent.Mode.PERCENTAGE -> builder.append(" [Percentage] : ").append(content.amount).append("% of Entry Fee")
                        }
                    }
                    is SlotPlaceHolderContent -> {
                        builder.append(" [Place Holder]")
                    }
                }

                builder.append("\n")
            }
        }

        builder.append("### Cooldown\n")

        if (cooldown <= 0L) {
            builder.append("`No Cooldown`")
        } else {
            builder.append("`").append(CardData.convertMillisecondsToText(cooldown)).append("`")
        }

        if (roles.isNotEmpty()) {
            builder.append("\n\nThis slot machine requires any of roles below!\n")

            roles.forEachIndexed { i, r ->
                builder.append("- <@&").append(r).append(">")

                if (i < roles.size - 1)
                    builder.append("\n")
            }
        }

        return builder.toString()
    }

    fun roll(message: Message, user: Long, inventory: Inventory, input: Long, skip: Boolean) {
        content.filter { c -> c.emoji == null }.forEach { c -> c.load() }

        if (content.any { c -> c.emoji == null }) {
            message.editMessage("Failed to roll slot machine due to invalid emoji data... Contact card managers!")
                .setComponents()
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()

            return
        }

        when(entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> inventory.catFoods -= input
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> inventory.platinumShard -= input
        }

        var previousEmoji: CustomEmoji? = null

        val emojiSequence = ArrayList<CustomEmoji>()
        val sequenceStacks = HashMap<CustomEmoji, Int>()
        var totalSequenceStacks = 0

        var temporalSequenceStack = 0

        val downArrow = Emoji.fromUnicode("🔽").formatted
        val upArrow = Emoji.fromUnicode("🔼").formatted

        val emojis = content.mapNotNull { c -> c.emoji }.toSet()

        if (emojis.isEmpty()) {
            message.editMessage("Failed to roll slot machine due to empty emoji data... Contact card managers!")
                .setComponents()
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()

            return
        }

        if (skip) {
            repeat(slotSize) { index ->
                val emoji = emojis.random()

                sequenceStacks.computeIfAbsent(emoji) { 0 }

                if (index > 0) {
                    if (previousEmoji === emoji) {
                        temporalSequenceStack++
                        totalSequenceStacks++
                    } else {
                        val e = previousEmoji

                        if (e != null) {
                            sequenceStacks[e] = max(sequenceStacks[e] ?: 0, temporalSequenceStack)
                        }

                        temporalSequenceStack = 0
                    }
                }

                previousEmoji = emoji

                emojiSequence.add(emoji)
            }

            val e = previousEmoji

            if (e != null) {
                sequenceStacks[e] = max(sequenceStacks[e] ?: 0, temporalSequenceStack)
            }
        } else {
            repeat(slotSize) { index ->
                val builder = StringBuilder()

                val emoji = emojis.random()

                sequenceStacks.computeIfAbsent(emoji) { 0 }

                if (index > 0) {
                    builder.append("**").append(" ").append(EmojiStore.AIR?.formatted?.repeat(index)).append("**").append(downArrow)
                } else {
                    builder.append("** **").append(downArrow)
                }

                builder.append("\n ")

                emojiSequence.forEach { e -> builder.append(e.formatted) }

                builder.append(emoji.formatted).append("\n ")

                builder.append(EmojiStore.AIR?.formatted?.repeat(index)).append(upArrow)

                message.editMessage(builder.toString())
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                if (index > 0) {
                    if (previousEmoji === emoji) {
                        temporalSequenceStack++
                        totalSequenceStacks++
                    } else {
                        val e = previousEmoji

                        if (e != null) {
                            sequenceStacks[e] = max(sequenceStacks[e] ?: 0, temporalSequenceStack)
                        }

                        temporalSequenceStack = 0
                    }
                }

                previousEmoji = emoji

                emojiSequence.add(emoji)

                Thread.sleep(1000)
            }

            val e = previousEmoji

            if (e != null) {
                sequenceStacks[e] = max(sequenceStacks[e] ?: 0, temporalSequenceStack)
            }
        }

        val result = StringBuilder()

        emojiSequence.forEach { e -> result.append(e.formatted) }

        val pickedContents = pickReward(sequenceStacks)

        if (pickedContents.isNotEmpty()) {
            if (pickedContents.any { c -> c.emoji == null }) {
                result.append("\n\nBot failed to find reward with emoji above... Please contact card managers!")

                message.editMessage(result.toString())
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            } else {
                val feeEmoji = when(entryFee.entryType) {
                    SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
                    SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
                }

                var currencySum = 0L
                val cardsSum = ArrayList<Card>()

                result.append("\n\n🎰 You won the slot machine!!! 🎰\n\nPicked Reward : \n")

                pickedContents.forEach { c ->
                    result.append("- ").append(c.emoji?.formatted).append("x").append(c.slot).append(" ")

                    when(c) {
                        is SlotCurrencyContent -> {
                            val reward = when(c.mode) {
                                SlotCurrencyContent.Mode.FLAT -> c.amount
                                SlotCurrencyContent.Mode.PERCENTAGE -> round(input * c.amount / 100.0).toLong()
                            }

                            currencySum += reward

                            when(c.mode) {
                                SlotCurrencyContent.Mode.FLAT -> {
                                    result.append("[Flat] : ").append(feeEmoji).append(" ").append(c.amount)
                                }
                                SlotCurrencyContent.Mode.PERCENTAGE -> {
                                    result.append("[Percentage] : ").append(c.amount).append("% of Entry Fee")
                                }
                            }

                            result.append("\n")
                        }
                        is SlotCardContent -> {
                            val cards = c.roll()

                            cardsSum.addAll(cards)

                            result.append("[Card] : ").append(c.name).append("\n")
                        }
                    }
                }

                result.append("\nReward : \n")

                if (currencySum != 0L) {
                    val feeName = when(entryFee.entryType) {
                        SlotEntryFee.EntryType.CAT_FOOD -> "Cat Foods"
                        SlotEntryFee.EntryType.PLATINUM_SHARDS -> "Platinum Shards"
                    }

                    result.append("### ").append(feeName).append("\n").append(feeEmoji).append(" ").append(currencySum).append("\n")
                }

                if (cardsSum.isNotEmpty()) {
                    result.append("### Cards\n")

                    cardsSum.forEach { c ->
                        result.append("- ").append(c.simpleCardInfo()).append("\n")
                    }
                }

                val files = ArrayList<FileUpload>()

                cardsSum.toSet().filter { c -> !inventory.cards.containsKey(c) }.forEach { c ->
                    files.add(FileUpload.fromData(c.cardImage))
                }

                when(entryFee.entryType) {
                    SlotEntryFee.EntryType.CAT_FOOD -> inventory.catFoods += currencySum
                    SlotEntryFee.EntryType.PLATINUM_SHARDS -> inventory.platinumShard += currencySum
                }

                cardsSum.forEach { c ->
                    inventory.cards[c] = (inventory.cards[c] ?: 0) + 1
                }

                CardBot.saveCardData()

                TransactionLogger.logSlotMachineWin(user, input, this, pickedContents, currencySum, cardsSum)

                message.editMessage(result.toString())
                    .setComponents()
                    .setFiles(files)
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            }
        } else {
            val percentage = if (slotSize == 2)
                0.0
            else
                min(1.0, totalSequenceStacks * 1.0 / (slotSize - 2))

            val entryEmoji = when(entryFee.entryType) {
                SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
                SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
            }

            val compensation = round(input * percentage).toLong()

            result.append("\n\n😔 You lost the slot machine... 😔\n\n")
                .append("Sequence Stack Score : ").append(totalSequenceStacks).append(" Point(s)\n")
                .append("Compensation : ").append(CardData.df.format(percentage * 100.0)).append("% of Entry Fee -> $entryEmoji $compensation")

            when (entryFee.entryType) {
                SlotEntryFee.EntryType.CAT_FOOD -> inventory.catFoods += compensation
                SlotEntryFee.EntryType.PLATINUM_SHARDS -> inventory.platinumShard += compensation
            }

            TransactionLogger.logSlotMachineRollFail(user, input, this, totalSequenceStacks, compensation)

            message.editMessage(result)
                .setComponents()
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()
        }

        val cooldownMap = CardData.slotCooldown.computeIfAbsent(user.toString()) { _ -> HashMap() }

        cooldownMap[uuid] = CardData.getUnixEpochTime() + cooldown

        CardBot.saveCardData()
    }

    fun asJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("name", name)
        obj.addProperty("uuid", uuid)

        obj.addProperty("activate", activate)

        obj.addProperty("cooldown", cooldown)
        obj.addProperty("slotSize", slotSize)
        obj.add("entryFee", entryFee.asJson())

        val arr = JsonArray()

        content.forEach { content ->
            arr.add(content.asJson())
        }

        obj.add("content", arr)

        val roleArr = JsonArray()

        roles.forEach { r -> roleArr.add(r) }

        obj.add("roles", roleArr)

        return obj
    }

    private fun pickReward(sequenceStacks: Map<CustomEmoji, Int>) : List<SlotContent> {
        val result = HashMap<CustomEmoji, HashSet<SlotContent>>()

        println(sequenceStacks)

        content.filter { c -> c !is SlotPlaceHolderContent }.forEach { c ->
            val e = c.emoji ?: return@forEach

            val stack = sequenceStacks[e] ?: return@forEach

            if (c.slot <= stack + 1) {
                val contentSet = result.computeIfAbsent(e) { HashSet() }

                contentSet.add(c)
            }
        }

        val finalResult = ArrayList<SlotContent>()

        result.forEach { (_, contents) ->
            val maxStack = contents.maxOf { c -> c.slot }

            finalResult.addAll(contents.filter { c -> c.slot == maxStack })
        }

        println(finalResult)

        return finalResult
    }
}