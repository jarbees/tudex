package tuda.campus.mobile.managers

import tuda.campus.mobile.PlatformImage
import tuda.campus.mobile.dataStore
import tuda.campus.mobile.persistentStateFlow
import tuda.campus.mobile.safeGet
import tuda.campus.mobile.safePost
import io.github.santimattius.persistent.cache.installPersistentCache
import io.github.santimattius.persistent.cache.CacheConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import korlibs.time.seconds
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class GraphQLRequest(
    val query: String,
    val variables: Map<String, JsonElement> = emptyMap()
)



@Serializable
data class GraphQLResponse(
    val data: JsonElement,
    val errors: List<Map<String, JsonElement>>? = null
)

// Serializables for Teasers

@Serializable
data class ResponseWrapper(
    val data: Data
)

@Serializable
data class Data(
    val teasers: List<Teaser>
)

@Serializable
data class Teaser(
    val __typename: String,
    val id: Int,
    val subtitle: String?,
    val title: String,
    val lastUpdated: Int,
    val image: Image,  // Nested Image object
    val detailTitle: String? = null,
    val detailText: String? = null,
    val infoText: String? = null,
    val infoTitle: String? = null,
    val url: String? = null
)

@Serializable
data class Image(
    val __typename: String,
    // The API would provide a thumbUrl for menuItems, but this is never used in UI
    val url: String,
    //@Transient val systemImage: SystemImage? = null
    //@Transient val imageData: ByteArray? = null
    @Transient val platformImage: PlatformImage? = null
)

// Serializables for MenuItems

@Serializable
data class MenuResponseWrapper(
    val data: MenuData
)

@Serializable
data class MenuData(
    val menuItems: List<MenuItem>
)

@Serializable
data class MenuItem(
    val issuingOffice: IssuingOffice,
    val __typename: String,
    val id: String,
    val lastUpdated: Int,
    val dish: Dish,
    val date: Int
)

@Serializable
data class IssuingOffice(
    val __typename: String,
    val id: Int,
    val canteenId: Int,
    val name: String,
)

@Serializable
data class Dish(
    val id: Int,
    val rating: Double,
    val specificAllergics: List<String>?,
    val ratingCount: Int,
    val mensa_vital: Boolean,
    val lastUpdated: Int,
    val dispositionPriority: Int,
    val feedback: Feedback?,
    val type: FoodType,
    val additionals: List<String>,
    val price: Double,
    val image: Image?,
    val allergics: List<String>,
    val bio: Boolean,
    val __typename: String,
    val name: String
)

@Serializable
enum class FoodType {
    VEGAN,
    MEATLESS,
    BEEF,
    PORK,
    POULTRY,
    FISH,
    VENISON,

    POULTRY_FISH,
    PORK_FISH,
    UNKNOWN;

    val displayName: String
        get() = when (this) {
            VEGAN -> "Vegan"
            MEATLESS -> "Meatless"
            BEEF -> "Beef"
            PORK -> "Pork"
            POULTRY -> "Poultry"
            FISH -> "Fish"
            VENISON -> "Venison"
            POULTRY_FISH -> "PoultryFish"
            PORK_FISH -> "PorkFish"
            UNKNOWN -> "Unknown"
        }
}

@Serializable
data class Feedback(
    val __typename: String,
    val id: Int,
    val tasteRating: Int,
    val message: String?,
    val priceRating: Int,
    val location: String? = null        // This is only sent on setFeedback
)

// Serializables for setFeedbackUpdate
@Serializable
data class FeedbackResponseWrapper(
    val data: FeedbackData
)

@Serializable
data class FeedbackData(
    val setFeedback: Feedback
)

// Serializables for Informations

@Serializable
data class InfoResponseWrapper(
    val data: InfoData
)

@Serializable
data class InfoData(
    val informations: List<InfoItem>
)

