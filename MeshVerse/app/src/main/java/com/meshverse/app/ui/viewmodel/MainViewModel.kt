package com.meshverse.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.meshverse.app.domain.model.Conversation
import com.meshverse.app.domain.model.ConversationType
import com.meshverse.app.domain.model.Message
import com.meshverse.app.domain.model.MessageStatus
import com.meshverse.app.domain.model.MeshPacket
import com.meshverse.app.domain.model.PacketType
import com.meshverse.app.domain.model.Post
import com.meshverse.app.domain.model.PostType
import com.meshverse.app.domain.model.User
import com.meshverse.app.domain.repository.ConversationRepository
import com.meshverse.app.domain.repository.MessageRepository
import com.meshverse.app.domain.repository.PeerRepository
import com.meshverse.app.domain.repository.PostRepository
import com.meshverse.app.domain.repository.UserRepository
import com.meshverse.app.mesh.MeshNetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val peerRepository: PeerRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val meshNetworkManager: MeshNetworkManager,
    private val gson: Gson
) : ViewModel() {

    val conversations = conversationRepository.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val peers = peerRepository.getAllPeers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val feed = postRepository.getFeed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val localUser = userRepository.getLocalUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val connectedPeerCount: StateFlow<Int> = peerRepository.getConnectedPeerCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val isLocalOnlyMode = connectedPeerCount
        .map { it == 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun ensureSeedData() {
        viewModelScope.launch {
            if (conversations.value.isEmpty()) {
                conversationRepository.createConversation(
                    Conversation(
                        conversationId = "local-general",
                        type = ConversationType.COMMUNITY,
                        name = "Local General",
                        description = "Offline-first community room",
                        participantIds = emptyList(),
                        adminIds = emptyList(),
                        locality = "Nearby"
                    )
                )
            }

            if (feed.value.isEmpty()) {
                postRepository.createPost(
                    Post(
                        postId = UUID.randomUUID().toString(),
                        authorId = localUser.value?.userId ?: "local",
                        title = "MeshVerse MVP",
                        content = "Running in offline-first mode. Posts propagate when peers appear.",
                        postType = PostType.ANNOUNCEMENT,
                        tags = listOf("mesh", "offline", "mvp")
                    )
                )
            }
        }
    }

    fun sendMessage(conversationId: String, recipientId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val senderId = localUser.value?.userId ?: meshNetworkManager.getLocalNodeId().ifBlank { "local-node" }
            val message = Message(
                messageId = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = senderId,
                recipientId = recipientId,
                content = content,
                status = MessageStatus.PENDING,
                isOwn = true
            )
            messageRepository.sendMessage(message)
            conversationRepository.updateLastMessage(conversationId, content.take(80), System.currentTimeMillis())

            val packet = MeshPacket(
                sourceId = senderId,
                destinationId = recipientId,
                senderId = senderId,
                payloadType = PacketType.MESSAGE,
                payload = gson.toJson(message).toByteArray()
            )
            meshNetworkManager.sendPacket(packet)
        }
    }

    fun sendSos(message: String, lat: Double = 0.0, lon: Double = 0.0) {
        viewModelScope.launch {
            val userId = localUser.value?.userId ?: "anonymous"
            meshNetworkManager.sendSOS(userId, lat, lon, message)
        }
    }

    fun createPost(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            postRepository.createPost(
                    Post(
                        postId = UUID.randomUUID().toString(),
                        authorId = localUser.value?.userId ?: "local",
                        content = text,
                        postType = PostType.POST,
                        tags = Regex("""(?:^|\s)#([A-Za-z0-9_]+)""")
                            .findAll(text)
                            .map { it.groupValues[1] }
                            .take(20)
                            .toList()
                    )
            )
        }
    }

    fun saveLocalIdentity(username: String) {
        if (username.isBlank()) return
        viewModelScope.launch {
            val id = localUser.value?.userId ?: UUID.randomUUID().toString()
            userRepository.saveUser(
                User(
                    userId = id,
                    username = username,
                    displayName = username,
                    avatarPath = null,
                    isLocalUser = true,
                    isAnonymous = false,
                    publicKey = "",
                    deviceFingerprint = "local"
                )
            )
        }
    }
}
