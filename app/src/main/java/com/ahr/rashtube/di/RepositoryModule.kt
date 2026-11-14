package com.ahr.rashtube.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.ahr.rashtube.data.local.PlayerPreferences
import com.ahr.rashtube.data.repository.YouTubeRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideYouTubeRepository(playerPreferences: PlayerPreferences): YouTubeRepository = YouTubeRepository.getInstance(playerPreferences)

    @Provides
    @Singleton
    fun provideSubscriptionRepository(
        @ApplicationContext context: Context,
    ): com.ahr.rashtube.data.local.SubscriptionRepository =
        com.ahr.rashtube.data.local.SubscriptionRepository
            .getInstance(context)

    @Provides
    @Singleton
    fun provideLikedVideosRepository(
        @ApplicationContext context: Context,
    ): com.ahr.rashtube.data.local.LikedVideosRepository =
        com.ahr.rashtube.data.local.LikedVideosRepository
            .getInstance(context)

    @Provides
    @Singleton
    fun provideViewHistory(
        @ApplicationContext context: Context,
    ): com.ahr.rashtube.data.local.ViewHistory =
        com.ahr.rashtube.data.local.ViewHistory
            .getInstance(context)

    @Provides
    @Singleton
    fun provideHomeFeedCacheRepository(
        @ApplicationContext context: Context,
    ): com.ahr.rashtube.data.local.HomeFeedCacheRepository =
        com.ahr.rashtube.data.local
            .HomeFeedCacheRepository(context)

    @Provides
    @Singleton
    fun provideMusicPlaylistRepository(
        @ApplicationContext context: Context,
    ): com.ahr.rashtube.data.music.PlaylistRepository =
        com.ahr.rashtube.data.music
            .PlaylistRepository(context)

    // VideoDownloadManager is now @Singleton @Inject — Hilt provides it automatically
    @Provides
    @Singleton
    fun providePlayerPreferences(
        @ApplicationContext context: Context,
    ): com.ahr.rashtube.data.local.PlayerPreferences =
        com.ahr.rashtube.data.local
            .PlayerPreferences(context)

    /** Transitional: the music player manager still reaches the queue store through getInstance, so this hands injected consumers the same instance. */
    @Provides
    @Singleton
    fun provideQueuePersistence(
        @ApplicationContext context: Context,
    ): com.ahr.rashtube.data.local.QueuePersistence =
        com.ahr.rashtube.data.local.QueuePersistence
            .getInstance(context)

    /**
     * Transitional: [com.ahr.rashtube.data.local.BackupRepository] still builds its own
     * collaborators, so it is provided here rather than injected through its constructor. One
     * instance serves the whole app.
     */
    @Provides
    @Singleton
    fun provideBackupRepository(
        @ApplicationContext context: Context,
    ): com.ahr.rashtube.data.local.BackupRepository =
        com.ahr.rashtube.data.local
            .BackupRepository(context)
}
