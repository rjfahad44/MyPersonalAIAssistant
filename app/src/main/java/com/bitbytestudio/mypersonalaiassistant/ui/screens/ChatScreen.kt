package com.bitbytestudio.mypersonalaiassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bitbytestudio.mypersonalaiassistant.ChatViewModel
import kotlinx.coroutines.delay


@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val isThinking by viewModel.isThinking.collectAsState()
    val messages by viewModel.messages.collectAsState()
    var prompt by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(messages.size, key = { index -> "$index-${messages[index].first.hashCode()}" }) { index ->
                    val (message, isUser) = messages[index]
                    ChatBubble(message = message, isUser = isUser)
                    Spacer(Modifier.height(4.dp))
                }
                if (isThinking) {
                    item {
                        TypingIndicator()
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp)
                .fillMaxWidth()
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
                placeholder = { Text("Type your message...") },
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                )
            )

            IconButton(
                onClick = {
                    if (prompt.isNotBlank()) {
                        viewModel.sendMessage(prompt)
                        prompt = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}


@Composable
fun ChatBubble(message: String, isUser: Boolean) {
    val (alignment, padding, title) = if (isUser){
        Triple(Alignment.CenterEnd, 16.dp, "USER")
    } else {
        Triple(Alignment.CenterStart, 16.dp, "AI")
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isUser) padding else 0.dp, end = if (isUser) 0.dp else padding),
        contentAlignment = alignment
    ) {
        Column {
            Text(
                text = title,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = if (isUser) TextAlign.End else TextAlign.Start
            )
            Box(modifier = Modifier
                .fillMaxWidth()
            ) {
                Text(
                    text = message,
                    modifier = Modifier
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(12.dp))
                        .background(color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
                        .padding(10.dp)
                        .align(if (isUser) Alignment.CenterEnd else Alignment.CenterStart),
                    color = if (isUser) Color.White else Color.Black
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    var dotCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotCount = (dotCount + 1) % 4
        }
    }

    Text(
        text = "Thinking" + ".".repeat(dotCount),
        modifier = Modifier
            .padding(8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

