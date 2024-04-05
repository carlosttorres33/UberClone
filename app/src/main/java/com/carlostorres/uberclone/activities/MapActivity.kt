package com.carlostorres.uberclone.activities

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.carlostorres.uberclone.R
import com.carlostorres.uberclone.databinding.ActivityMapBinding
import com.carlostorres.uberclone.providers.AuthProvider
import com.carlostorres.uberclone.providers.GeoProvider
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.maps.android.SphericalUtil
import java.lang.Exception

class MapActivity : AppCompatActivity(), OnMapReadyCallback, Listener {

    private lateinit var binding: ActivityMapBinding
    private var googleMap: GoogleMap? = null
    private var myLocationLatLng: LatLng? = null

    private var easyWayLocation: EasyWayLocation? = null

    private var markerDriver: Marker? = null

    private val geoProvider = GeoProvider()
    private val authProvider = AuthProvider()

    //GOOGLE MPLACES
    private var places : PlacesClient? = null
    private var autocompleteOrigin : AutocompleteSupportFragment? = null
    private var autocompleteDestination : AutocompleteSupportFragment? = null
    private var originName: String = ""
    private var destinationName: String = ""
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null
    private var isLocationEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
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

        locationPermissions.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION

            )
        )

        startGooglePlaces()

    }

    val locationPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                when {
                    permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                        Log.d("Localization", "Permiso Concedido")
                        easyWayLocation?.startLocation()
                    }

                    permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                        easyWayLocation?.startLocation()
                        Log.d("Localization", "Permiso Concedido con limitacion")
                    }

                    else -> {
                        Log.d("Localization", "Permiso Denegado")
                    }
                }

            }

        }



    private fun connectDriver(){

        easyWayLocation?.endUpdates()
        easyWayLocation?.startLocation()


    }

    override fun onMapReady(map: GoogleMap) {

        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        onCameraMove()
//        easyWayLocation?.startLocation()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        googleMap?.isMyLocationEnabled = true

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

    private fun onCameraMove(){

        googleMap?.setOnCameraIdleListener {

            try {

                val geocoder = Geocoder(this)
                originLatLng = googleMap?.cameraPosition?.target

                if (originLatLng != null){

                    val addressList = geocoder.getFromLocation(originLatLng?.latitude!!, originLatLng?.longitude!!, 1)

                    if (addressList!!.size > 0){

                        val city = addressList!![0].locality
                        val country = addressList[0].countryName
                        val address = addressList[0].getAddressLine(0)

                        originName = "$address $city"

                        autocompleteOrigin?.setText(originName)

                    }

                }

            } catch (e: Exception) {

                Log.d("Error", "${e.message}")

            }

        }

    }

    override fun locationOn() {

    }

    //Se Obtiene latitud y longitud de la posicion en tiempo real
    override fun currentLocation(location: Location) {
        myLocationLatLng = LatLng(location.latitude, location.longitude)

        if (!isLocationEnabled){

            isLocationEnabled = true

            googleMap?.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(myLocationLatLng!!).zoom(15f).build()
                )
            )

            limitSearch()

        }

    }

    override fun locationCancelled() {

    }

    private fun startGooglePlaces(){

        if (!Places.isInitialized()){

            Places.initialize(applicationContext, resources.getString(R.string.google_maps_key))

        }

        places = Places.createClient(this)
        instanceAutocompleteOrigin()
        instanceAutocompleteDestination()

    }

    private fun limitSearch(){

        val northSide = SphericalUtil.computeOffset(myLocationLatLng, 10000.0, 0.0)
        val southSide = SphericalUtil.computeOffset(myLocationLatLng, 10000.0, 180.0)

        autocompleteOrigin?.setLocationBias(RectangularBounds.newInstance(southSide, northSide))
        autocompleteDestination?.setLocationBias(RectangularBounds.newInstance(southSide, northSide))

    }

    private fun instanceAutocompleteOrigin(){

        autocompleteOrigin = supportFragmentManager.findFragmentById(R.id.placesAutocompleteOrigin) as AutocompleteSupportFragment

        autocompleteOrigin?.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
            )
        )

        autocompleteOrigin?.setHint("Lugar de recogida")
        autocompleteOrigin?.setCountries("MX")
        autocompleteOrigin?.setOnPlaceSelectedListener(object:PlaceSelectionListener{

            override fun onPlaceSelected(place: Place) {

                originName = place.name!!
                originLatLng = place.latLng
                Log.d("Places", "Address: $originName")
                Log.d("Places", "Lat: ${originLatLng?.latitude}")
                Log.d("Places", "Lng: ${originLatLng?.longitude}")

            }

            override fun onError(p0: Status) {

            }

        })

    }

    private fun instanceAutocompleteDestination(){

        autocompleteDestination = supportFragmentManager.findFragmentById(R.id.placesAutocompleteDestination) as AutocompleteSupportFragment

        autocompleteDestination?.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
            )
        )

        autocompleteDestination?.setHint("Lugar de recogida")
        autocompleteDestination?.setCountries("MX")
        autocompleteDestination?.setOnPlaceSelectedListener(object:PlaceSelectionListener{

            override fun onPlaceSelected(place: Place) {

                destinationName = place.name!!
                destinationLatLng = place.latLng
                Log.d("Places", "Address: $destinationName")
                Log.d("Places", "Lat: ${destinationLatLng?.latitude}")
                Log.d("Places", "Lng: ${destinationLatLng?.longitude}")

            }

            override fun onError(p0: Status) {

            }

        })

    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()
        easyWayLocation?.endUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        easyWayLocation?.endUpdates()
    }

}