@Serializable
data class InfoItem(
    val __typename: String,
    val id: Int,
    val title: String,
    val content: String
)

// Serializables for Locations

@Serializable
data class LocationWrapper(
    val data: LocationData
)

@Serializable
data class LocationData(
    val locations: List<Location>
)

@Serializable
data class Location(
    val openingHours: String,
    val description: String,
    val id: Int,
    val image: Image,
    val coordinates: Coordinates,
    val type: LocationType,
    val name: String
)

@Serializable
data class Coordinates(
    val latitude: Double,
    val longitude: Double,
    val __typename: String
)

enum class LocationType {
    BISTRO,
    BIERGARTEN,
    KAFFEEBAR,
    MENSA;

    val displayName: String
        get() = when (this) {
            BISTRO -> "Bistro"
            BIERGARTEN -> "Biergarten"
            KAFFEEBAR -> "Kaffeebar"
            MENSA -> "Mensa"
        }
}

//expect suspend fun loadSystemImage(url: String): SystemImage?

//class MensaManager(val auth: String) {
object MensaManager {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = 3)
            exponentialDelay()
        }

        installPersistentCache(
            CacheConfig(
                enabled = true,
                cacheDirectory = "http_cache",
                maxCacheSize = 50L * 1024 * 1024,           // 50 MB
                cacheTtl = 14 * 24 * 60 * 60 * 1000,        // 14 days
                isShared = true,
                isPublic = true
            )
        )

