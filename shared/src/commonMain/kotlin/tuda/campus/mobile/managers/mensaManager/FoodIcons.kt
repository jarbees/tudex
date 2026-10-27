package tuda.campus.mobile.managers.mensaManager

import com.fleeksoft.ksoup.Ksoup
import tuda.campus.mobile.managers.FoodType
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.cancellation.CancellationException

object FoodIcons {
    private val _icons = MutableStateFlow<Map<FoodType, String>>(emptyMap())
    val icons = _icons.asStateFlow()

    private val _mensaVital = MutableStateFlow<String?>(null)
    val mensaVital = _mensaVital.asStateFlow()

    private const val iconURL = "https://studierendenwerkdarmstadt.de/hochschulgastronomie/speisen/stadtmitte/"
    private const val vitalURL = "https://web.archive.org/web/20251103141811if_/https://www.mensavital.de/typo3conf/ext/mensavital/Resources/Public/Img/mensavital-logo.svg"
    private val client = HttpClient()

    operator fun get(type: FoodType): String? = icons.value[type]

    suspend fun load() {
        try {
            val html = client.get(iconURL).bodyAsText()
            val parsed = parse(html)

            if (parsed.isNotEmpty()) {
                _icons.value = parsed
            }

            val vitalHtml = client.get(vitalURL).bodyAsText()
            val document = Ksoup.parse(vitalHtml)

            val svg = document.selectFirst("svg") ?: throw Exception()
            svg.select("path").drop(4).forEach { it.remove() }

            val vitalSvg = svg.outerHtml()

            if (vitalSvg.isNotEmpty()) {
                _mensaVital.value = vitalSvg
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Failed to load food icons: $e")
        }
    }

    private fun parse(html: String): Map<FoodType, String> {
        val document = Ksoup.parse(html)

        return document
            .select(".fmc-filter-items li[data-cat]")
            .mapNotNull { element ->
                val category = element.attr("data-cat")
                    .toIntOrNull()
                    ?.toFoodType()
                    ?: return@mapNotNull null

                val svg = element.selectFirst("svg.icon-food")
                    ?.outerHtml()
                    ?: return@mapNotNull null

                category to svg
            }
            .toMap()
    }

    private fun Int.toFoodType(): FoodType? = when (this) {
        //1 -> FoodType.VEGAN_PIG
        2 -> FoodType.VEGAN
        3 -> FoodType.MEATLESS
        4 -> FoodType.POULTRY
        5 -> FoodType.PORK
        6 -> FoodType.BEEF
        //7 -> FoodType.CALF
        //8 -> FoodType.LAMB
        9 -> FoodType.VENISON
        10 -> FoodType.FISH
        //11 -> FoodType.BEEF_PIG
        12 -> FoodType.PORK_FISH
        //13 -> FoodType.BEEF_PIG
        14 -> FoodType.POULTRY_FISH
        else -> null
    }
}