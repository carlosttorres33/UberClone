package com.carlostorres.uberclone.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.carlostorres.uberclone.R
import com.carlostorres.uberclone.databinding.ActivityMapBinding
import com.carlostorres.uberclone.models.DriverLocation
import com.carlostorres.uberclone.providers.AuthProvider
import com.carlostorres.uberclone.providers.BookingProvider
import com.carlostorres.uberclone.providers.GeoProvider
import com.carlostorres.uberclone.utils.CarMoveAnim
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.SphericalUtil
import org.imperiumlabs.geofirestore.callbacks.GeoQueryEventListener
import java.lang.Exception

class MapActivity : AppCompatActivity(), OnMapReadyCallback, Listener {

    private lateinit var binding: ActivityMapBinding
    private var googleMap: GoogleMap? = null
    private var easyWayLocation: EasyWayLocation? = null
    private var myLocationLatLng: LatLng? = null


    private var markerDriver: Marker? = null

    private val geoProvider = GeoProvider()
    private val authProvider = AuthProvider()
    private val bookingProvider = BookingProvider()

    //GOOGLE MPLACES
    private var places : PlacesClient? = null
    private var autocompleteOrigin : AutocompleteSupportFragment? = null
    private var autocompleteDestination : AutocompleteSupportFragment? = null
    private var originName: String = ""
    private var destinationName: String = ""
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null
    private var isLocationEnabled = false

    private val driverMarkers = ArrayList<Marker>()
    private val driversLocation = ArrayList<DriverLocation>()


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

        binding.btnRequestTrip.setOnClickListener {
            goToTripInfo()
        }

        removeBooking()

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

    private fun getPositionDriver( id: String) : Int {

        var position = 0

        for (i in driversLocation.indices){
            if (id == driversLocation[i].id){

                position = i

                break
            }
        }

        return position

    }

    private fun removeBooking(){

        bookingProvider.remove()

    }

    private fun getNearbyDrivers(){

        if (myLocationLatLng == null){
            return
        }
        geoProvider.getNearbyDrivers(myLocationLatLng!!, 15.0).addGeoQueryEventListener(object : GeoQueryEventListener{

            override fun onKeyEntered(documentID: String, location: GeoPoint) {
                for (marker in driverMarkers){

                    if (marker.tag != null){

                        if (marker.tag == documentID){

                            return

                        }

                    }

                }

                val driverLatLng = LatLng(location.latitude, location.longitude)

                val marker = googleMap?.addMarker(
                    MarkerOptions().position(driverLatLng).title("Conductor Disponible").icon(
                        BitmapDescriptorFactory.fromResource(R.drawable.uber_car)
                    )
                )

                marker?.tag = documentID

                driverMarkers.add(marker!!)

                val dLocation = DriverLocation()
                dLocation.id = documentID

                driversLocation.add(dLocation)

            }

            override fun onKeyExited(documentID: String) {

                for (marker in driverMarkers){

                    if (marker.tag != null){

                        if (marker.tag == documentID){

                            marker.remove()

                            driverMarkers.remove(marker)

                            driversLocation.removeAt(getPositionDriver(documentID))

                            return

                        }

                    }

                }

            }

            override fun onKeyMoved(documentID: String, location: GeoPoint) {

                for (marker in driverMarkers){

                    val start = LatLng(location.latitude, location.longitude)
                    var end : LatLng? = null
                    val position = getPositionDriver(marker.tag.toString())

                    if (marker.tag != null){

                        if (marker.tag == documentID){

                            if (driversLocation[position].latlng != null ){

                                end = driversLocation[position].latlng

                            }

                            driversLocation[position].latlng = LatLng(location.latitude, location.longitude)

                            if (end != null){

                                CarMoveAnim.carAnim(marker, end, start)

                            }

                            //marker.position = LatLng(location.latitude, location.longitude)

                        }

                    }

                }

            }

            override fun onGeoQueryError(exception: Exception) {

            }

            override fun onGeoQueryReady() {

            }

        })

    }

    private fun connectDriver(){

        easyWayLocation?.endUpdates()
        easyWayLocation?.startLocation()


    }

    private fun goToTripInfo(){

        if (originLatLng != null && destinationLatLng != null){

            val intent = Intent(this, TripInfoActivity::class.java)

            intent.putExtra("origin", originName)
            intent.putExtra("destination", destinationName)
            intent.putExtra("origin_lat", originLatLng?.latitude)
            intent.putExtra("origin_lng", originLatLng?.longitude)
            intent.putExtra("destination_lat", destinationLatLng?.latitude)
            intent.putExtra("destination_lng", destinationLatLng?.longitude)

            startActivity(intent)

        }else{

            Toast.makeText(this, "Debes seleccionar el origen y el destino", Toast.LENGTH_LONG).show()

        }

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

            getNearbyDrivers()

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

        autocompleteDestination?.setHint("Destino")
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