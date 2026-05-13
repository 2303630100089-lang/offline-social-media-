package com.meshverse.app.di

import com.meshverse.app.data.repository.*
import com.meshverse.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds @Singleton
    abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository

    @Binds @Singleton
    abstract fun bindPeerRepository(impl: PeerRepositoryImpl): PeerRepository

    @Binds @Singleton
    abstract fun bindPostRepository(impl: PostRepositoryImpl): PostRepository

    @Binds @Singleton
    abstract fun bindMeshRepository(impl: MeshRepositoryImpl): MeshRepository

    @Binds @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds @Singleton
    abstract fun bindCommentRepository(impl: CommentRepositoryImpl): CommentRepository
}

