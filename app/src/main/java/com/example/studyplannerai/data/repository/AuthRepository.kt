package com.example.studyplannerai.data.repository
import com.google.firebase.auth.FirebaseAuth
class AuthRepository {
    private val auth: FirebaseAuth=FirebaseAuth.getInstance();
    fun signUp(email:String, password:String,
               onResult: (Boolean, String?) -> Unit
               ){
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    onResult(true,null)
                }
                else{
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun logIn(email: String,password: String,onResult: (Boolean, String?) -> Unit){
        auth.signInWithEmailAndPassword(email,password).addOnCompleteListener { task ->
            if(task.isSuccessful){
                onResult(true,null)
            }
            else{
                onResult(false, task.exception?.message)
            }
        }
    }

    fun logOut(){
        auth.signOut()
    }

    fun getCurrentUser()=  auth.currentUser

}