//        install("Test") {
//            receivePipeline.intercept(HttpReceivePipeline.Before) { response ->
//                println(response)
//            }
//        }
    }

    private val _teasers = MutableStateFlow<List<Teaser>>(emptyList())
    val teasers = _teasers.asStateFlow()

    private val _menuItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val menuItems = _menuItems.asStateFlow()

    private val _infoItems = MutableStateFlow<List<InfoItem>>(emptyList())
    val infoItems = _infoItems.asStateFlow()

    private val _locations = MutableStateFlow<List<Location>>(emptyList())
    val locations = _locations.asStateFlow()

    @OptIn(ExperimentalUuidApi::class)
    private val _auth: MutableStateFlow<String> = dataStore.persistentStateFlow(keyName = "canteenAuth", defaultValue = Uuid.generateV4().toString())
    val auth = _auth.asStateFlow()

    private val _feedbackSent: MutableStateFlow<Boolean> = dataStore.persistentStateFlow(keyName = "canteenFeedbackSent", defaultValue = false)
    val feedbackSent = _feedbackSent.asStateFlow()

    private val _status: MutableStateFlow<String> = dataStore.persistentStateFlow(keyName = "canteenStatus", defaultValue = "STUDENT")
    val status = _status.asStateFlow()

    private val _lang: MutableStateFlow<String> = dataStore.persistentStateFlow(keyName = "canteenLanguage", defaultValue = "DE")
    val lang = _lang.asStateFlow()

    private val _defaultCanteen: MutableStateFlow<Int> = dataStore.persistentStateFlow(keyName = "defaultCanteen", defaultValue = 0)
    val defaultCanteen = _defaultCanteen.asStateFlow()

    private val _selectedCanteen: MutableStateFlow<Int> = dataStore.persistentStateFlow(keyName = "selectedCanteen", defaultValue = 1)
    val selectedCanteen = _selectedCanteen.asStateFlow()

    private val _disabledFoodTypes: MutableStateFlow<List<FoodType>> = dataStore.persistentStateFlow(keyName = "canteenDisabledFoodTypes", defaultValue = emptyList())
    val disabledFoodTypes = _disabledFoodTypes.asStateFlow()

    private val _enabledAllergies: MutableStateFlow<List<String>> = dataStore.persistentStateFlow(keyName = "canteenEnabledAllergies", defaultValue = emptyList())
    val enabledAllergies = _enabledAllergies.asStateFlow()


    private val _cardID: MutableStateFlow<String> = dataStore.persistentStateFlow(keyName = "cardID", defaultValue = "")
    val cardID = _cardID.asStateFlow()

    private val _cardBalance: MutableStateFlow<String> = dataStore.persistentStateFlow(keyName = "cardBalance", defaultValue = "0.00")
    val cardBalance = _cardBalance.asStateFlow()

    private val _cardLastBalance: MutableStateFlow<String> = dataStore.persistentStateFlow(keyName = "cardLastBalance", defaultValue = "0.00")
    val cardLastBalance = _cardLastBalance.asStateFlow()

    private val _cardLastScanDate: MutableStateFlow<String> = dataStore.persistentStateFlow(keyName = "cardLastScanDate", defaultValue = "???")
    val cardLastScanDate = _cardLastScanDate.asStateFlow()

    private val _cardLastScanTime: MutableStateFlow<String> = dataStore.persistentStateFlow(keyName = "cardLastScanTime", defaultValue = "???")
    val cardLastScanTime = _cardLastScanTime.asStateFlow()

    @OptIn(ExperimentalUuidApi::class)
    suspend fun initializeStates() {
        coroutineScope {
            async { getTeasers() }
            async { getCurrentMenuItems() }
            async { getInfoItems() }
            async { getLocations() }
        }
    }

    fun updateCard(cardID: String, cardBalance: String, cardLastBalance: String) {
        _cardID.value = cardID
        _cardBalance.value = cardBalance
        _cardLastBalance.value = cardLastBalance

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        _cardLastScanDate.value = now.format(
            LocalDateTime.Format {
                day(); char('.'); monthNumber(); char('.'); year()
            }
        )

        _cardLastScanTime.value = now.format(
            LocalDateTime.Format {
                hour(); char(':'); minute()
            }
        )
    }

    suspend fun updateStatus(newStatus: String) {
        _status.value = newStatus
        getCurrentMenuItems()
    }

    suspend fun updateLang(newLang: String) {
        _lang.value = newLang
        getCurrentMenuItems()
    }

    suspend fun updateDefaultCanteen(newCanteen: Int) {
        if (defaultCanteen.value != 0) {
            _selectedCanteen.value = newCanteen
            _defaultCanteen.value = newCanteen
            getCurrentMenuItems()
        } else {
            _defaultCanteen.value = newCanteen
        }
    }

    suspend fun updateSelectedCanteen(newCanteen: Int) {
        _selectedCanteen.value = newCanteen
        getCurrentMenuItems()
    }

    fun updateDisabledFoodTypes(newFoodTypes: List<FoodType>) {
        _disabledFoodTypes.value = newFoodTypes
    }

    fun updateEnabledAllergies(newAllergies: List<String>) {
        _enabledAllergies.value = newAllergies
    }

    suspend fun getTeasers() {
        val request = GraphQLRequest(
            query = """
                    query Teasers {
                        teasers {
                            __typename
                            ... on Teaser {
                                id
                                subtitle
                                title
                                lastUpdated
                                image {
                                    __typename
                                    url
                                }
                            }
                            ... on InAppTeaser {
                                detailTitle
                                detailText
                                infoText
                                infoTitle
                            }
                            ... on ExternalTeaser {
                                url
                            }
                        }
                   }
                   """
        )

        val httpResponse: HttpResponse = client.safePost("https://mensa.k8s.incloud.de/graphql") {
            // I have no idea, whether or not this is user specific...
            header("Authorization", auth.value)
            contentType(ContentType.Application.Json)
            setBody(request)
        } ?: return
        val responseWrapper: ResponseWrapper = httpResponse.body()

        coroutineScope {
            val updatedTeasers = responseWrapper.data.teasers.map { item ->
                async {
//                    val imageData: ByteArray? = client.safeGet(item.image.url)?.body()
//                    item.copy(image = item.image.copy(imageData = imageData))

                    val imageData: ByteArray = client.safeGet(item.image.url)?.body() ?: return@async item.copy()
                    item.copy(image = item.image.copy(platformImage = PlatformImage(imageData)))

//                    val image = loadSystemImage(item.image.url)
//                    item.copy(image = item.image.copy(systemImage = image))
//                    //item.image.systemImage = loadSystemImage(item.image.url)
                }
            }

            _teasers.value = updatedTeasers.awaitAll()
        }
    }

    suspend fun getCurrentMenuItems() {
        getCurrentMenuItems(selectedCanteen.value, status.value, lang.value)
    }

    /**
     * Updates menu items for mon-fri. If its sat/sun then do it for next week
     * Uses current day to determine week
     */
    @OptIn(ExperimentalTime::class)
    suspend fun getCurrentMenuItems(canteenId: Int, status: String, lang: String/*, todayDate: Int*/) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val dayOfWeek = today.dayOfWeek

        // If it's Sat/Sun, get menuItems for next week
        val isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
        val offset = if (isWeekend) 7 else 0

        // Calculate how many days to subtract to get to Monday (1) and to add to get to Friday (5)
        val monday = today.minus(dayOfWeek.ordinal, DateTimeUnit.DAY).plus(offset, DateTimeUnit.DAY)
        val friday = monday.plus(4, DateTimeUnit.DAY)

        val midnight = today.atStartOfDayIn(TimeZone.UTC)
        val epochSeconds = midnight.epochSeconds.toInt()

        // toString() defaults to "yyyy-mm-dd" on LocalDate, which getMenuItems needs
        //getMenuItems(canteenId, status, lang, monday.toString(), friday.toString(), todayDate)
        getMenuItems(canteenId, status, lang, monday.toString(), friday.toString(), epochSeconds)
    }

    /**
     * Updates menu items from provided canteen for a given date range (usually mon-fri).
     *
     * @param canteenId ID of the canteen
     * @param status User type such as "GUEST" or "STUDENT"
     * @param lang Language choices include "DE" or "EN"
     * @param minDate starting date, usually monday, in "yyyy-mm-dd" format
     * @param maxDate ending date, usually friday, in "yyyy-mm-dd" format
     * @return Void; updates menuItems object attribute
     */
    private suspend fun getMenuItems(canteenId: Int, status: String, lang: String, minDate: String, maxDate: String, todayDate: Int) {
        val query = """
                    query MenuItems(${'$'}canteenId: ID!, ${'$'}status: UserStatus!, ${'$'}lang: Language!, ${'$'}minDate: String, ${'$'}maxDate: String) {
                      menuItems(canteen: ${'$'}canteenId, minDate: ${'$'}minDate, maxDate: ${'$'}maxDate) {
                        __typename
                        id
                        date
                        dish {
                          __typename
                          id
                          type
                          rating
                          ratingCount
                          dispositionPriority
                          name(language: ${'$'}lang)
                          price(status: ${'$'}status)
                          image {
                            __typename
                            id
                            url
                            thumbUrl
                          }
                          allergics
                          specificAllergics
                          additionals
                          mensa_vital
                          bio
                          feedback {
                            __typename
                            id
                            tasteRating
                            priceRating
                            message
                          }
                          lastUpdated
                        }
                        issuingOffice {
                          __typename
                          id
                          name
                          canteenId
                        }
                        lastUpdated
                      }
                    }
                    """
        val variables = mapOf(
            "canteenId" to JsonPrimitive(canteenId),
            "status" to JsonPrimitive(status),
            "lang" to JsonPrimitive(lang),
            "minDate" to JsonPrimitive(minDate),
            "maxDate" to JsonPrimitive(maxDate)
        )

        val request = GraphQLRequest(
            query = query,
            variables = variables
        )

        val httpResponse: HttpResponse = client.safePost("https://mensa.k8s.incloud.de/graphql") {
            // This code is different for each User, but I don't know in which way; This one works?
            header("Authorization", auth.value)
            contentType(ContentType.Application.Json)
            setBody(request)
        } ?: return

        val responseWrapper: MenuResponseWrapper = httpResponse.body()

        coroutineScope {
//            val imageCache = mutableMapOf<String, Deferred<SystemImage?>>()
            val imageCache = mutableMapOf<String, Deferred<ByteArray?>>()
//            val imageCache = mutableMapOf<String, Deferred<PlatformImage?>>()

            var updatedMenuItems = responseWrapper.data.menuItems
            _menuItems.value = updatedMenuItems

            var loadTodayItems = true

            // Load today's items then rest of week
            repeat(2) {
                updatedMenuItems = updatedMenuItems.map { menuItem ->
                    async {
                        val shouldLoad = if (loadTodayItems) {
                            menuItem.date == todayDate
                        } else {
                            menuItem.date != todayDate
                        }

                        if (shouldLoad) {
                            val image = menuItem.dish.image?.let { image ->
                                val deferredImage = imageCache.getOrPut(image.url) {
//                                    async { loadSystemImage(image.url) }
                                    async { client.safeGet(image.url)?.body() }
                                }
//                                val systemImage = deferredImage.await()
//                                image.copy(imageData = systemImage)

//                                val imageData = deferredImage.await()
//                                image.copy(imageData = imageData)

                                val imageData = deferredImage.await() ?: return@let image.copy()
                                image.copy(platformImage = PlatformImage(imageData))
                            } ?: Image(
                                __typename = "NoImageExists",
                                url = "",
//                                systemImage = null
//                                imageData = null
                                platformImage = null
                            )

                            menuItem.copy(dish = menuItem.dish.copy(image = image))
                        } else {
                            menuItem
                        }
                    }
                }.awaitAll()

                _menuItems.value = updatedMenuItems
                loadTodayItems = false
            }

            /*
            var isFirstLoad = true     // Decides whether to load todays or other images
            var updatedMenuItem = responseWrapper.data.menuItems

            repeat(2) {
                updatedMenuItem = updatedMenuItem.map { menuItem ->
                    if (if (isFirstLoad) menuItem.date == todayDate else menuItem.date != todayDate) {
                        val image = menuItem.dish.image?.let { image ->
                            val deferredImage = imageCache.getOrPut(image.url) {
                                async {
                                    loadSystemImage(image.url)
                                }
                            }

                            val systemImage = deferredImage.await()
                            image.copy(systemImage = systemImage)
                        } ?: Image(
                            __typename = "NoImageExists",
                            url = "",
                            systemImage = null
                        )

                        val dish = menuItem.dish.copy(
                            image = image
                        )

                        menuItem.copy(dish = dish)
                    } else {
                        menuItem.copy()
                    }
                }

                _menuItems.value = updatedMenuItem
                isFirstLoad = false
            }
            */

            /*
            val updatedMenuItems = responseWrapper.data.menuItems.map { menuItem ->
                async {
                    // If image is not null, create new image with preloaded systemImage
                    val image = menuItem.dish.image?.let { image ->
                        val deferredImage = imageCache.getOrPut(image.url) {
                            async {
                                loadSystemImage(image.url)
                            }
                        }

                        val systemImage = deferredImage.await()
                        image.copy(systemImage = systemImage)
                    } ?: Image(
                        __typename = "NoImageExists",
                        url = "",
                        systemImage = null
                    )

                    val dish = menuItem.dish.copy(
                        image = image
                    )

                    menuItem.copy(dish = dish)
                }
            }

            _menuItems.value = updatedMenuItems.awaitAll()
             */
        }
    }

    suspend fun getSpecificMenuItems(ids: List<Int>, lang: String, status: String): List<MenuItem> {
        try {
            val query = """
                        query Dishes(${'$'}ids: [ID!]!, ${'$'}status: UserStatus!, ${'$'}lang: Language!) {
                          dishes(ids: ${'$'}ids) {
                            __typename
                            id
                            type
                            rating
                            ratingCount
                            name(language: ${'$'}lang)
                            price(status: ${'$'}status)
                            image {
                              __typename
                              id
                              url
                              thumbUrl
                            }
                            allergics
                            specificAllergics
                            additionals
                            mensa_vital
                            bio
                            feedback {
                              __typename
                              id
                              tasteRating
                              priceRating
                              message
                            }
                            lastUpdated
                          }
                        }
                        """
            val variables: Map<String, JsonElement> = mapOf(
                "lang" to JsonPrimitive(lang),
                "status" to JsonPrimitive(status),
                "ids" to JsonArray(ids.map { JsonPrimitive(it)})
            )

            val request = GraphQLRequest(
                query = query,
                variables = variables
            )

            val httpResponse: HttpResponse = client.safePost("https://mensa.k8s.incloud.de/graphql") {
                // This code is different for each User, but I don't know in which way; This one works?
                header("Authorization", auth.value)
                contentType(ContentType.Application.Json)
                setBody(request)
            } ?: return emptyList()
            val responseWrapper: MenuResponseWrapper = httpResponse.body()

            val menuItems = coroutineScope {
//                val imageCache = mutableMapOf<String, Deferred<SystemImage?>>()
                val imageCache = mutableMapOf<String, Deferred<ByteArray?>>()
                var tmpMenuItems = responseWrapper.data.menuItems

                tmpMenuItems = tmpMenuItems.map { menuItem ->
                    val image = menuItem.dish.image?.let { image ->
                        val deferredImage = imageCache.getOrPut(image.url) {
                            async {
//                                loadSystemImage(image.url)
                                client.safeGet(image.url)?.body()
                            }
                        }

//                        val systemImage = deferredImage.await()
//                        image.copy(systemImage = systemImage)

//                        val imageData = deferredImage.await()
//                        image.copy(imageData = imageData)

                        val imageData = deferredImage.await() ?: return@let image.copy()
                        image.copy(platformImage = PlatformImage(imageData))
                    } ?: Image(
                        __typename = "NoImageExists",
                        url = "",
//                        systemImage = null
//                        imageData = null
                        platformImage = null
                    )

                    val dish = menuItem.dish.copy(
                        image = image
                    )

                    menuItem.copy(dish = dish)
                }

                return@coroutineScope tmpMenuItems
            }

            return menuItems

        } catch (e: Exception) {
            println("Request failed: ${e.message}")
            return emptyList()
        }
    }

    suspend fun getInfoItems() {
        val request = GraphQLRequest(
            query = """
                    query Informations {
                        informations {
                            __typename
                            id
                            title
                            content
                        }
                   }
                   """
        )

        val httpResponse: HttpResponse = client.safePost("https://mensa.k8s.incloud.de/graphql") {
            // This code is different for each User, but I don't know in which way; This one works?
            header("Authorization", auth.value)
            contentType(ContentType.Application.Json)
            setBody(request)
        } ?: return

        val responseWrapper: InfoResponseWrapper = httpResponse.body()
        _infoItems.value = responseWrapper.data.informations
    }

    suspend fun getLocations() {
        val request = GraphQLRequest(
            query = """
                    query Locations {
                      locations {
                        __typename
                        id
                        name
                        coordinates {
                          __typename
                          latitude
                          longitude
                        }
                        type
                        openingHours
                        description
                        image {
                          __typename
                          id
                          url
                        }
                      }
                    }
                   """
        )

        val httpResponse: HttpResponse = client.safePost("https://mensa.k8s.incloud.de/graphql") {
            // This code is different for each User, but I don't know in which way; This one works?
            header("Authorization", auth.value)
            contentType(ContentType.Application.Json)
            setBody(request)
        } ?: return

        val responseWrapper: LocationWrapper = httpResponse.body()

        coroutineScope {
            val updatedLocations = responseWrapper.data.locations.map { item ->
                async {
//                    val imageData: ByteArray? = client.safeGet(item.image.url)?.body()
//                    item.copy(image = item.image.copy(imageData = imageData))

                    val imageData: ByteArray = client.safeGet(item.image.url)?.bodyAsBytes() ?: return@async item.copy()
                    item.copy(image = item.image.copy(platformImage = PlatformImage(imageData)))

//                    val image = loadSystemImage(item.image.url)
//                    item.copy(image = item.image.copy(systemImage = image))
//                    //item.image.systemImage = loadSystemImage(item.image.url)
                }
            }

            _locations.value = updatedLocations.awaitAll()
        }
    }

    suspend fun sendFeedback(tasteRating: Int, priceRating: Int, message: String, location: String, dishId: Int) {
        // In Demo Mode, feedback shouldn't be counted
        if (IdentityManager.tuLoginStatus.value == TULoginState.DEMO) run {
            delay(2.seconds)
            return
        }

        val query = """
                mutation Feedback(${'$'}feedback: SetFeedbackInput!) {
                  setFeedback(feedback: ${'$'}feedback) {
                    __typename
                    id
                    tasteRating
                    priceRating
                    location
                    message
                  }
                }
                """
        val variables: Map<String, JsonElement> = mapOf(
            "feedback" to JsonObject(
                mapOf(
                    "tasteRating" to JsonPrimitive(tasteRating),
                    "priceRating" to JsonPrimitive(priceRating),
                    "message" to JsonPrimitive(message.trim().takeIf { it.isNotEmpty() }),
                    "location" to JsonPrimitive(location),
                    "dishId" to JsonPrimitive(dishId)
                )
            )
        )

        val request = GraphQLRequest(
            query = query,
            variables = variables
        )

        val httpResponse: HttpResponse = client.safePost("https://mensa.k8s.incloud.de/graphql") {
            header("Authorization", auth.value)
            contentType(ContentType.Application.Json)
            setBody(request)
        } ?: return

        _feedbackSent.value = true

        val responseWrapper: FeedbackResponseWrapper = httpResponse.body()
        val newFeedback = responseWrapper.data.setFeedback

        val newMenuItems = _menuItems.value.map { item ->
            if (dishId == item.dish.id) {
                val newDish = item.dish.copy(feedback = newFeedback)
                return@map item.copy(dish = newDish)
            } else {
                return@map item.copy()
            }
        }

        _menuItems.value = newMenuItems
    }


    suspend fun createDebugTeasers() {
        val imageData = client.safeGet("https://s3.eu-central-1.amazonaws.com/mensa-production/Teaser_Kartenzahlung_final_(1000_x_667_px).jpg")?.bodyAsBytes()

        val image = Image(
            __typename = "Image",
            url = "https://s3.eu-central-1.amazonaws.com/mensa-production/Teaser_Kartenzahlung_final_(1000_x_667_px).jpg",
//            systemImage = loadSystemImage("https://s3.eu-central-1.amazonaws.com/mensa-production/Teaser_Kartenzahlung_final_(1000_x_667_px).jpg")
//            imageData = client.safeGet("https://s3.eu-central-1.amazonaws.com/mensa-production/Teaser_Kartenzahlung_final_(1000_x_667_px).jpg")?.body()
            platformImage = if (imageData != null) PlatformImage(imageData) else null
        )

        val teasers = listOf(
            Teaser(
                __typename = "InAppTeaser",
                id = 92,
                subtitle = "",
                title = "*",
                lastUpdated = 1753335888,
                image = image,
                detailTitle = "Easy & Schnell",
                infoText = "ab sofort gibt es BlaBlaBla",
                infoTitle = "Liebe Gäste,",
            ),
            Teaser(
                __typename = "ExternalTeaser",
                id = 213,
                title = "Herz",
                lastUpdated = 1737970289,
                image = image,
                subtitle = null
            ))

        _teasers.value = teasers
    }

    suspend fun createDebugLocations() {
        val imageData = client.safeGet("https://s3.eu-central-1.amazonaws.com/mensa-production/mensa-Stadtmitte_test.jpg")?.bodyAsBytes()

        val image = Image(
            __typename = "Image",
            url = "https://s3.eu-central-1.amazonaws.com/mensa-production/mensa-Stadtmitte_test.jpg",
//            systemImage = loadSystemImage("https://s3.eu-central-1.amazonaws.com/mensa-production/mensa-Stadtmitte_test.jpg")
//            imageData = client.safeGet("https://s3.eu-central-1.amazonaws.com/mensa-production/mensa-Stadtmitte_test.jpg")?.body()
            platformImage = if (imageData != null) PlatformImage(imageData) else null
        )

        val coordinates = Coordinates(
            latitude = 49.875958,
            longitude = 8.657824,
            __typename = "Coordinates"
        )

        val location = Location(
            openingHours = "\uD83D\uDD57 Mo - Do 08.00 - 16.00 Uhr\n" +
                    "       Fr 08.00 - 15.00 Uhr\n" +
                    "\n",
            description = "Alexanderstr. 4\n" +
                    "64283 Darmstadt\n" +
                    "\n" +
                    "Betriebskoordination:\n" +
                    "Herr Florian Eck\n" +
                    "E-Mail: f.eck@stwda.de\n" +
                    "Tel. (06151) 16-29420",
            id = 1,
            image = image,
            coordinates = coordinates,
            type = LocationType.BISTRO,
            name = "Geöffnet: Bistro Stadtmitte"
        )

        val locations = listOf(location)

        _locations.value = locations
    }

    @OptIn(ExperimentalTime::class)
    suspend fun createDebugMenuItems() {
        val issuingOffice = IssuingOffice(
            __typename = "IssuingOffice",
            id = 303,
            canteenId = 1,
            name = "Marktrestaurant",
        )

        val imageData = client.safeGet("https://s3.eu-central-1.amazonaws.com/mensa-production/WOK_Tofu_Pad_Thai_mit_Reisnudeln.jpg")?.bodyAsBytes()

        val image = Image(
            __typename = "Image",
            url = "https://s3.eu-central-1.amazonaws.com/mensa-production/WOK_Tofu_Pad_Thai_mit_Reisnudeln.jpg",
//            systemImage = loadSystemImage("https://s3.eu-central-1.amazonaws.com/mensa-production/WOK_Tofu_Pad_Thai_mit_Reisnudeln.jpg")
//            imageData = client.safeGet("https://s3.eu-central-1.amazonaws.com/mensa-production/WOK_Tofu_Pad_Thai_mit_Reisnudeln.jpg")?.body()
            platformImage = if (imageData != null) PlatformImage(imageData) else null
        )

        val dish = Dish(
            id = 8717,
            rating = 3.98,
            specificAllergics = listOf("A1"),
            ratingCount = 59,
            mensa_vital = false,
            lastUpdated = 1752485848,
            dispositionPriority = 2,
            feedback = null,
            type = FoodType.VEGAN,
            additionals = listOf("2"),
            price = 3.0,
            image = image,
            allergics = listOf("A", "E", "F"),
            bio = false,
            __typename = "dish",
            name = "WOK Tofu Pad Thai mit Reisnudeln",
        )

        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val midnight = today.atStartOfDayIn(TimeZone.UTC)
        val date: Int = midnight.epochSeconds.toInt()

        val menuItem = MenuItem(
            issuingOffice = issuingOffice,
            __typename = "MenuItem",
            id = "1007a0accff267ca76da237220a5bd24",
            lastUpdated = 1761615108,
            dish = dish,
            date = date
        )

        _menuItems.value = listOf(menuItem)
    }
}
