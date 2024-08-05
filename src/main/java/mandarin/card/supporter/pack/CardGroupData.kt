package mandarin.card.supporter.pack

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.CardData
import mandarin.card.supporter.filter.BannerFilter

class CardGroupData(
    val types: ArrayList<CardPack.CardType>,
    val extra: ArrayList<BannerFilter.Banner>
) {
    companion object {
        fun fromJson(obj: JsonObject) : CardGroupData {
            val groupData = CardGroupData(ArrayList(), ArrayList())

            if (obj.has("types")) {
                val arr = obj.getAsJsonArray("types")

                arr.forEach { e ->
                    groupData.types.add(CardPack.CardType.valueOf(e.asString))
                }
            }

            if (obj.has("extra")) {
                val arr = obj.getAsJsonArray("extra")

                arr.forEach { e ->
                    groupData.extra.add(BannerFilter.Banner.valueOf(e.asString))
                }
            }

            return groupData
        }
    }

    fun gatherPools() : List<Card> {
        val result = ArrayList<Card>()

        for (type in types) {
            result.addAll(
                when (type) {
                    CardPack.CardType.T1 -> CardData.cards.filter { c -> c.tier == CardData.Tier.COMMON }
                    CardPack.CardType.T2 -> CardData.appendUncommon()
                    CardPack.CardType.REGULAR -> CardData.cards.filter { c -> c.isRegularUncommon() }
                    CardPack.CardType.SEASONAL -> CardData.cards.filter { c -> c.isSeasonalUncommon() }
                    CardPack.CardType.COLLABORATION -> CardData.cards.filter { c -> c.isCollaborationUncommon() }
                    CardPack.CardType.T3 -> CardData.appendUltra()
                    CardPack.CardType.T4 -> CardData.appendLR()
                }
            )
        }

        result.removeIf { c -> c.unitID < 0 }

        for (banner in extra) {
            result.addAll(banner.getBannerData().mapNotNull { id -> CardData.cards.find { c -> c.unitID == id } })
        }

        return result.toSet().toList()
    }

    fun getName() : String {
        val builder = StringBuilder()

        for (i in types.indices) {
            val typeName = when(types[i]) {
                CardPack.CardType.T1 -> "Tier 1 [Common]"
                CardPack.CardType.T2 -> "Tier 2 [Uncommon]"
                CardPack.CardType.REGULAR -> "Regular Tier 2 [Uncommon]"
                CardPack.CardType.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
                CardPack.CardType.COLLABORATION -> "Collaboration Tier 2 [Uncommon]"
                CardPack.CardType.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
                CardPack.CardType.T4 -> "Tier 4 [Legend Rare]"
            }

            builder.append(typeName)

            if (i < types.size - 1) {
                builder.append(", ")
            }
        }

        if (types.isNotEmpty() && extra.isNotEmpty()) {
            builder.append(", ")
        }

        for (i in extra.indices) {
            val bannerName = BannerFilter.getBannerName(extra[i])

            builder.append(bannerName)

            if (i < extra.size - 1) {
                builder.append(", ")
            }
        }

        return builder.toString()
    }

    fun asJson() : JsonObject {
        val obj = JsonObject()

        val typeArr = JsonArray()

        types.forEach { tier ->
            typeArr.add(tier.name)
        }

        obj.add("types", typeArr)

        val extraArr = JsonArray()

        extra.forEach { banner ->
            extraArr.add(banner.name)
        }

        obj.add("extra", extraArr)

        return obj
    }
}