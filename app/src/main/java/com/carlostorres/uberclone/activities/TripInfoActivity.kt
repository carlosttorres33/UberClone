package com.carlostorres.uberclone.activities

import android.content.Intent
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.carlostorres.uberclone.R
import com.carlostorres.uberclone.databinding.ActivityTripInfoBinding
import com.carlostorres.uberclone.models.Price
import com.carlostorres.uberclone.providers.ConfigProvider
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.example.easywaylocation.draw_path.DirectionUtil
import com.example.easywaylocation.draw_path.PolyLineDataBean
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
import kotlin.math.min

class TripInfoActivity : AppCompatActivity(), OnMapReadyCallback, Listener, DirectionUtil.DirectionCallBack {

    private lateinit var binding : ActivityTripInfoBinding

    private var googleMap: GoogleMap? = null
    private var easyWayLocation: EasyWayLocation? = null

    private var extraOriginName = ""
    private var extraDestinationName = ""
    private var extraOriginLat = 0.0
    private var extraOriginLng = 0.0
    private var extraDestinationLat = 0.0
    private var extraDestinationLng = 0.0

    private var originLatLng : LatLng? = null
    private var destinationLatLng : LatLng? = null

    private var wayPoints : ArrayList<LatLng> = ArrayList()
    private val WAY_POINT_TAG = "way_point_tag"
    private lateinit var directionUtil : DirectionUtil

    private var markerOrigin : Marker? = null
    private var markerDestination : Marker? = null

    private var configProvider = ConfigProvider()

    var distance = 0.0
    var time = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityTripInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        //Parametros Extras recibidos por otra pantalla
        extraOriginName = intent.getStringExtra("origin")!!
        extraDestinationName = intent.getStringExtra("destination")!!
        extraOriginLat = intent.getDoubleExtra("origin_lat", 0.0)
        extraOriginLng = intent.getDoubleExtra("origin_lng", 0.0)
        extraDestinationLat = intent.getDoubleExtra("destination_lat", 0.0)
        extraDestinationLng = intent.getDoubleExtra("destination_lng", 0.0)

        originLatLng = LatLng(extraOriginLat, extraOriginLng)
        destinationLatLng = LatLng(extraDestinationLat, extraDestinationLng)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }

        easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

        binding.tvOrigin.text = extraOriginName
        binding.tvDestination.text = extraDestinationName

        binding.ivBack.setOnClickListener {

            finish()

        }

        binding.btnConfirmRequest.setOnClickListener {
            goToSearchDriver()
        }

    }

    private fun goToSearchDriver(){

        if (originLatLng != null && destinationLatLng != null){

            val intent = Intent(this, SearchActivity::class.java)

            intent.putExtra("origin", extraOriginName)
            intent.putExtra("destination", extraDestinationName)
            intent.putExtra("origin_lat", originLatLng?.latitude)
            intent.putExtra("origin_lng", originLatLng?.longitude)
            intent.putExtra("destination_lat", destinationLatLng?.latitude)
            intent.putExtra("destination_lng", destinationLatLng?.longitude)
            intent.putExtra("time", time)
            intent.putExtra("distance", distance)

            startActivity(intent)

        }else{

            Toast.makeText(this, "Debes seleccionar el origen y el destino", Toast.LENGTH_LONG).show()

        }

    }

    private fun getPrices(distance: Double, time: Double){

        configProvider.getPrices().addOnSuccessListener { document ->

            if (document.exists()){

                val prices = document.toObject(Price::class.java)  //Obtenemos el documento completo con su información

                val totalDistance = distance * prices?.km!!  //valor por km
                val totalTime = time * prices?.min!!  //valor por min

                var total = totalDistance + totalTime  //Total a pagar
                total = if (total < 5.0) prices?.minValue!! else total

                var minTotal = total - prices.difference!!  //Al total se le restan 2
                var maxTotal = total + prices.difference!!  //Al total se le suman 2

                binding.tvPrice.text = "$ ${String.format("%.2f", minTotal)} - $ ${String.format("%.2f", maxTotal)}"

            }
        }

    }

    private fun addOriginMarker(){

        markerOrigin = googleMap?.addMarker(MarkerOptions()
            .position(originLatLng!!)
            .title("Origen")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_location_person))
        )

    }


    private fun addDestinationMarker(){

        markerDestination = googleMap?.addMarker(MarkerOptions()
            .position(destinationLatLng!!)
            .title("Destino")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_pin))
        )

    }

    private fun easyDrawRoute(){

        wayPoints.add(originLatLng!!)
        wayPoints.add(destinationLatLng!!)

        directionUtil = DirectionUtil.Builder()
            .setDirectionKey(resources.getString(R.string.google_maps_key))
            .setOrigin(originLatLng!!)
            .setWayPoints(wayPoints)
            .setGoogleMap(googleMap!!)
            .setPolyLinePrimaryColor(R.color.black)
            .setPolyLineWidth(10)
            .setPathAnimation(true)
            .setCallback(this)
            .setDestination(destinationLatLng!!)
            .build()

        directionUtil.initPath()

    }

    override fun onMapReady(map: GoogleMap) {

        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true

        googleMap?.moveCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(originLatLng!!).zoom(13f).build()
            )
        )

        easyDrawRoute()
        addOriginMarker()
        addDestinationMarker()

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

    override fun pathFindFinish(
        polyLineDetailsMap: HashMap<String,
        PolyLineDataBean>,
        polyLineDetailsArray: ArrayList<PolyLineDataBean>
    ) {

        distance = polyLineDetailsArray[1].distance.toDouble() //Mts
        time = polyLineDetailsArray[1].time.toDouble()  //Seg

        distance = if (distance < 1000.0) 1000.0 else distance   //Si es menos de 1km se pone por defecto de 1km
        time = if (time < 60.0) 60.0 else time

        distance /= 1000
        time /= 60

        getPrices(distance, time)

        binding.tvTimeAndDistance.text = "${String.format("%.2f", time).replace(".", ":")} minutos - ${String.format("%.3f", distance)} Km "

        directionUtil.drawPath(WAY_POINT_TAG)

    }

}