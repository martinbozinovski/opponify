package com.opponify.security

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name=["opponify.security.dev-mode"],havingValue="false",matchIfMissing=true)
class FirebaseAuthConfig(@Value("\${FIREBASE_SERVICE_ACCOUNT_JSON:}") private val serviceAccountJson:String){
    @Bean
    fun firebaseAuth():FirebaseAuth {
        if(FirebaseApp.getApps().isEmpty()){
            val builder=FirebaseOptions.builder()
            if(serviceAccountJson.isNotBlank()) builder.setCredentials(GoogleCredentials.fromStream(serviceAccountJson.byteInputStream()))
            else builder.setCredentials(GoogleCredentials.getApplicationDefault())
            FirebaseApp.initializeApp(builder.build())
        }
        return FirebaseAuth.getInstance()
    }
}
