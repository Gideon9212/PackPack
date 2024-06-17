package mandarin.card.supporter.holder.moderation.modify

import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.modal.CatFoodModifyHolder
import mandarin.card.supporter.holder.modal.PlatinumShardModifyHolder
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal

class ModifyModeSelectHolder(author: Message, channelID: String, private val message: Message, private val category: CardData.ModifyCategory, private val inventory: Inventory, private val targetMember: Member) : ComponentHolder(author, channelID, message.id) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "mode" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                val isAdd = when(val mode = event.values[0]) {
                    "add" -> {
                        true
                    }
                    "remove" -> {
                        false
                    }
                    else -> {
                        throw IllegalStateException("E/ModifyModeSelectHolder::onEvent - Invalid mode $mode")
                    }
                }

                when (category) {
                    CardData.ModifyCategory.CARD -> {
                        connectTo(event, CardModifyHolder(authorMessage, channelID, message, isAdd, inventory, targetMember))
                    }
                    CardData.ModifyCategory.ROLE -> {
                        connectTo(event, RoleModifyHolder(authorMessage, channelID, message, isAdd, inventory, targetMember))
                    }
                    CardData.ModifyCategory.CF -> {
                        val input = TextInput.create("amount", "Amount", TextInputStyle.SHORT)
                            .setPlaceholder("Define amount of cat foods that will be ${if(isAdd) "added" else "removed"} from this user")
                            .build()

                        val modal = Modal.create("cf", "Modify Cat Food")
                            .addActionRow(input)
                            .build()

                        event.replyModal(modal).queue()

                        connectTo(CatFoodModifyHolder(authorMessage, channelID, message.id, inventory, isAdd, targetMember.id))
                    }
                    CardData.ModifyCategory.SHARD -> {
                        val input = TextInput.create("amount", "Amount", TextInputStyle.SHORT)
                            .setPlaceholder("Define amount of platinum shards that will be ${if(isAdd) "added" else "removed"} from this user")
                            .build()

                        val modal = Modal.create("shard", "Modify Platinum Shard")
                            .addActionRow(input)
                            .build()

                        event.replyModal(modal).queue()

                        connectTo(PlatinumShardModifyHolder(authorMessage, channelID, message.id, inventory, isAdd, targetMember.id))
                    }
                }
            }
            "back" -> {
                event.deferEdit().queue()

                goBack()
            }
            "close" -> {
                event.deferEdit()
                    .setContent("Modify closed")
                    .setComponents()
                    .mentionRepliedUser(false)
                    .setAllowedMentions(ArrayList())
                    .queue()

                expired = true

                expire(authorMessage.author.id)
            }
        }
    }

    override fun onBack(child: Holder) {
        super.onBack(child)

        applyResult()
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    private fun applyResult() {
        val target = when (category) {
            CardData.ModifyCategory.CARD -> "cards"
            CardData.ModifyCategory.ROLE -> "roles"
            CardData.ModifyCategory.CF -> "cat foods"
            else -> "platinum shards"
        }

        val content = when (category) {
            CardData.ModifyCategory.CF -> {
                "Do you want to add or remove $target?\n\nCurrently this user has ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.catFoods}"
            }
            CardData.ModifyCategory.SHARD -> {
                "Do you want to add or remove $target?\n\nCurrently this user has ${EmojiStore.ABILITY["SHARD"]?.formatted} ${inventory.platinumShard}"
            }
            else -> {
                "Do you want to add or remove $target?"
            }
        }

        message.editMessage(content)
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        val target = when (category) {
            CardData.ModifyCategory.CARD -> "cards"
            CardData.ModifyCategory.ROLE -> "roles"
            CardData.ModifyCategory.CF -> "cat foods"
            else -> "platinum shards"
        }

        val content = when (category) {
            CardData.ModifyCategory.CF -> {
                "Do you want to add or remove $target?\n\nCurrently this user has ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.catFoods}"
            }
            CardData.ModifyCategory.SHARD -> {
                "Do you want to add or remove $target?\n\nCurrently this user has ${EmojiStore.ABILITY["SHARD"]?.formatted} ${inventory.platinumShard}"
            }
            else -> {
                "Do you want to add or remove $target?"
            }
        }

        event.deferEdit()
            .setContent(content)
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getComponents() : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val modeOptions = ArrayList<SelectOption>()

        modeOptions.add(SelectOption.of("Add", "add").withEmoji(Emoji.fromUnicode("➕")))
        modeOptions.add(SelectOption.of("Remove", "remove").withEmoji(Emoji.fromUnicode("➖")))

        val modes = StringSelectMenu.create("mode")
            .addOptions(modeOptions)
            .setPlaceholder("Please select mode")
            .build()

        rows.add(ActionRow.of(modes))

        rows.add(ActionRow.of(
            Button.secondary("back", "Back"),
            Button.danger("close", "Close")
        ))

        return rows
    }
}