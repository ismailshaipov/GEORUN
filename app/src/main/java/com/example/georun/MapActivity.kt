package com.example.georun

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import com.example.georun.database.Coordinates
import com.example.georun.ui.theme.GEORUNTheme
import com.example.georun.viewmodels.MainViewModel
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView

class MapActivity : ComponentActivity() {
    private lateinit var mvm: MainViewModel
    private var sessionId: Long = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mvm = ViewModelProvider(this)[MainViewModel::class.java]
        sessionId = intent.getLongExtra("sessionID", -1)

        setContent {
            GEORUNTheme {
                MapUI (Modifier.fillMaxSize(), onBackPressed = { finish()}){
                    val coordinates = mvm.getCoordinatesForSession(sessionId)
                    Log.d("Kolvo",coordinates.size.toString())
                    YaMap(Modifier.fillMaxSize(), coordinates)
                }

            }

        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapUI(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.map)) },
                navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Column(Modifier.padding(it)) {
            content()
        }
    }
}

@Composable
fun YaMap(
    modifier: Modifier = Modifier,
    coordinates: List<Coordinates>
){
    val points = coordinates.map { Point(it.latitude, it.longitude) }
    val polyline = Polyline(points)


    Box(modifier = modifier) {
        AndroidView(factory = {
            MapView(it).apply {
                if(coordinates.size >= 2){
                    mapWindow.map.move(
                        CameraPosition(
                            Point(coordinates.firstOrNull()?.latitude ?:  41.303921, coordinates.firstOrNull()?.longitude ?:  -81.901693),
                            17.0f,
                            0.0f,
                            30.0f
                        )
                    )
                    mapWindow.map.mapObjects.addPolyline(polyline)
                }else{
                    /*ometry = Point(59.935493, 30.327392)
                        //setIcon(ImageProvider.fromResource(context,R.drawable.twotone_not_listed_location_48))
                        setText("Special place",
                            TextStyle().apply {
                            size = 10f
                            placement = TextStyle.Placement.RIGHT
                            offset = 5f
                            }
                        )
                    }*/
                    mapWindow.map.move(
                        CameraPosition(
                            Point(coordinates.firstOrNull()?.latitude ?: 41.303921, coordinates.firstOrNull()?.longitude ?: -81.901693),
                            17.0f,
                            0.0f,
                            30.0f
                        )
                    )
                }
            }
        })
    }
}

