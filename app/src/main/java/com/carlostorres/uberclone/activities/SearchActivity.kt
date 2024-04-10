package com.carlostorres.uberclone.activities

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.carlostorres.uberclone.R
import com.carlostorres.uberclone.databinding.ActivitySearchBinding
import com.carlostorres.uberclone.models.Booking
import com.carlostorres.uberclone.providers.AuthProvider
import com.carlostorres.uberclone.providers.BookingProvider
import com.carlostorres.uberclone.providers.GeoProvider
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import org.imperiumlabs.geofirestore.callbacks.GeoQueryEventListener

class SearchActivity : AppCompatActivity() {

    private lateinit var binding : ActivitySearchBinding

    private var extraOriginName = ""
    private var extraDestinationName = ""
    private var extraOriginLat = 0.0
    private var extraOriginLng = 0.0
    private var extraDestinationLat = 0.0
    private var extraDestinationLng = 0.0
    private var extraTime = 0.0
    private var extraDistance = 0.0

    private var originLatLng : LatLng? = null
    private var destinationLatLng : LatLng? = null

    private val geoProvider = GeoProvider()
    private val authProvider = AuthProvider()

            //Busqueda driver
    private var radius = 0.1
    private var idDriver = ""
    private var isDriverFound = false
    private var driverLatLng : LatLng? = null
    private var limitRadius = 20.0

    private val bookingProvider = BookingProvider()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
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
        extraTime = intent.getDoubleExtra("time", 0.0)
        extraDistance = intent.getDoubleExtra("distance", 0.0)

        originLatLng = LatLng(extraOriginLat, extraOriginLng)
        destinationLatLng = LatLng(extraDestinationLat, extraDestinationLng)

        getClosestDrivers()

    }

    private fun createBooking(idDriver : String){

        val booking = Booking(

            idClient = authProvider.getId(),
            idDriver = idDriver,
            status = "create",
            destination = extraDestinationName,
            origin = extraOriginName,
            time = extraTime,
            km = extraDistance,
            originLat = extraOriginLat,
            originLng = extraOriginLng,
            destinationLat = extraDestinationLat,
            destinationLng = extraDestinationLng

        )
        
        bookingProvider.create(booking).addOnCompleteListener { 
            
            if (it.isSuccessful){

                Toast.makeText(this@SearchActivity, "Datos del viaje creados", Toast.LENGTH_SHORT).show()
                
            } else {

                Toast.makeText(this@SearchActivity, "Error al crear el viaje", Toast.LENGTH_SHORT).show()

            }
            
        }

    }

    private fun getClosestDrivers(){

        geoProvider.getNearbyDrivers(originLatLng!!, radius).addGeoQueryEventListener(object : GeoQueryEventListener{

            override fun onKeyEntered(documentID: String, location: GeoPoint) {

                if (!isDriverFound){

                    isDriverFound = true
                    idDriver = documentID
                    driverLatLng = LatLng(location.latitude, location.longitude)

                    binding.tvSearch.text = "Conductor Encontrado\nEsperando Respuesta Del Conductor"

                    createBooking(documentID)

                }

            }

            override fun onKeyExited(documentID: String) {

            }

            override fun onKeyMoved(documentID: String, location: GeoPoint) {

            }

            override fun onGeoQueryError(exception: Exception) {

            }

            override fun onGeoQueryReady() {

                if (!isDriverFound){

                    radius += 0.1

                    if (radius > limitRadius){

                        binding.tvSearch.text = "NO SE ENCONTRÓ NINGÚN CONDUCTOR"

                        return

                    } else {

                        getClosestDrivers()

                    }

                }

            }


        })

    }

}