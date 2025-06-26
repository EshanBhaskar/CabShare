package com.project.cabshare.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.project.cabshare.data.ChatRepository
import com.project.cabshare.data.FirestoreChatRepositoryImpl
import com.project.cabshare.models.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Date

class ChatViewModel(
    private val repository: ChatRepository = FirestoreChatRepositoryImpl(),
    private val userEmail: String,
    private val userName: String
) : ViewModel() {
    private val TAG = "ChatViewModel"
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private var currentRideId: String? = null
    private var lastMessageTimestamp: Long? = null
    private val PAGE_SIZE = 50
    
    fun startObservingMessages(rideId: String) {
        currentRideId = rideId
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting to observe messages for ride: $rideId")
                repository.observeMessages(rideId, userEmail)
                    .catch { e ->
                        Log.e(TAG, "Error observing messages", e)
                        _error.value = "Error loading messages: ${e.message}"
                    }
                    .collect { messages ->
                        Log.d(TAG, "Received ${messages.size} messages in real-time")
                        _messages.value = messages
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in message observation", e)
                _error.value = "Error in message observation: ${e.message}"
            }
        }
    }
    
    fun loadInitialMessages(rideId: String) {
        currentRideId = rideId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "Loading initial messages for ride: $rideId")
                val messages = repository.getMessages(rideId, PAGE_SIZE, null, userEmail)
                Log.d(TAG, "Loaded ${messages.size} initial messages")
                _messages.value = messages
                lastMessageTimestamp = messages.lastOrNull()?.timestamp?.time
            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial messages", e)
                _error.value = "Failed to load messages: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadMoreMessages() {
        val rideId = currentRideId ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val olderMessages = repository.getMessages(rideId, PAGE_SIZE, lastMessageTimestamp, userEmail)
                if (olderMessages.isNotEmpty()) {
                    _messages.value = _messages.value + olderMessages
                    lastMessageTimestamp = olderMessages.lastOrNull()?.timestamp?.time
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading more messages", e)
                _error.value = "Failed to load more messages: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val rideId = currentRideId ?: return
        
        viewModelScope.launch {
            try {
                val message = ChatMessage(
                    rideId = rideId,
                    senderEmail = userEmail,
                    senderName = userName,
                    message = text,
                    timestamp = Date()
                )
                repository.sendMessage(message)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                _error.value = "Failed to send message: ${e.message}"
            }
        }
    }
    
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                repository.markMessageAsDeleted(messageId, userEmail)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting message", e)
                _error.value = "Failed to delete message: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    class Factory(
        private val userEmail: String,
        private val userName: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                return ChatViewModel(FirestoreChatRepositoryImpl(), userEmail, userName) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 