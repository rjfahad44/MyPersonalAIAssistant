package com.bitbytestudio.mypersonalaiassistant.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bitbytestudio.mypersonalaiassistant.ChatUiMessage
import com.bitbytestudio.mypersonalaiassistant.ChatViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val isLoadingModel by viewModel.isLoadingModel.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val messages by viewModel.messages.collectAsState()
    var prompt by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE8F5E9), Color.White)
                )
            )
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                //reverseLayout = true
            ) {
                items(messages.size, key = { index -> "$index-${messages[index].timestamp.hashCode()}" }) { index ->
                    val message = messages[index]
                    ChatBubble(message = message)
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
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = prompt,
                onValueChange = { prompt = it },
                maxLines = 5,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
                placeholder = { Text("Type your message...") },
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            IconButton(
                onClick = {
                    if (isThinking) {
                        viewModel.cancelGeneration()
                    } else if (prompt.isNotBlank()) {
                        viewModel.sendMessage(prompt)
                        prompt = ""
                    }
                },
                modifier = Modifier
            ) {
                Icon(
                    imageVector = if (isThinking) Icons.Default.Stop else Icons.Default.Send,
                    contentDescription = if (isThinking) "Stop" else "Send",
                    tint = LocalContentColor.current
                )
            }
        }

        if (isLoadingModel) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(message: ChatUiMessage) {
    val backgroundColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
    val textColor = if (message.isUser) Color.White else Color.Black
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleShape = if (message.isUser) {
        RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val currentTime = remember {
        SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date(message.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .background(color = backgroundColor, shape = bubbleShape)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        clipboardManager.setText(AnnotatedString(message.text))
                        Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 300.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = currentTime,
                color = textColor.copy(alpha = 0.6f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.End)
            )
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

    Row(
        modifier = Modifier
            .padding(8.dp)
            .wrapContentWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "Thinking" + ".".repeat(dotCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}



