package com.example.georun

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.georun.database.DBHelper
import com.example.georun.database.TrackSession
import com.example.georun.ui.theme.GEORUNTheme
import com.example.georun.viewmodels.MainViewModel
import java.time.LocalDateTime


class MainActivity : ComponentActivity() {
    lateinit var dbBDHelper: DBHelper

    private val mvm: MainViewModel by lazy{
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        mvm.showRequestDialog = false
        when {
            permissions.getOrDefault(ACCESS_FINE_LOCATION, false) -> {

            }
            permissions.getOrDefault(ACCESS_COARSE_LOCATION, false) -> {
            }
            else -> {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GEORUNTheme {
                MainUI(
                    mvm,
                    Modifier.fillMaxSize()
                )
                mvm.showRequestDialog =
                    !mvm.isPermissionsGranted(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION, context = this)
                if (mvm.showRequestDialog){
                    LocationRequestDialog(
                        onDeny = {
                            finish()
                        }
                    ){
                        // Формирование запроса из системы на доступ к геолокации
                        mvm.showRequestDialog = false
                        locationPermissionRequest.launch(
                            arrayOf(
                                ACCESS_FINE_LOCATION,
                                ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            }
            mvm.loadTrackSessionsWithCoordinates()
        }
    }

    override fun onResume() {
        super.onResume()
        if (mvm.requestLocationUpdates) mvm.startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        mvm.stopLocationUpdates()
    }
}

@Composable
fun MainUI(
    mvm: MainViewModel,
    modifier: Modifier = Modifier,
){
    val loc by mvm.location.collectAsState()
    val locStr = loc?.let{ "Lat: ${it.latitude} Lon: ${it.longitude}" } ?: "Unknown location"

    val session: List<TrackSession> = mvm.trackSessions
    //val session by remember { mvm.trackSessions}

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = locStr,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall
        )

        Button(
            onClick = {
                if (mvm.requestLocationUpdates) {
                    mvm.startLocationUpdates()

                } else {
                    mvm.stopLocationUpdates()
                }
                mvm.requestLocationUpdates = !mvm.requestLocationUpdates
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(if (mvm.requestLocationUpdates) "Start Tracking" else "Stop Tracking")
        }

        /*LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(session){session ->
                SessionItem(session)
            }
        }*/
    }
}


@Composable
fun SessionItem(session: TrackSession) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Session ID: ${session.sessionId}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            /*Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start Time: ${session.startTime}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "End Time: ${session.endTime}",
                style = MaterialTheme.typography.bodyMedium
            )*/
        }
    }
}

/*@Composable
fun formatDateTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.getDefault())
    return formatter.format(dateTime)
}*/

@Preview
@Composable
fun SessionItemPreview() {
    GEORUNTheme {
        SessionItem(
            TrackSession(
                sessionId = 1,
                startTime = LocalDateTime.now(),
                endTime = LocalDateTime.now().plusHours(1)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationRequestDialog(
    modifier: Modifier = Modifier,
    onDeny: ()->Unit,
    onAllow: ()->Unit,
){
    AlertDialog(
        onDismissRequest = { onDeny() },
    ) {
        ElevatedCard(
            modifier = modifier.shadow(3.dp, shape = RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painterResource(id = R.drawable.twotone_not_listed_location_48),
                    contentDescription = null,
                    tint = colorResource(id = R.color.brown)
                )
                Text(stringResource(R.string.loc_permission_request))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { onDeny() }) {
                        Text("No")
                    }
                    Button(onClick = { onAllow() }) {
                        Text("Yes")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun LocationRequestDialogPreview(){
    LocationRequestDialog(onDeny = { /*TODO*/ }) {

    }
}