package tuda.campus.mobile.managers

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import tuda.campus.mobile.EncryptedValue
import tuda.campus.mobile.dataStore
import tuda.campus.mobile.generateTotpPairFromBase32
import tuda.campus.mobile.managers.tucanManager.RequestStatus
import tuda.campus.mobile.persistentEncryptedString
import tuda.campus.mobile.persistentStateFlow
import tuda.campus.mobile.safeGet
import tuda.campus.mobile.safeSubmitForm
import tuda.campus.mobile.settings
import tuda.campus.mobile.sha256
import tuda.campus.mobile.vault
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.parameters
import io.ktor.http.setCookie
import io.ktor.util.encodeBase64
import korlibs.time.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.CoroutineContext
import kotlin.io.encoding.Base64
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration
import kotlin.time.Instant


data class TucanCredentials @OptIn(ExperimentalTime::class) constructor(
    val sessionId: String,
    val cookie: Cookie,
    val tokenCreationTime: Instant
)

@Serializable
data class StudentID(
    val studentCard: StudentCard?,
    val euroStudentID: String?,
    val libraryCardID: String?,
)

@Serializable
data class StudentCard(
    val semester: String,
    val lastName: String,
    val firstName: String,
    val lastValidDate: String,
    val matriNumber: String,
    val birthdate: String,
)

enum class TULoginState {
    NONE,
    MFA,                    // MFA is a legacy login type once used when people gave their credentials
    MFA_UNCONFIRMED,        // Unconfirmed is used, when the last login was of this type
    MFA_TUCAN_ERROR,
    SSO,
    SSO_UNCONFIRMED,
    SSO_INVALID,            // Used, when token refresh fails, requires new login
    DEMO
}


@Serializable
data class OAuthTokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val refresh_token: String,
    val id_token: String,
    val scope: String
)

private fun getIdentityClient(): HttpClient {
    return HttpClient {
        /*
        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = 3)
            exponentialDelay()
        }
         */

        install(HttpCookies)
    }
}

/**
 * A coroutineScope that provides an additional `reset()` method,
 * which cancels all tasks but allows new ones to start from then on.
 */
