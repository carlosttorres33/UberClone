package com.carlostorres.uberclone.activities

import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.carlostorres.uberclone.R
import com.carlostorres.uberclone.databinding.ActivityTripInfoBinding
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MapStyleOptions

class TripInfoActivity : AppCompatActivity(), OnMapReadyCallback, Listener {

    private lateinit var binding : ActivityTripInfoBinding

    private var googleMap: GoogleMap? = null
    private var easyWayLocation: EasyWayLocation? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityTripInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }

        easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

    }

    override fun onMapReady(map: GoogleMap) {

        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true

        try {

            val succes = googleMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.style)
            )

            if (!succes!!){
                Log.d("MAPAS", "Erro al encontrar el estilo")
            }

        }catch (e: Resources.NotFoundException){
            Log.d("MAPAS", "Erro $e")
        }

    }

    override fun locationOn() {

    }

    override fun currentLocation(location: Location?) {

    }

    override fun locationCancelled() {

    }

    override fun onDestroy() {
        super.onDestroy()
        easyWayLocation?.endUpdates()
    }

}