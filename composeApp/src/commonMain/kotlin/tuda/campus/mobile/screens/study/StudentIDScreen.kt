package tuda.campus.mobile.screens.study

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tuda.campus.mobile.PlatformImage
import tuda.campus.mobile.components.LoginLoadingScreen
import tuda.campus.mobile.components.LoginLockedScreen
import tuda.campus.mobile.components.MediumNavTitle
import tuda.campus.mobile.components.RequestLoadingScreen
import tuda.campus.mobile.managers.IdentityManager
import tuda.campus.mobile.managers.tucanManager.RequestStatus
import tuda.campus.mobile.managers.TULoginState
import tuda.campus.mobile.platformImageFromInterleaved25
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import qrcode.QRCode
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.cardNumberIs
import tudex.composeapp.generated.resources.errorLoadingBarcode
import tudex.composeapp.generated.resources.esi_logo
import tudex.composeapp.generated.resources.europeanStudentIdentifier
import tudex.composeapp.generated.resources.identityCards
import tudex.composeapp.generated.resources.invalidI25Input
import tudex.composeapp.generated.resources.libraryCard
import tudex.composeapp.generated.resources.ulb_logo

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StudentIDScreen(
    backAction: () -> Unit
) {
    val tuLoginStatus by IdentityManager.tuLoginStatus.collectAsStateWithLifecycle()
    val studentIDStatus by IdentityManager.studentIDStatus.collectAsStateWithLifecycle()
    val studentID by IdentityManager.studentID.collectAsStateWithLifecycle()

    val pages: List<String> = rememberSaveable {
        val list = mutableListOf<String>()
        //if (studentID?.studentCard != null) list.add("StudentCard")
        if (studentID?.libraryCardID != null) list.add("LibraryCard")
        if (studentID?.euroStudentID != null) list.add("EuroStudentID")
        return@rememberSaveable list
    }

    val pagerState = rememberPagerState(pageCount = { pages.count() })

    MediumNavTitle(
        titleComp = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.identityCards))
                if (studentID != null) {
                    if (tuLoginStatus == TULoginState.MFA_UNCONFIRMED ||
                        studentIDStatus == RequestStatus.LOADING
                    ) {
                        LoadingIndicator(modifier = Modifier.size(32.dp))
                    }
                }
            }
        },
        backAction = backAction
    ) {
        if (studentID == null) {
            if (tuLoginStatus == TULoginState.MFA_UNCONFIRMED) {
                LoginLoadingScreen()
            } else if (tuLoginStatus != TULoginState.MFA && tuLoginStatus != TULoginState.MFA_TUCAN_ERROR) {
                LoginLockedScreen(modifier = Modifier.padding(horizontal = 16.dp))
            } else if (studentIDStatus == RequestStatus.LOADING) {
                RequestLoadingScreen(stringResource(Res.string.identityCards))
            }
        } else {
            HorizontalPager(state = pagerState) { page ->
                val pageName = pages[page]

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    when (pageName) {
                        /*
                        "StudentCard" -> {
                            StudentCard(studentID!!.studentCard!!)
                        }
                         */

                        "LibraryCard" -> {
                            LibraryCard(studentID!!.libraryCardID!!)
                        }

                        "EuroStudentID" -> {
                            EuroStudentIDCard(studentID!!.euroStudentID!!)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/*
@Composable
private fun StudentCard(
    studentCard: StudentCard
) {
    val platformImage by IdentityManager.userImage.collectAsStateWithLifecycle()

    Box(modifier = Modifier.aspectRatio(16f / 10f)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceBright)
                .padding(14.dp)
        ) {
            Text(stringResource(Res.string.studentIDForSemester, studentCard.semester), modifier = Modifier.padding(start = 2.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(vertical = 10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .aspectRatio(3f / 4f)
                ) {
                    platformImage?.bytes?.let { bytes ->
                        Image(
                            bitmap = bytes.decodeToImageBitmap(),
                            contentDescription = null
                        )
                    } ?: run {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Text(
                                text = stringResource(Res.string.noImageFound),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy((-4).dp)) {
                        Text(stringResource(Res.string.firstName), style = MaterialTheme.typography.labelSmall)
                        Text(studentCard.firstName, style = MaterialTheme.typography.bodyLarge)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy((-4).dp)) {
                        Text(stringResource(Res.string.lastName), style = MaterialTheme.typography.labelSmall)
                        Text(studentCard.lastName, style = MaterialTheme.typography.bodyLarge)
                    }

                    Row {
                        Column(
                            verticalArrangement = Arrangement.spacedBy((-4).dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(Res.string.birthdate), style = MaterialTheme.typography.labelSmall)
                            Text(studentCard.birthdate, style = MaterialTheme.typography.bodyLarge)
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy((-4).dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(Res.string.matrNumber), style = MaterialTheme.typography.labelSmall)
                            Text(
                                studentCard.matriNumber,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy((-4).dp)) {
                        Text(stringResource(Res.string.validUntil), style = MaterialTheme.typography.labelSmall)
                        Text(studentCard.lastValidDate, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(Res.string.validCaps),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 8.dp)

            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painterResource(Res.drawable.tuda_logo),
                contentDescription = null,
                modifier = Modifier
                    .background(
                        color =
                            if (isSystemInDarkTheme())
                                MaterialTheme.colorScheme.inverseSurface
                            else
                                MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(bottomStart = 20.dp, topEnd = 20.dp)
                    )
                    .padding(top = if (isSystemInDarkTheme()) 12.dp else 16.dp)
                    .padding(end = if (isSystemInDarkTheme()) 12.dp else 16.dp)
                    .padding(start = 10.dp, bottom = 10.dp)
                    .height(32.dp)
            )
        }
    }
}
 */

@Composable
private fun LibraryCard(
    libraryID: String
) {
    Box(modifier = Modifier.aspectRatio(16f / 10f)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceBright)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = stringResource(Res.string.libraryCard),
                    modifier = Modifier.padding(start = 2.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .background(color =
                            if (isSystemInDarkTheme())
                                MaterialTheme.colorScheme.inverseSurface
                            else
                                MaterialTheme.colorScheme.surfaceBright,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .weight(1f)
                        .padding(vertical = 16.dp)
                        .padding(horizontal = if (isSystemInDarkTheme()) 16.dp else 0.dp)
                ) {
                    if (libraryID.isNotEmpty()) {
                        platformImageFromInterleaved25(
                            data = libraryID,
                            height = 60
                        )?.bytes?.decodeToImageBitmap()?.let { imageBitmap ->
                            Row {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillHeight,
                                    filterQuality = FilterQuality.None,
                                    modifier = Modifier.fillMaxHeight()
                                )
                            }
                        } ?: run {
                            Text(stringResource(Res.string.errorLoadingBarcode))
                        }
                    } else {
                        Text(stringResource(Res.string.invalidI25Input))
                    }
                }

                Text(
                    text = stringResource(Res.string.cardNumberIs, libraryID),
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painterResource(Res.drawable.ulb_logo),
                contentDescription = null,
                modifier = Modifier
                    .background(
                        color =
                            if (isSystemInDarkTheme())
                                MaterialTheme.colorScheme.inverseSurface
                            else
                                MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(bottomStart = 20.dp, topEnd = 20.dp)
                    )
                    .padding(top = if (isSystemInDarkTheme()) 12.dp else 16.dp)
                    .padding(end = if (isSystemInDarkTheme()) 12.dp else 16.dp)
                    .padding(start = 10.dp, bottom = 10.dp)
                    .height(32.dp)
            )
        }
    }
}

@Composable
private fun EuroStudentIDCard(
    euroStudentID: String
) {
    Box(modifier = Modifier.aspectRatio(16f / 10f)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceBright)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = stringResource(Res.string.europeanStudentIdentifier),
                    modifier = Modifier.padding(start = 2.dp)
                )


                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .weight(1f)
                        .background(color =
                            if (isSystemInDarkTheme())
                                MaterialTheme.colorScheme.inverseSurface
                            else
                                MaterialTheme.colorScheme.surfaceBright
                        )
                        .padding(if (isSystemInDarkTheme()) 4.dp else 0.dp)
                ) {
                    val qrCode = QRCode.ofSquares()
                        .withInnerSpacing(0)
                        .build(euroStudentID)
                        .render()
                        .getBytes()

                    val platformImage = PlatformImage(qrCode)
                    val bitmap = platformImage.bytes.decodeToImageBitmap()
                    Row {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.FillHeight,
                            filterQuality = FilterQuality.None,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }

                Text(
                    text = euroStudentID,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painterResource(Res.drawable.esi_logo),
                contentDescription = null,
                modifier = Modifier
                    .background(
                        color =
                            if (isSystemInDarkTheme())
                                MaterialTheme.colorScheme.inverseSurface
                            else
                                MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(bottomStart = 20.dp, topEnd = 20.dp)
                    )
                    .padding(top = if (isSystemInDarkTheme()) 16.dp else 20.dp)
                    .padding(end = if (isSystemInDarkTheme()) 12.dp else 16.dp)
                    .padding(start = 10.dp, bottom = 16.dp)
                    .height(16.dp)
            )
        }
    }
}

/*
@Preview
@Composable
private fun PreviewStudentCard() {
    StudentCard(
        studentCard = StudentCard(
            semester = "WiSe 2025/2026",
            lastName = "Verylonglastname",
            firstName = "extraordinarilylongfirstname",
            lastValidDate = "2024-03-31",
            matriNumber = "1234567",
            birthdate = "01.01.1970",
        )
    )
}
 */

@Preview
@Composable
private fun PreviewLibraryCard() {
    LibraryCard("012345678910")
}

@Preview
@Composable
private fun PreviewEuroStudentIDCard() {
    EuroStudentIDCard("urn:schac:personalUniqueCode:int:esi:tu-darmstadt.de:1234567")
}