class ResettableCoroutineScope(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : CoroutineScope {
    private var job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = dispatcher + job

    fun reset() {
        job.cancelChildren()   // Cancel current coroutines
        job = SupervisorJob()  // Replace Job so new coroutines can be launched
    }

    fun close() {
        job.cancel()
    }
}

@OptIn(ExperimentalTime::class)
object IdentityManager {
    private var client = getIdentityClient()
    private val postLoginScope = ResettableCoroutineScope()

    
    private val _tuUsername = settings.persistentStateFlow("TUsername", "")
    val tuUsername = _tuUsername.asStateFlow()

    private var tuPassword: EncryptedValue<String> = vault.persistentEncryptedString("TuPassword", "")
    private var tuTotpSecret: EncryptedValue<String> = vault.persistentEncryptedString("TuTotpSecret", "")

    // This is used to prevent tuTotpSecret actually being stored anywhere
    private val _tuTotpSecretExists = settings.persistentStateFlow("TuTotpSecret", false)
    val tuTotpSecretExists = _tuTotpSecretExists.asStateFlow()

    private val _tuLoginStatus = settings.persistentStateFlow("TuLoginState", TULoginState.NONE)
    val tuLoginStatus = _tuLoginStatus.asStateFlow()

    private var tucanAccessToken: EncryptedValue<String> = vault.persistentEncryptedString("TucanAccessToken", "")
    private var tucanRefreshToken: EncryptedValue<String> = vault.persistentEncryptedString("TucanRefreshToken", "")
    private var tucanIdToken: EncryptedValue<String> = vault.persistentEncryptedString("TucanIdToken", "")
    private var tucanAccessExpiry: MutableStateFlow<Long> = dataStore.persistentStateFlow("TucanAccessExpiry", 0)
    private var tucanCredentials: TucanCredentials? = null


    private val _studentID: MutableStateFlow<StudentID?> = dataStore.persistentStateFlow("StudentID", null)
    val studentID = _studentID.asStateFlow()

    private val _studentIDStatus = MutableStateFlow(RequestStatus.READY)
    val studentIDStatus = _studentIDStatus.asStateFlow()


    // Everything above is initialized at this point
    init {
        if (tuLoginStatus.value == TULoginState.MFA || tuLoginStatus.value == TULoginState.MFA_TUCAN_ERROR) {
            SettingsManager.updateMfaLoginAvailability(true)
            _tuLoginStatus.value = TULoginState.MFA_UNCONFIRMED
        }
        if (tuLoginStatus.value == TULoginState.SSO || tuLoginStatus.value == TULoginState.SSO_INVALID) {
            _tuLoginStatus.value = TULoginState.SSO_UNCONFIRMED
        }

        CoroutineScope(Dispatchers.Default).launch  {
            tuRefreshLogin()
        }
    }

    /**
     * Meant for enabling previews with dummy data
     */
    fun initializeDebugStates() {
        _tuLoginStatus.value = TULoginState.MFA
        _tuUsername.value = "DebugUser"
        _studentID.value = StudentID(
            studentCard = StudentCard(
                semester = "WiSe 2025/2026",
                lastName = "Verylonglastname",
                firstName = "RealGuy",
                lastValidDate = "77.07.2007",
                matriNumber = "1234567",
                birthdate = "37.12.2042"),
            euroStudentID = "urn:schac:personalUniqueCode:int:esi:tu-darmstadt.de:1234567",
            libraryCardID = "012345678910")
    }


    // General TU ID functions

    /**
     * Sets stored TU credentials to [username] and [password]
     */
    private fun setTuCredentials(username: String, password: String) {
        _tuUsername.value = username
        runBlocking { tuPassword.set(password) }
    }

    /**
     * Sets stored [tuTotpSecret] to passed [totpSecret]
     */
    private fun setTotpSecret(totpSecret: String) {
        runBlocking { tuTotpSecret.set(totpSecret) }
        _tuTotpSecretExists.value = true
    }

    /**
     * Resets the stored [tuTotpSecret] to `""`
     */
    fun resetTotpSecret() {
        runBlocking { tuTotpSecret.reset() }
        _tuTotpSecretExists.value = false
    }


    // The login flow is as follows:
    
    // First login: tuLogin() for MFA Login or tucanLogin() for SSO Login
    // After that: tuRefreshLogin()
    
    // If the user logged out and then logs back in, its identical to first login
    
    // All Int return types shall follow this convention:
    // -1: unknown error, -2: invalid credentials,
    // -3: missing totp token, -4: wrong token entered
    // otherwise internet status code (e.g. 200: success, 404: not found, etc.)

    fun tuDemoLogin() {
        _tuLoginStatus.value = TULoginState.DEMO
        postLoginScope.launch { TucanManager.initializeStates() }
    }

    suspend fun tuLogin(username: String, password: String): Int {
        if (tuTotpSecret.await() == "") return -4
        return tuLogin(username, password, tuTotpSecret.await())
    }

    /**
     * Performs 2FA TU login using provided totp secret, username and password.
     *
     * Only useful if called after successful [tuBasicLogin()][tuBasicLogin]
     *
     * If successful, starts loading everything from TUCaN and StudentID.
     *
     * **Warning**: This will fail if called too rapidly, see [tuWebLogin()][tuWebLogin]
     *
     * @param totpSecret The user provided totp Secret. 2FA-token must contain name "TUPlan"
     * @return Int status code following the convention for login returns:
     *   - `-1`: unknown error
     *   - `-2`: invalid credentials
     *   - `-3`: missing TOTP token
     *   - `-4`: wrong token entered
     *   - Otherwise: HTTP status code (`200` = success, `404` = not found, etc.)
     */
    suspend fun tuLogin(username: String, password: String, totpSecret: String): Int {
        val status = tuWebLogin(username, password, totpSecret)

        if (status == 200) {
            setTuCredentials(username, password)
            setTotpSecret(totpSecret)
            createTucanCredentialsFromMFALogin()

            if (tucanCredentials != null) {
                _tuLoginStatus.value = TULoginState.MFA
                postLoginScope.launch { updateStudentID() }
                postLoginScope.launch { TucanManager.initializeStates() }
            } else {
                _tuLoginStatus.value = TULoginState.MFA_TUCAN_ERROR
            }
        }

        return status
    }

    /**
     * Log in with TU ID based on previous successful login.
     *
     * Results in either mfa, basic or no login.
     *
     * @return Int status code following the convention for login returns:
     *   - `-1`: unknown error
     *   - `-2`: invalid credentials
     *   - `-3`: no token named "tuplan" exists in TUDa 2FA-Management
     *   - `-4`: wrong token entered
     *   - Otherwise: HTTP status code (`200` = success, `404` = not found, etc.)
     */
    suspend fun tuRefreshLogin(): Int {
        if (tuLoginStatus.value == TULoginState.DEMO) {
            postLoginScope.launch { TucanManager.initializeStates() }
        }
        else if (tuLoginStatus.value == TULoginState.SSO_UNCONFIRMED || tuLoginStatus.value == TULoginState.SSO_INVALID) {
            if (tucanAccessToken.await().isNotEmpty()) {
                val credentials = updateTucanCredentials()
                if (credentials != null) {
                    _tuLoginStatus.value = TULoginState.SSO
                    postLoginScope.launch { TucanManager.initializeStates() }
                } else {
                    _tuLoginStatus.value = TULoginState.SSO_INVALID
                }
            } else {
                _tuLoginStatus.value = TULoginState.NONE
            }
        }
        else if (tuTotpSecret.await() != "" && tuLoginStatus.value != TULoginState.NONE) {
            _tuLoginStatus.value = TULoginState.MFA_UNCONFIRMED
            // If a tucanAccessToken exists, we can log in to tucan without SSO
            val tucanCredentialsJob = if (tucanAccessToken.await().isNotEmpty()) {
                postLoginScope.async { updateTucanCredentials() }
            } else null

            val status = tuWebLogin(tuUsername.value, tuPassword.await(), tuTotpSecret.await())

            if (status == 200) {
                setTotpSecret(tuTotpSecret.await())
                setTuCredentials(tuUsername.value, tuPassword.await())

                if (tucanAccessToken.await().isEmpty()) {
                    updateTucanCredentials()
                } else {
                    tucanCredentialsJob?.await()
                }

                if (tucanCredentials != null) {
                    _tuLoginStatus.value = TULoginState.MFA
                    postLoginScope.launch { updateStudentID() }
                    postLoginScope.launch { TucanManager.initializeStates() }
                } else {
                    _tuLoginStatus.value = TULoginState.MFA_TUCAN_ERROR
                }
            } else {
                _tuLoginStatus.value = TULoginState.NONE
            }

            return status
        }
        else {
            _tuLoginStatus.value = TULoginState.NONE
        }

        return -2
    }


    /**
     * Resets all states **except** [tuTotpSecret] and invalidates tucan tokens
     *
     * [tuTotpSecret] must be reset using [resetTotpSecret()][resetTotpSecret]
     */
    // tuTotpSecret should persist unless the user explicitly also removes it,
    // so that the user doesn't unexpectedly lose access to their TU ID
    suspend fun tuLogout() {
        postLoginScope.reset()
        _tuUsername.value = ""
        tuPassword.reset()
        _tuLoginStatus.value = TULoginState.NONE
        _studentIDStatus.value = RequestStatus.READY
        tucanCredentials = null
        _studentID.value = null
        TucanManager.resetStates()

        if (tucanAccessToken.await() != "") {
            client.safeGet("https://dsf.tucan.tu-darmstadt.de/IdentityServer/connect/endsession") {
                parameter("id_token_hint", tucanIdToken)
                parameter("post_logout_redirect_uri", "de.datenlotsen.campusnet.tuda://oauth2redirect")
                parameter("state", Random.nextBytes(32).base64Url())
            }       // If this fails, simply remove the tokens anyway
        }

        tucanAccessToken.reset()
        tucanRefreshToken.reset()
        tucanIdToken.reset()
        tucanAccessExpiry.value = 0

        client.close()
        client = getIdentityClient()
    }

    /**
     * Get the next two 2FA one-time-codes. Requires mfa-login.
     *
     * @return current (+0s) and next (+30s) one-time-codes for 2FA
     */
    fun getTotpCodePair(): Pair<String, String>? {
        val secret = runBlocking { tuTotpSecret.await() }
        return generateTotpPairFromBase32(secret)
    }



    // Internal TUCaN identity functions

    /**
     * @return The current [TucanCredentials] or `null` if none present
     */
    internal fun getTucanCredentials(): TucanCredentials? {
        return tucanCredentials
    }

    /**
     * Checks whether the stored [TucanCredentials] are still valid
     */
    @OptIn(ExperimentalTime::class)
    internal fun isTucanCredentialsExpired(): Boolean {
        return tucanCredentials?.tokenCreationTime == null ||
                Clock.System.now() - tucanCredentials!!.tokenCreationTime > 29.toDuration(DurationUnit.MINUTES)
    }

    /**
     * Updates [TucanCredentials] if login level is mfa.
     *
     * Requires TUCaN service to be reachable.
     *
     * @return The created instance of [TucanCredentials] or `null` if not successful
     */
    internal suspend fun updateTucanCredentials(): TucanCredentials? {
        if (tucanAccessToken.await() == "") {
            println("Used MFA")
            createTucanCredentialsFromMFALogin()        // sets tucanCredentials implicitly
        } else {
            println("Used code")
            tucanCredentials = createTucanCredentialsFromAuthCode()
        }

        return tucanCredentials
    }

    // TUCaN-only Login

    private val json = Json { ignoreUnknownKeys = true }
    /**
     * Takes a code provided by completing a TU ID Login in the SSO
     *
     * @param code the code returned by SSO
     * @param codeVerifier the string used to generate a codeChallenge
     */
    suspend fun tucanLogin(code: String, codeVerifier: String, nonce: String) {
        _tuLoginStatus.value = TULoginState.SSO_UNCONFIRMED
        exchangeCodeForToken(code, codeVerifier, nonce)
        _tuLoginStatus.value = TULoginState.SSO

        if (tucanCredentials != null) {
            postLoginScope.launch { TucanManager.initializeStates() }
        }
        // There exists no error handling, this case hasn't happened yet
    }

    /**
     * Creates tucan token based on successful login in SSO
     *
     * @param code the code returned by SSO
     * @param codeVerifier the string used to generate a codeChallenge
     * @param nonce a one time code that verifies the authenticity of the response
     */
    suspend fun exchangeCodeForToken(code: String, codeVerifier: String, nonce: String) {
        val response = client.safeSubmitForm(
            url = "https://dsf.tucan.tu-darmstadt.de/IdentityServer/connect/token",
            formParameters = parameters {
                append("code", code)
                append("code_verifier", codeVerifier)
                append("redirect_uri", "de.datenlotsen.campusnet.tuda://oauth2redirect")
                append("client_id", "MobileApp")
                append("grant_type", "authorization_code")
            }
        ) ?: return

        val tokenResponse: OAuthTokenResponse
        try {
            tokenResponse = json.decodeFromString(OAuthTokenResponse.serializer(), response.body())
        } catch (e: Exception) {
            return
        }

        // Check for nonce equality in id token

        val parts = tokenResponse.id_token.split(".")
        val payloadBase64 = parts[1]
        val padding = (4 - payloadBase64.length % 4) % 4
        val normalized = payloadBase64 + "=".repeat(padding)
        val payloadJson = Base64.UrlSafe.decode(normalized).decodeToString()
        val json = Json.parseToJsonElement(payloadJson).jsonObject
        val newNonce = json["nonce"]?.toString()?.trim('"')

        if (newNonce != nonce) {
            println("Nonce mismatch")
            return
        }

        tucanAccessToken.set(tokenResponse.access_token)
        tucanRefreshToken.set(tokenResponse.refresh_token)
        tucanIdToken.set(tokenResponse.id_token)

        val idParts = tokenResponse.id_token.split(".")
        if (idParts.size == 3) {
            val jwtBase64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

            val payload = idParts[1]
            val decodedBytes = jwtBase64.decode(payload)
            val decodedPayload = decodedBytes.decodeToString()

            val claimKey = "urn:oid:0.9.2342.19200300.100.1.1"
            val jsonObject = Json.parseToJsonElement(decodedPayload).jsonObject
            val username = jsonObject[claimKey]?.jsonPrimitive?.contentOrNull

            if (username != null) {
                _tuUsername.value = username
            }
        }

        val expiryInstant = Clock.System.now() + tokenResponse.expires_in.seconds
        tucanAccessExpiry.value = expiryInstant.toEpochMilliseconds() - 360000      // An hour buffer

        tucanCredentials = createTucanCredentialsFromAuthCode()
    }

    private suspend fun createTucanCredentialsFromAuthCode(): TucanCredentials? {
        suspend fun refreshTokens(): Boolean? {
            val refreshToken = tucanRefreshToken.await()
            val response = client.safeSubmitForm(
                url = "https://dsf.tucan.tu-darmstadt.de/IdentityServer/connect/token",
                formParameters = parameters {
                    append("refresh_token", refreshToken)
                    append("client_id", "MobileApp")
                    append("grant_type", "refresh_token")
                }
            ) ?: return null

            // Sometimes, the refresh will fail with {"error":"invalid_grant"} and status 400
            if (response.status.value == 400) {
                _tuLoginStatus.value = TULoginState.SSO_INVALID
                return null
            }

            val tokenResponse: OAuthTokenResponse
            try {
                tokenResponse = json.decodeFromString(OAuthTokenResponse.serializer(), response.body())
            } catch (e: Exception) {
                return null
            }

            tucanAccessToken.set(tokenResponse.access_token)
            tucanRefreshToken.set(tokenResponse.refresh_token)
            tucanIdToken.set(tokenResponse.id_token)

            val parts = tokenResponse.id_token.split(".")
            if (parts.size == 3) {
                val jwtBase64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

                val payload = parts[1]
                val decodedBytes = jwtBase64.decode(payload)
                val decodedPayload = decodedBytes.decodeToString()

                val claimKey = "urn:oid:0.9.2342.19200300.100.1.1"
                val jsonObject = Json.parseToJsonElement(decodedPayload).jsonObject
                val username = jsonObject[claimKey]?.jsonPrimitive?.contentOrNull

                if (username != null) {
                    _tuUsername.value = username
                }
            }

            val expiryInstant = Clock.System.now() + tokenResponse.expires_in.seconds
            tucanAccessExpiry.value = expiryInstant.toEpochMilliseconds() - 360000      // An hour buffer

            return true
        }

        val accessToken = tucanAccessToken.await()
        if (accessToken == "") return null

        val expiryInstant = Instant.fromEpochMilliseconds(tucanAccessExpiry.value)
        if (Clock.System.now() > expiryInstant) {
            refreshTokens() ?: return null
        }

        val response = client.safeSubmitForm(
            url = "https://www.tucan.tu-darmstadt.de/scripts/mgrqispi.dll",
            formParameters = parameters {
                append("PRGNAME", "LOGINCHECK")
                append("ids_mode", "M")
                append("ARGUMENTS", "-N000000000000001,ids_mode")
                append("access_token", accessToken)
                append("APPNAME", "CampusNet")
            }
        ) ?: return null

        if (response.bodyAsText().contains(Regex("Häufung fehlgeschlagener Einlog|Benutzerkonto vorübergehend gesperrt"))) {
            refreshTokens() ?: return null
        }

        val cookies: List<Cookie> = response.setCookie()
        if (cookies.isEmpty()) {
            return null
        }

        // When successfully logged in, TUCaN returns a REFRESH Header in the format
        // 0; URL=/scripts/mgrqispi.dll?APPNAME=CampusNet&PRGNAME=STARTPAGE_DISPATCH&ARGUMENTS=-NXXXXXXXXXXXXXXX,-N000019,-N000000000000000
        // where the first "-N value" is specific for the current session
        val headerToken: String = response.headers["REFRESH"]
            ?.substringAfter("ARGUMENTS=")
            ?.split(",")?.firstOrNull()?.trim()
            ?: return null

        return TucanCredentials(
            sessionId = headerToken,
            cookie = cookies.first(),
            tokenCreationTime = Clock.System.now()
        )
    }


    // MFA Login

    /**
     * Tries logging in to TU SSO with valid 2FA credentials.
     * By doing so, the [IdentityManager]'s [HttpClient] can access websites requiring authentification afterwards.
     *
     * **Warning**: This will fail if called too rapidly.
     * Every generated one-time-code can only be used once and this method only uses current and next (+30s) token.
     *
     * @return Int status code following the convention for login returns:
     *   - `-1`: unknown error
     *   - `-2`: invalid credentials
     *   - `-3`: no token named "tuplan" exists in TUDa 2FA-Management
     *   - `-4`: wrong token entered
     *   - Otherwise: HTTP status code (`200` = success, `404` = not found, etc.)
     */
    private suspend fun tuWebLogin(username: String, password: String, totpSecret: String): Int {
        // As a broad overview, the login flow is as follows:
        //    Load page & capture csrf token            -> submit username + password as form
        // -> select mfa option (totp named tuplan)     -> submit one-time token                -> success

        // Get csrfToken

        client.close()
        client = getIdentityClient()

        var response: HttpResponse = client.safeGet("https://login.tu-darmstadt.de/idp/profile/admin/hello") ?: return -1
        var document = Ksoup.parse(response.bodyAsText())

        if (response.status.value != 200 && response.status.value != 302) {
            return response.status.value
        }

        var csrfToken = document.select("input[name=csrf_token]").first()?.value() ?: return -1

        // Login with user credentials

        response = client.safeSubmitForm(
            "https://login.tu-darmstadt.de/idp/profile/admin/hello?execution=e1s1",
            formParameters = parameters {
                append("csrf_token", csrfToken)
                append("j_username", username)
                append("j_password", password)
                append("_eventId_proceed", "")
            }) ?: return -1

        response = client.safeGet("https://login.tu-darmstadt.de/idp/profile/admin/hello?execution=e1s2") ?: return -1
        document = Ksoup.parse(response.bodyAsText())

        if ("Password ist falsch" in response.bodyAsText() ||
            "password you entered was incorrect" in response.bodyAsText()) {
            return -2
        }

        if ("Token auswählen" !in response.bodyAsText() &&
            "select token" !in response.bodyAsText())
        {
            if ("Hello World" in response.bodyAsText()) {
                return 200
            } else if (response.status.value != 200 && response.status.value != 302) {
                return response.status.value
            } else {
                return -1
            }
        }

        // Select the option containing "tuplan", case-insensitive
        val totpTokenID = document.select("option")
            .find { it.text().contains("tuplan", ignoreCase = true) }
            ?.attr("value")
            ?: return -3

        csrfToken = document.select("input[name=csrf_token]").first()?.value() ?: return -1

        // Submit selected MFA option (the one containing "tuplan")

        client.safeSubmitForm(
            "https://login.tu-darmstadt.de/idp/profile/admin/hello?execution=e1s2",
            formParameters = parameters {
                append("csrf_token", csrfToken)
                append("fudis_selected_token_ids_input", totpTokenID)
                append("_eventId_proceed", "")
            })

        response = client.safeGet("https://login.tu-darmstadt.de/idp/profile/admin/hello?execution=e1s3") ?: return -1
        document = Ksoup.parse(response.bodyAsText())

        csrfToken = document.select("input[name=csrf_token]").first()?.value() ?: return -1

        val totpTokenPair = generateTotpPairFromBase32(totpSecret) ?: return -4

        // Submit totp token and complete login

        response = client.safeSubmitForm(
            "https://login.tu-darmstadt.de/idp/profile/admin/hello?execution=e1s3",
            formParameters = parameters {
                append("csrf_token", csrfToken)
                append("fudis_otp_input", totpTokenPair.first)
                append("_eventId_proceed", "")
            }) ?: return -1

        // The totp token might have already been used in this 30s window.
        // Try again with the token for the next 30s.
        // Can still fail if the user rapidly tries to login multiple times
        if (response.status.value == 302) {
            response = client.safeGet("https://login.tu-darmstadt.de/idp/profile/admin/hello?execution=e1s4") ?: return -1
            document = Ksoup.parse(response.bodyAsText())

            csrfToken = document.select("input[name=csrf_token]").first()?.value() ?: return -1

            response = client.safeSubmitForm(
                "https://login.tu-darmstadt.de/idp/profile/admin/hello?execution=e1s4",
                formParameters = parameters {
                    append("csrf_token", csrfToken)
                    append("fudis_otp_input", totpTokenPair.second)
                    append("_eventId_proceed", "")
                }) ?: return -1
        }

        if ("Hello World" in response.bodyAsText()) {
            return 200
        } else if (response.status.value == 302) {
            return -4
        } else if (response.status.value != 200) {
            return response.status.value
        } else {
            return -1
        }
    }

    private fun ByteArray.base64Url(): String =
        encodeBase64().replace('+', '-')
            .replace('/', '_')
            .replace("=", "")

    /**
     * Creates [TucanCredentials] if login level is mfa.
     * Logging in with a tucan auth code is significantly faster since it bypasses SSO
     * For this reason [createTucanCredentialsFromMFALogin] has the side effect of creating an auth code
     * so that succeeding attempts use [createTucanCredentialsFromAuthCode] instead
     *
     * Requires TUCaN service to be reachable.
     *
     * @return Int status code following the convention for login returns:
     *   - `-1`: unknown error (when request fails, e.g. no internet)
     *   - `-2`: invalid credentials
     *   - Otherwise: HTTP status code (`200` = success, `404` = not found, etc.)
     */
    private suspend fun createTucanCredentialsFromMFALogin() {
        val nonce = Random.nextBytes(32).base64Url()
        val state = Random.nextBytes(32).base64Url()

        val codeVerifier = Random.nextBytes(32).base64Url()
        val codeChallenge = sha256(codeVerifier.encodeToByteArray()).base64Url()

        client.safeGet("https://dsf.tucan.tu-darmstadt.de/IdentityServer/connect/authorize") {
            parameter("nonce", nonce)
            parameter("max_age", "0")
            parameter("response_type", "code")
            parameter("code_challenge_method", "S256")
            parameter("scope", "openid DSF profile offline_access")
            parameter("code_challenge", codeChallenge)
            parameter("redirect_uri", "de.datenlotsen.campusnet.tuda://oauth2redirect")
            parameter("client_id", "MobileApp")
            parameter("state", state)
        } ?: return

        var response = client.safeGet("https://dsf.tucan.tu-darmstadt.de/IdentityServer/External/Challenge") {
            parameter("provider", "dfnshib")
            parameter("returnUrl", "/IdentityServer/connect/authorize/callback?nonce=$nonce&max_age=0&response_type=code&code_challenge_method=S256&scope=openid%20DSF%20profile%20offline_access&code_challenge=$codeChallenge&redirect_uri=de.datenlotsen.campusnet.tuda%3A%2F%2Foauth2redirect&client_id=MobileApp&state=$state")
        } ?: return

        val document = Ksoup.parse(response.bodyAsText())

        val samlResponse = document.selectFirst("input[name=SAMLResponse]")
            ?.attr("value")
            ?: return

        val relayState = document.selectFirst("input[name=RelayState]")
            ?.attr("value")
            ?: return


        response = client.safeSubmitForm(
            url = "https://dsf.tucan.tu-darmstadt.de/IdentityServer/external/saml/acs/dfnshib",
            formParameters = parameters {
                append("RelayState", relayState)
                append("SAMLResponse", samlResponse)
            }
        ) ?: return

        var location = response.headers[HttpHeaders.Location]
            ?: return

        response = client.safeGet("https://dsf.tucan.tu-darmstadt.de$location")
            ?: return

        location = response.headers[HttpHeaders.Location]
            ?: return

        val url = Url(location)

        val code = url.parameters["code"] ?: return
        val returnedState = url.parameters["state"] ?: return

        if (returnedState != state) return       // Possible CSRF Attack

        exchangeCodeForToken(code, codeVerifier, nonce)
    }


    // Student ID function


    /**
     * Get [studentID] property from internet.
     *
     * Requires successful mfa-login before usage.
     */
    @OptIn(ExperimentalTime::class)
    private suspend fun getStudentID(): StudentID? {
        fun getAttributeValuesType1(document: Document, attributeName: String): List<String> {
            val values = mutableListOf<String>()

            // Find all <strong> tags (same as soup.find_all('strong'))
            val strongTags = document.select("strong")

            for (strongTag in strongTags) {
                if (strongTag.text().trim() == attributeName) {

                    // Find the next <small> after the matching <strong>
                    var smallTag = strongTag.parent()?.nextElementSibling()

                    // BeautifulSoup's find_next('small')
                    while (smallTag != null && smallTag.tagName() != "small") {
                        smallTag = smallTag.nextElementSibling()
                    }

                    if (smallTag != null && smallTag.tagName() == "small") {
                        values.add(smallTag.text().trim())
                    }

                    // Now walk the siblings exactly like in your Python code
                    var sibling = smallTag?.nextElementSibling()

                    while (sibling != null) {
                        when (sibling.tagName()) {
                            "br" -> {
                                sibling = sibling.nextElementSibling()
                                continue
                            }

                            "small" -> {
                                val innerStrong = sibling.selectFirst("strong")
                                if (innerStrong != null) {
                                    break
                                }
                                values.add(sibling.text().trim())
                            }
                        }

                        sibling = sibling.nextElementSibling()
                    }
                }
            }

            return values
        }

        fun getLibraryCardIDFromDocument(document: Document): String? {
            var libCardNum: String? = null

            val rows = document.select("div.row")

            for (row in rows) {
                val keyTag = row.selectFirst("div.col-xs-5 b")
                val key = keyTag?.text()?.trim()?.removeSuffix(":") ?: continue

                if (!("Bibliotheksausweisnummer" in key || "Library card number" in key)) {
                    continue
                }

                val valueTag = row.selectFirst("div.col-xs-7 span")
                val value = valueTag?.text()?.trim()

                if (value != null) {
                    libCardNum = value
                    break
                }
            }

            return libCardNum
        }

        fun getAttributeValuesType2(document: Document, possibleKeys: List<String>): String? {
            for (row in document.select("tr")) {
                val tds = row.select("td")

                if (tds.size == 2) {
                    val key = tds[0].text().trim()

                    if (possibleKeys.contains(key)) {
                        return tds[1].text().trim()
                    }
                }
            }

            return null
        }

        data class SemesterDeadlines(
            val reregister: String,
            val graceReregister: String,
            val degreeChange: String,
            val registration: String,
            val guestRegistration: String,
            val absenceNotify: String,
            val deregistration: String
        )
        data class Semester(
            val title: String,
            val startDate: String,
            val endDate: String,
            val lectureStartDate: String,
            val lectureEndDate: String,
            val winterBreakDateRange: String?,
            val courseCatalogueStartDate: String?,
            val semesterDeadlines: SemesterDeadlines?
        )

        suspend fun getSemesterDates(): List<Semester> {
            val response: HttpResponse = client.safeGet("https://www.tu-darmstadt.de/studieren/studierende_tu/semestermine/index.de.jsp") ?: return emptyList()
            val document = Ksoup.parse(response.bodyAsText())

            val semesters = mutableListOf<Semester>()

            // Detailed semesters (use a h2 title)
            for (heading in document.select("h2")) {
                val semesterName = heading.text().trim()

                if (!semesterName.contains("Winter") && !semesterName.contains("Sommer")) {
                    continue
                }

                // container = go up 4 levels
                val container = heading.parents().getOrNull(3) ?: continue

                val tables = container.select("table")

                val deadlineTableRows = tables[1].select("tr")
                val generalTableRows = tables[0].select("tr")

                val semesterDeadlines = SemesterDeadlines(
                    reregister = deadlineTableRows[1].select("td")[1].text(),
                    graceReregister = deadlineTableRows[2].select("td")[1].text(),
                    degreeChange = deadlineTableRows[3].select("td")[1].text(),
                    registration = deadlineTableRows[4].select("td")[1].text(),
                    guestRegistration = deadlineTableRows[5].select("td")[1].text(),
                    absenceNotify = deadlineTableRows[6].select("td")[1].text(),
                    deregistration = deadlineTableRows[7].select("td")[1].text()
                )

                var winterBreakDateRange: String? = null
                var courseCatalogueStartDate: String? = null

                if (semesterName.contains("Winter")) {
                    winterBreakDateRange = generalTableRows[4].select("td")[1].text()
                    courseCatalogueStartDate = generalTableRows[5].select("td")[1].text()
                } else {
                    courseCatalogueStartDate = generalTableRows[4].select("td")[1].text()
                }

                val semester = Semester(
                    title = semesterName,
                    startDate = generalTableRows[0].select("td")[1].text(),
                    endDate = generalTableRows[3].select("td")[1].text(),
                    lectureStartDate = generalTableRows[1].select("td")[1].text(),
                    lectureEndDate = generalTableRows[2].select("td")[1].text(),
                    winterBreakDateRange = winterBreakDateRange,
                    courseCatalogueStartDate = courseCatalogueStartDate,
                    semesterDeadlines = semesterDeadlines
                )

                semesters.add(semester)
            }


            // Less detailed semesters (use a h3 title)
            for (heading in document.select("h3")) {
                val semesterName = heading.text().trim()

                if (!(semesterName.contains("Sommer") || semesterName.contains("Winter"))) {
                    continue
                }

                val table = heading.parent()?.nextElementSibling()?.selectFirst("table") ?: continue
                val tableRows = table.select("tr")

                var winterBreakDateRange: String? = null

                if (semesterName.contains("Winter")) {
                    winterBreakDateRange = tableRows[4].select("td")[1].text()
                }

                val semester = Semester(
                    title = semesterName,
                    startDate = tableRows[0].select("td")[1].text(),
                    endDate = tableRows[3].select("td")[1].text(),
                    lectureStartDate = tableRows[1].select("td")[1].text(),
                    lectureEndDate = tableRows[2].select("td")[1].text(),
                    winterBreakDateRange = winterBreakDateRange,
                    courseCatalogueStartDate = null,
                    semesterDeadlines = null
                )

                semesters.add(semester)
            }

            return semesters
        }

        // latestRegSemester looks like so: "YYYYS", basically 20252 = WiSe 2025/2026 or 20321 = SoSe 2031
        // Account expiry looks like so: "Month dd, YYYY"
        // returns a date if the ID is valid, otherwise null
        fun checkValidity(latestRegSemester: String, accountExpiry: String, semesters: List<Semester>, affiliation: List<String>): String? {
            if (!affiliation.contains("student")) {
                return null
            }

            val year = latestRegSemester.subSequence(0, 4).toString()
            val semester = latestRegSemester.subSequence(4, 5).toString()

            val matchSemester = if (semester == "1") "Sommer" else "Winter"

            val latestRegSemesterInfo = semesters.find { semesterObj ->
                semesterObj.title.contains(year) && semesterObj.title.contains(matchSemester)
            } ?: return null

            // latestRegSemesterInfo is in dd.mm.yyyy format
            val parts = latestRegSemesterInfo.endDate.split(".")
            val dateDay = parts[0].toInt()
            val dateMonth = parts[1].toInt()
            val dateYear = parts[2].toInt()

            val semesterEndDate = LocalDate(dateYear, dateMonth, dateDay)

            // accountExpiry is in "Month dd, yyyy" format if english
            //                     "dd. Month yyyy" format if german
            val monthMap = mapOf(
                "January" to 1, "February" to 2, "March" to 3, "April" to 4, "May" to 5, "June" to 6,
                "July" to 7, "August" to 8, "September" to 9, "October" to 10, "November" to 11, "December" to 12,
                "Januar" to 1, "Februar" to 2, "März" to 3, "Mai" to 5, "Juni" to 6,
                "Juli" to 7, "Oktober" to 10, "Dezember" to 12,
            )

            val accountParts = accountExpiry.split(" ")

            val accountYear = accountParts[2].toInt()
            var accountMonth: Int
            var accountDay: Int

            if (monthMap[accountParts[0]] != null) {        // EN format ("Month dd, yyyy")
                accountMonth = monthMap[accountParts[0]] ?: throw Error("never thrown")
                accountDay = accountParts[1].removeSuffix(",").toInt()
            } else if (monthMap[accountParts[1]] != null) { // DE format ("dd. Month yyyy")
                accountMonth = monthMap[accountParts[1]] ?: throw Error("never thrown")
                accountDay = accountParts[0].removeSuffix(".").toInt()
            } else {
                throw IllegalArgumentException("Invalid month name")
            }

            val accountExpiryDate = LocalDate(accountYear, accountMonth, accountDay)

            val earlierDate = if (semesterEndDate < accountExpiryDate) semesterEndDate else accountExpiryDate
            val todayDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

            if (earlierDate > todayDate) {
                return earlierDate.toString()
            } else {
                return null
            }
        }


        // Execution starts here

        var response: HttpResponse = client.safeGet("https://login.tu-darmstadt.de/idp/profile/admin/hello") ?: return null
        var document = Ksoup.parse(response.bodyAsText())

        val lastName = getAttributeValuesType1(document, "surname")[0]
        val firstName = getAttributeValuesType1(document, "givenName")[0]
        val matriNumber = getAttributeValuesType1(document, "tudMatrikel")[0]
        val birthdate = getAttributeValuesType1(document, "schacDateOfBirth")[0]     // Format 20060722
        val affiliation = getAttributeValuesType1(document, "eduPersonAffiliation")

        val euroStudentID = getAttributeValuesType1(document, "schacPersonalUniqueCode")[0]


        // Get LibraryCardID

        response = client.safeGet("https://idm.tu-darmstadt.de/password/accountNames?execution=e1s1") ?: return null
        document = Ksoup.parse(response.bodyAsText())

        val libraryCardID: String? = getLibraryCardIDFromDocument(document)


        // Get account matriculation info

        client.safeGet("https://idm.tu-darmstadt.de/accountDashboard/selfReport?execution=e1s1")        // The idm can't load eventId without base URL
        response = client.safeGet("https://idm.tu-darmstadt.de/accountDashboard/selfReport?execution=e1s1&_eventId=account") ?: return null
        document = Ksoup.parse(response.bodyAsText())

        val latestRegisteredSemester = getAttributeValuesType2(document, listOf("Semester of last re-registration:", "Semester der letzten Rückmeldung:"))
            ?: return null
        val accountExpiry = getAttributeValuesType2(document, listOf("Expiry Date:", "Ablaufdatum:"))
            ?: return null

        val semesters = getSemesterDates()

        val lastValidDate = checkValidity(latestRegisteredSemester, accountExpiry, semesters, affiliation)
            ?: return null


        // Create and return StudentID

        var semesterYear = latestRegisteredSemester.substring(0, 4)
        val semesterType = if (latestRegisteredSemester.substring(4, 5) == "1") "SoSe" else "WiSe"

        if (semesterType == "WiSe") {
            val newYear = (semesterYear.toInt() + 1).toString()

            semesterYear = "$semesterYear/$newYear"
        }

        // birthdate is formatted as YYYYMMDD
        val formattedBirthdate = "${birthdate.substring(0, 4)}-${birthdate.substring(4, 6)}-${birthdate.substring(6, 8)}"

        val studentCard = StudentCard(
            semester = "$semesterType $semesterYear",
            lastName = lastName,
            firstName = firstName,
            lastValidDate = lastValidDate,
            matriNumber = matriNumber,
            birthdate = formattedBirthdate
        )

        val studentID = StudentID(
            studentCard = studentCard,
            euroStudentID = euroStudentID,
            libraryCardID = libraryCardID
        )

        //_studentID.value = studentID
        return studentID
    }

    /**
     * Updates [studentID] property from internet.
     *
     * Requires successful mfa-login before usage.
     */
    suspend fun updateStudentID() {
        _studentIDStatus.value = RequestStatus.LOADING
        _studentID.value = getStudentID()
        _studentIDStatus.value = RequestStatus.READY
    }
}

