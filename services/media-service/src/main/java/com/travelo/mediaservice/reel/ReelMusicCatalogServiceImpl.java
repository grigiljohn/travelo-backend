package com.travelo.mediaservice.reel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Default catalog uses SoundHelix example MP3s (replace with licensed S3 objects in production).
 */
@Service
public class ReelMusicCatalogServiceImpl implements ReelMusicCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ReelMusicCatalogServiceImpl.class);

    private final List<ReelMusicTrack> tracks;

    public ReelMusicCatalogServiceImpl(
            @Value("${reel.music.override-urls:}") String overrideCsv) {
        this.tracks = buildCatalog(overrideCsv);
        log.info("Reel music catalog loaded: {} tracks", tracks.size());
    }

    private static List<ReelMusicTrack> buildCatalog(String overrideCsv) {
        List<ReelMusicTrack> list = new ArrayList<>();
        if (overrideCsv != null && !overrideCsv.isBlank()) {
            int i = 0;
            for (String part : overrideCsv.split(",")) {
                String url = part.trim();
                if (url.isEmpty()) {
                    continue;
                }
                ReelMusicCategory cat = switch (i % 3) {
                    case 0 -> ReelMusicCategory.ENERGETIC;
                    case 1 -> ReelMusicCategory.CHILL;
                    default -> ReelMusicCategory.CINEMATIC;
                };
                list.add(new ReelMusicTrack("ov" + i, "Catalog " + i, cat, url));
                i++;
            }
        }
        if (!list.isEmpty()) {
            return List.copyOf(list);
        }
        for (int n = 1; n <= 5; n++) {
            String u = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-" + n + ".mp3";
            list.add(new ReelMusicTrack("e" + n, "Helix Drive " + n, ReelMusicCategory.ENERGETIC, u));
        }
        for (int n = 6; n <= 10; n++) {
            String u = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-" + n + ".mp3";
            list.add(new ReelMusicTrack("c" + n, "Helix Chill " + n, ReelMusicCategory.CHILL, u));
        }
        for (int n = 11; n <= 15; n++) {
            String u = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-" + n + ".mp3";
            list.add(new ReelMusicTrack("m" + n, "Helix Cinematic " + n, ReelMusicCategory.CINEMATIC, u));
        }
        return List.copyOf(list);
    }

    @Override
    public ReelMusicTrack pickRandom(ReelMusicCategory category) {
        List<ReelMusicTrack> sub = tracks.stream()
                .filter(t -> t.category() == category)
                .toList();
        if (sub.isEmpty()) {
            return tracks.get(ThreadLocalRandom.current().nextInt(tracks.size()));
        }
        return sub.get(ThreadLocalRandom.current().nextInt(sub.size()));
    }

    @Override
    public ReelMusicCategory categoryForFilter(ReelFilterType filter) {
        if (filter == null) {
            return ReelMusicCategory.CHILL;
        }
        return switch (filter) {
            case CINEMATIC, DRAMATIC -> ReelMusicCategory.CINEMATIC;
            case VIBRANT -> ReelMusicCategory.ENERGETIC;
            case COOL, MONO, SOFT_SKIN, NONE -> ReelMusicCategory.CHILL;
        };
    }
}
