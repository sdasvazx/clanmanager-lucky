package com.clanmanager.clanmanager.service;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.MusicWithThumbnail;
import dev.lavalink.youtube.clients.MWebWithThumbnail;
import dev.lavalink.youtube.clients.TvHtml5SimplyWithThumbnail;
import dev.lavalink.youtube.clients.WebWithThumbnail;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

@Service
public class DiscordMusicService {

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers = new ConcurrentHashMap<>();

    public DiscordMusicService() {
        playerManager = new DefaultAudioPlayerManager();
        // YouTube regularly retires or blocks InnerTube clients. In particular,
        // WEB_EMBEDDED_PLAYER and ANDROID_VR can accept a search result and then
        // fail as soon as playback starts. TVHTML5_SIMPLY is the current playback
        // client recommended by youtube-source; the other clients remain as
        // search/metadata and playback fallbacks.
        playerManager.registerSourceManager(new YoutubeAudioSourceManager(
                true,
                new MusicWithThumbnail(),
                new TvHtml5SimplyWithThumbnail(),
                new MWebWithThumbnail(),
                new WebWithThumbnail()
        ));
        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
    }

    public void play(Guild guild, AudioChannel voiceChannel, String query, String requester, Consumer<String> callback) {
        GuildMusicManager musicManager = getMusicManager(guild);
        guild.getAudioManager().openAudioConnection(voiceChannel);

        // Railway data-center IPs are frequently challenged by YouTube even
        // when metadata search succeeds. Use SoundCloud for ordinary song-name
        // searches so playback remains reliable; explicit YouTube URLs are
        // still handled by youtube-source.
        String identifier = isUrl(query) ? query : "scsearch:" + query;
        playerManager.loadItemOrdered(musicManager, identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                enqueue(track, requester, musicManager, callback);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack track = playlist.getSelectedTrack();
                if (track == null && !playlist.getTracks().isEmpty()) {
                    track = playlist.getTracks().get(0);
                }
                if (track == null) {
                    callback.accept("검색 결과가 없습니다.");
                    return;
                }
                enqueue(track, requester, musicManager, callback);
            }

            @Override
            public void noMatches() {
                callback.accept("유튜브에서 검색 결과를 찾지 못했습니다.");
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                callback.accept("노래를 불러오지 못했습니다. 다른 검색어나 유튜브 주소로 다시 시도해 주세요.");
            }
        });
    }

    public String stop(Guild guild) {
        GuildMusicManager manager = managerFor(guild);
        if (manager == null) {
            return "현재 재생 중인 노래가 없습니다.";
        }
        manager.scheduler.clear();
        manager.player.stopTrack();
        return "⏹️ 재생을 중지하고 대기열을 비웠습니다.";
    }

    public String skip(Guild guild) {
        GuildMusicManager manager = managerFor(guild);
        if (manager == null || manager.player.getPlayingTrack() == null) {
            return "현재 재생 중인 노래가 없습니다.";
        }
        AudioTrack next = manager.scheduler.nextTrack();
        return next == null ? "⏭️ 현재 곡을 건너뛰었습니다. 대기 중인 다음 곡은 없습니다."
                : "⏭️ 다음 곡: **%s**".formatted(next.getInfo().title);
    }

    public String leave(Guild guild) {
        if (guild == null) {
            return "Discord 서버에서만 사용할 수 있습니다.";
        }
        GuildMusicManager manager = musicManagers.remove(guild.getIdLong());
        if (manager != null) {
            manager.scheduler.clear();
            manager.player.stopTrack();
            manager.player.destroy();
        }
        guild.getAudioManager().closeAudioConnection();
        return "👋 재생을 중지하고 음성 채널에서 나갔습니다.";
    }

    public String queueStatus(Guild guild) {
        GuildMusicManager manager = managerFor(guild);
        if (manager == null || manager.player.getPlayingTrack() == null) {
            return "현재 재생 중인 노래와 대기열이 없습니다.";
        }
        StringBuilder message = new StringBuilder("🎵 **현재 재생**\n")
                .append(formatTrack(manager.player.getPlayingTrack()));
        List<AudioTrack> queued = manager.scheduler.snapshot();
        if (queued.isEmpty()) {
            return message.append("\n\n대기 중인 곡이 없습니다.").toString();
        }
        message.append("\n\n📋 **신청 대기열**\n");
        int limit = Math.min(queued.size(), 15);
        for (int index = 0; index < limit; index++) {
            message.append(index + 1).append(". ").append(formatTrack(queued.get(index))).append('\n');
        }
        if (queued.size() > limit) {
            message.append("외 ").append(queued.size() - limit).append("곡");
        }
        return message.toString().trim();
    }

    private void enqueue(AudioTrack track, String requester, GuildMusicManager manager, Consumer<String> callback) {
        track.setUserData(requester);
        int position = manager.scheduler.enqueue(track);
        if (position == 0) {
            callback.accept("▶️ 지금 재생합니다: **%s** · %s".formatted(track.getInfo().title, track.getInfo().author));
        } else {
            callback.accept("✅ 신청곡을 대기열 %d번에 추가했습니다: **%s** · %s"
                    .formatted(position, track.getInfo().title, track.getInfo().author));
        }
    }

    private GuildMusicManager getMusicManager(Guild guild) {
        return musicManagers.computeIfAbsent(guild.getIdLong(), ignored -> {
            GuildMusicManager manager = new GuildMusicManager(playerManager.createPlayer());
            guild.getAudioManager().setSendingHandler(new DiscordAudioSendHandler(manager.player));
            return manager;
        });
    }

    private GuildMusicManager managerFor(Guild guild) {
        return guild == null ? null : musicManagers.get(guild.getIdLong());
    }

    private static boolean isUrl(String query) {
        return query.startsWith("https://") || query.startsWith("http://");
    }

    private static String formatTrack(AudioTrack track) {
        Object requester = track.getUserData();
        return "**%s** · %s%s".formatted(
                track.getInfo().title,
                track.getInfo().author,
                requester == null ? "" : " (신청: " + requester + ")"
        );
    }

    @PreDestroy
    public void shutdown() {
        playerManager.shutdown();
    }

    private static final class GuildMusicManager {
        private final AudioPlayer player;
        private final TrackScheduler scheduler;

        private GuildMusicManager(AudioPlayer player) {
            this.player = player;
            this.scheduler = new TrackScheduler(player);
            player.addListener(scheduler);
        }
    }

    private static final class TrackScheduler extends AudioEventAdapter {
        private final AudioPlayer player;
        private final LinkedBlockingQueue<AudioTrack> queue = new LinkedBlockingQueue<>();

        private TrackScheduler(AudioPlayer player) {
            this.player = player;
        }

        private synchronized int enqueue(AudioTrack track) {
            if (player.startTrack(track, true)) {
                return 0;
            }
            queue.offer(track);
            return queue.size();
        }

        private synchronized AudioTrack nextTrack() {
            AudioTrack next = queue.poll();
            player.startTrack(next, false);
            return next;
        }

        private void clear() {
            queue.clear();
        }

        private List<AudioTrack> snapshot() {
            return new ArrayList<>(queue);
        }

        @Override
        public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
            if (endReason.mayStartNext) {
                nextTrack();
            }
        }
    }
}
