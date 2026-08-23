package com.google.ai.edge.gallery.capabilities.ui.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

class UniversalAgentTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TestSurface()
                }
            }
        }
    }
}

@Composable
fun TestSurface() {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Not submitted") }
    var optionEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .semantics { testTag = "test_surface_scroll" }
    ) {
        Text("Universal Agent Test Surface", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { testTag = "name_input" }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { testTag = "password_input" }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Switch(
                checked = optionEnabled,
                onCheckedChange = { optionEnabled = it },
                modifier = Modifier.semantics { testTag = "option_switch" }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Enable Option")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { status = "Submitted: $name" },
            modifier = Modifier.semantics { testTag = "submit_button" }
        ) {
            Text("Submit")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Status: $status", modifier = Modifier.semantics { testTag = "status_text" })
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Scrollable content padding
        for (i in 1..20) {
            Text("Scrollable item $i", modifier = Modifier.padding(8.dp))
        }
    }
}
