package com.carlostorres.uberclone.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.carlostorres.uberclone.databinding.ActivityRegisterBinding
import com.carlostorres.uberclone.models.Client
import com.carlostorres.uberclone.providers.AuthProvider
import com.carlostorres.uberclone.providers.ClientProvider

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding : ActivityRegisterBinding

    private val authProvider = AuthProvider()

    private val clientProvider = ClientProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding= ActivityRegisterBinding.inflate(layoutInflater)

        setContentView(binding.root)

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        binding.btnGoToLogin.setOnClickListener {
            goToLogin()
        }

        binding.btnRegister.setOnClickListener {
            register()
        }

    }

    private fun goToLogin(){

        val i = Intent(this, MainActivity::class.java)
        startActivity(i)

    }

    private fun register(){

        val name = binding.textFieldName.text.toString()
        val lastName = binding.textFieldLastName.text.toString()
        val email = binding.textFieldEmail.text.toString()
        val phone = binding.textFieldPhone.text.toString()
        val password = binding.textFieldPassword.text.toString()
        val confirmPassword = binding.textFieldConfirmPassword.text.toString()

        if (isValidForm(name, lastName, email, phone, password, confirmPassword)){
            authProvider.register(email, password).addOnCompleteListener {
                if (it.isSuccessful){

                    val client = Client(
                        id = authProvider.getId(),
                        name = name,
                        lastname = lastName,
                        phone = phone,
                        email = email
                    )

                    clientProvider.create(client).addOnCompleteListener {
                        if (it.isSuccessful){
                            goToMap()
                        } else {
                            Toast.makeText(this@RegisterActivity, "Error almacenando datos ${it.exception.toString()}", Toast.LENGTH_SHORT).show()
                            Log.d("Firebase", it.exception.toString())
                        }
                    }

                } else {
                    Toast.makeText(this@RegisterActivity, "Registro fallido: ${it.exception.toString()}", Toast.LENGTH_SHORT).show()
                    Log.d("Firebase", "Registro fallido: ${it.exception.toString()}")
                }
            }
        }

    }

    private fun isValidForm(name: String, lastName: String, email: String, phone: String, password: String, confirmPassword: String): Boolean {

        if (email.isEmpty()){
            Toast.makeText(this, "Ingresa tu correo electronico", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()){
            Toast.makeText(this, "Ingresa tu contraseña", Toast.LENGTH_SHORT).show()
            return false
        }
        if (lastName.isEmpty()){
            Toast.makeText(this, "Ingresa tu apellido", Toast.LENGTH_SHORT).show()
            return false
        }
        if (name.isEmpty()){
            Toast.makeText(this, "Ingresa tu nombre", Toast.LENGTH_SHORT).show()
            return false
        }
        if (phone.isEmpty()){
            Toast.makeText(this, "Ingresa tu teléfono", Toast.LENGTH_SHORT).show()
            return false
        }
        if (confirmPassword.isEmpty()){
            Toast.makeText(this, "Confirma tu contraseña", Toast.LENGTH_SHORT).show()
            return false
        }
        if (confirmPassword!=password){
            Toast.makeText(this, "Las contraseñas deben cohincidir", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length<6){
            Toast.makeText(this, "La contraseña debe tener mas de 6 caracteres", Toast.LENGTH_LONG).show()
            return false
        }

        return true

    }

    fun goToMap(){

        val intent = Intent (this, MapActivity::class.java)

        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

        startActivity(intent)

    }

}