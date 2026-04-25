package com.example.studyplannerai.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyplannerai.ui.theme.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatFab(
    onSendMessage: suspend (String, List<ChatMessage>) -> String,
    modifier: Modifier = Modifier
) {
    var showChat by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val messages = remember { mutableStateListOf(
        ChatMessage("Hi! I'm your AI study assistant. Ask me anything about your subjects, study strategies, or get explanations for any topic! 🎓", isUser = false)
    ) }

    val inf = rememberInfiniteTransition(label = "fabPulse")
    val fabScale by inf.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fabScale"
    )
    val glowAlpha by inf.animateFloat(
        initialValue = 0.2f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fabGlow"
    )

    Box(modifier = modifier) {
        // Glow ring
        Box(Modifier.size((72 * fabScale).dp).clip(CircleShape).background(Violet400.copy(glowAlpha)).align(Alignment.Center))
        // FAB button
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Violet500, Cyan400)))
                .clickable { showChat = true }
                .scale(fabScale)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, "AI Chat", tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }

    if (showChat) {
        ModalBottomSheet(
            onDismissRequest = { showChat = false },
            sheetState = sheetState,
            containerColor = Surface800,
            dragHandle = {
                Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(Surface600))
                }
            }
        ) {
            AiChatContent(messages = messages, onSendMessage = onSendMessage)
        }
    }
}

@Composable
private fun AiChatContent(messages: androidx.compose.runtime.snapshots.SnapshotStateList<ChatMessage>, onSendMessage: suspend (String, List<ChatMessage>) -> String) {
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isBlank() || isLoading) return
        inputText = ""
        messages.add(ChatMessage(text, isUser = true))
        messages.add(ChatMessage("", isUser = false, isLoading = true))
        isLoading = true
        scope.launch {
            try {
                val response = onSendMessage(text, messages.dropLast(1))
                messages.removeLastOrNull()
                messages.add(ChatMessage(response, isUser = false))
            } catch (e: Exception) {
                messages.removeLastOrNull()
                messages.add(ChatMessage("Sorry, I couldn't respond right now. Please try again.", isUser = false))
            }
            isLoading = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).navigationBarsPadding()) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Violet500.copy(0.2f), Cyan400.copy(0.1f)))).padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Violet500, Cyan400))), contentAlignment = Alignment.Center) {
                    Text("✦", fontSize = 20.sp, color = Color.White)
                }
                Column {
                    Text("AI Study Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurface100)
                    Text("Powered by Groq", style = MaterialTheme.typography.labelSmall, color = OnSurface300)
                }
            }
        }

        // Quick prompts
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Study tips", "Explain a topic", "Motivation").forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Violet400.copy(0.1f),
                    border = BorderStroke(1.dp, Violet400.copy(0.25f)),
                    modifier = Modifier.clickable {
                        inputText = prompt
                    }
                ) {
                    Text(prompt, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall, color = Violet300)
                }
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
        }

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp).imePadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask anything...", color = OnSurface300) },
                shape = RoundedCornerShape(20.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Violet400, unfocusedBorderColor = Surface600,
                    focusedTextColor = OnSurface100, unfocusedTextColor = OnSurface100,
                    cursorColor = Violet300, focusedContainerColor = Surface700,
                    unfocusedContainerColor = Surface700
                )
            )
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(if (inputText.isNotBlank()) Brush.linearGradient(listOf(Violet500, Cyan400)) else Brush.linearGradient(listOf(Surface600, Surface600)))
                    .clickable(enabled = inputText.isNotBlank()) { sendMessage() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(Icons.Default.Send, null, tint = if (inputText.isNotBlank()) Color.White else OnSurface300, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!msg.isUser) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Violet500, Cyan400))), contentAlignment = Alignment.Center) {
                Text("✦", fontSize = 12.sp, color = Color.White)
            }
            Spacer(Modifier.width(6.dp))
        }
        Box(
            modifier = Modifier.widthIn(max = 280.dp)
                .clip(RoundedCornerShape(topStart = if (msg.isUser) 18.dp else 4.dp, topEnd = if (msg.isUser) 4.dp else 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(if (msg.isUser) Brush.linearGradient(listOf(Violet500.copy(0.8f), Violet400.copy(0.6f))) else Brush.linearGradient(listOf(Surface700, Surface700)))
                .border(BorderStroke(1.dp, if (msg.isUser) Violet400.copy(0.3f) else Surface600), RoundedCornerShape(topStart = if (msg.isUser) 18.dp else 4.dp, topEnd = if (msg.isUser) 4.dp else 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (msg.isLoading) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(3) { i ->
                        val inf = rememberInfiniteTransition(label = "dot$i")
                        val alpha by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, i * 200), RepeatMode.Reverse), label = "da$i")
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Violet300.copy(alpha)))
                    }
                }
            } else {
                Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = if (msg.isUser) Color.White else OnSurface100)
            }
        }
    }
}
