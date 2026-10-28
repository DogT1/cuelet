package com.tuvin.pixelautomations.editor

import com.tuvin.pixelautomations.setup.t

import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuvin.pixelautomations.model.Place
import com.tuvin.pixelautomations.triggers.Locations
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** Pick a place: the phone's current location or an address search, then a name and a radius. */
@Composable
fun PlacePicker(title: String, place: Place?, onChange: (Place) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t(title), style = MaterialTheme.typography.labelLarge)
        if (place != null) {
            OutlinedTextField(
                value = place.name,
                onValueChange = { onChange(place.copy(name = it)) },
                label = { Text(t("Place name")) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(t("%.5f, %.5f").format(place.lat, place.lng), style = MaterialTheme.typography.bodySmall)
            Text("Radius: ${place.radius} m", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = place.radius.toFloat(),
                onValueChange = { onChange(place.copy(radius = (it / 50).roundToInt() * 50)) },
                valueRange = 100f..2000f,
            )
        }
        OutlinedButton(onClick = {
            status = t("Finding your location…")
            scope.launch {
                val here = Locations.current(ctx)
                if (here == null) {
                    status = t("Couldn't get a location. Check that Location is on and allowed.")
                } else {
                    val name = reverseGeocode(ctx, here.latitude, here.longitude) ?: t("Current location")
                    onChange(Place(name, here.latitude, here.longitude, place?.radius ?: 150))
                    status = null
                }
            }
        }) { Text(t("Use current location")) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text(t("Search address")) }, modifier = Modifier.weight(1f))
            TextButton(enabled = query.isNotBlank(), onClick = {
                status = t("Searching…")
                scope.launch {
                    val hit = geocode(ctx, query)
                    if (hit == null) {
                        status = t("No match for “%s”.", query)
                    } else {
                        onChange(Place(shortName(hit) ?: query, hit.latitude, hit.longitude, place?.radius ?: 150))
                        status = null
                        query = ""
                    }
                }
            }) { Text(t("Find")) }
        }
        status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

private fun shortName(a: Address): String? =
    listOfNotNull(a.thoroughfare?.let { street -> a.subThoroughfare?.let { "$street $it" } ?: street }, a.locality)
        .joinToString(", ").ifBlank { a.getAddressLine(0) }

private suspend fun geocode(ctx: Context, query: String): Address? = suspendCancellableCoroutine { cont ->
    if (!Geocoder.isPresent()) return@suspendCancellableCoroutine cont.resume(null)
    Geocoder(ctx, Locale.getDefault()).getFromLocationName(query, 1, object : Geocoder.GeocodeListener {
        override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses.firstOrNull())
        override fun onError(errorMessage: String?) = cont.resume(null)
    })
}

private suspend fun reverseGeocode(ctx: Context, lat: Double, lng: Double): String? = suspendCancellableCoroutine { cont ->
    if (!Geocoder.isPresent()) return@suspendCancellableCoroutine cont.resume(null)
    Geocoder(ctx, Locale.getDefault()).getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
        override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses.firstOrNull()?.let(::shortName))
        override fun onError(errorMessage: String?) = cont.resume(null)
    })
}
