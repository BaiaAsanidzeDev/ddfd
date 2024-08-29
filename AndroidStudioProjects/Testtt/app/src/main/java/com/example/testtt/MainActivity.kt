package com.example.testtt

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.example.testtt.ui.theme.TestttTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            TestttTheme {
                Greeting(
                    context = LocalContext.current,
                    name = "Android",
                    modifier = Modifier.padding(top = 50.dp)
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier, context: Context) {
    Column(modifier = Modifier.padding(top = 100.dp)) {
        TextField(value = "dd", onValueChange = {})
        MyTextField(label = "ss", onValueChange = {}, textFieldValue = "ff")
        Button(onClick = {
//            val currentLanguage = LocaleListCompat.getDefault()[0]?.language.toString()
//            setAppLanguage( context, "en")
        }) {
            Text(text = "click")
        }
    }
}

fun setAppLanguage(context: Context, languageCode: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Android 13 and above use Per-app language preferences
        context.getSystemService(android.app.LocaleManager::class.java)?.applicationLocales =
            LocaleList.forLanguageTags(languageCode)
    } else {
        // Fallback for older versions
        setLocaleLegacy(context, languageCode)
        restartActivity(context)
    }
//    changeLanguage(languageCode)
}

// Fallback method for setting locale on older versions
@Suppress("DEPRECATION")
fun setLocaleLegacy(context: Context, languageCode: String): Context {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    val config = context.resources.configuration
    config.setLocale(locale)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.createConfigurationContext(config)
    } else {
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        context
    }
}

// Restart the current activity
fun restartActivity(context: Context) {
    val intent = (context as Activity).intent
    context.finish()
    context.startActivity(intent)
}

