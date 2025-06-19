package com.bitbytestudio.mypersonalaiassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.bitbytestudio.mypersonalaiassistant.ui.screens.ChatScreen
import com.bitbytestudio.mypersonalaiassistant.ui.theme.MyPersonalAIAssistantTheme
import com.bitbytestudio.mypersonalaiassistant.utils.getFileNameFromUri
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ChatViewModel = hiltViewModel()
            val pickModelFileLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
                onResult = { uri ->
                    uri?.let {
                        viewModel.modelName.value = getFileNameFromUri(this, it) ?: "model.gguf"
                        viewModel.loadModelFromUri(it)
                    }
                }
            )
            MyPersonalAIAssistantTheme {
                Scaffold(
                    topBar = {
                        AppBar(
                            title = viewModel.modelName.value.takeIf { it.isNotEmpty() },
                            onModelSelect = {
                                pickModelFileLauncher.launch(arrayOf("application/octet-stream"))
                            },
                            onChatClear = {
                                viewModel.clearChat()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
                    ) {
                        ChatScreen(modifier = Modifier.fillMaxSize(), viewModel = viewModel)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(title: String? = null, onModelSelect: () -> Unit, onChatClear: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                title?:stringResource(R.string.select_model),
                textAlign = TextAlign.Center,
                modifier = Modifier.basicMarquee(
                    animationMode = MarqueeAnimationMode.Immediately
                    ))
        },
        actions = {
            IconButton(onClick = onModelSelect) {
                Icon(Icons.Default.FileOpen, contentDescription = "Select Model")
            }
            IconButton(onClick = onChatClear) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Chat")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    MyPersonalAIAssistantTheme {
        //ChatScreen()
    }
}