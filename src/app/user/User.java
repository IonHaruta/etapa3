package app.user;

import app.Admin;
import app.audio.Collections.Album;
import app.audio.Collections.AudioCollection;
import app.audio.Collections.Playlist;
import app.audio.Collections.PlaylistOutput;
import app.audio.Files.AudioFile;
import app.audio.Files.Song;
import app.audio.LibraryEntry;
import app.pages.HomePage;
import app.pages.LikedContentPage;
import app.pages.Page;
import app.player.Player;
import app.player.PlayerStats;
import app.searchBar.Filters;
import app.searchBar.SearchBar;
import app.utils.Enums;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fileio.input.CommandInput;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;


/**
 * The type User.
 */
public final class User extends UserAbstract {
    @Getter
    private ArrayList<Playlist> playlists;
    @Getter
    private ArrayList<Song> likedSongs;
    @Getter
    private ArrayList<Playlist> followedPlaylists;
    @Getter
    private final Player player;
    @Getter
    private boolean status;
    private final SearchBar searchBar;
    private boolean lastSearched;
    @Getter
    @Setter
    private Page currentPage;
    @Getter
    @Setter
    private HomePage homePage;
    @Getter
    @Setter
    private LikedContentPage likedContentPage;
    @Getter
    @Setter
    private Map<String, Integer> artistNames = new HashMap<>();
    @Getter
    @Setter
    private Map<String, Integer> genreName = new HashMap<>();
    @Getter
    @Setter
    private Map<String, Integer> songName = new HashMap<>();
    @Getter
    @Setter
    private Map<String, Integer> albumName = new HashMap<>();
    @Getter
    @Setter
    private String currentSong = null;
    @Setter
    @Getter
    private List<String> songList = new ArrayList<>();
    @Getter
    @Setter
    private List<Map<String, Integer>> result = new ArrayList<>();
    private final int wrappedLimit = 5;


    /**
     * Instantiates a new User.
     *
     * @param username the username
     * @param age      the age
     * @param city     the city
     */
    public User(final String username, final int age, final String city) {
        super(username, age, city);
        playlists = new ArrayList<>();
        likedSongs = new ArrayList<>();
        followedPlaylists = new ArrayList<>();
        player = new Player();
        searchBar = new SearchBar(username);
        lastSearched = false;
        status = true;

        homePage = new HomePage(this);
        currentPage = homePage;
        likedContentPage = new LikedContentPage(this);
    }

    @Override
    public String userType() {
        return "user";
    }

    /**
     * Search array list.
     *
     * @param filters the filters
     * @param type    the type
     * @return the array list
     */
    public ArrayList<String> search(final Filters filters, final String type) {
        searchBar.clearSelection();
        player.stop();

        lastSearched = true;
        ArrayList<String> results = new ArrayList<>();

        if (type.equals("artist") || type.equals("host")) {
            List<ContentCreator> contentCreatorsEntries =
                    searchBar.searchContentCreator(filters, type);

            for (ContentCreator contentCreator : contentCreatorsEntries) {
                results.add(contentCreator.getUsername());
            }
        } else {
            List<LibraryEntry> libraryEntries = searchBar.search(filters, type);

            for (LibraryEntry libraryEntry : libraryEntries) {
                results.add(libraryEntry.getName());
            }
        }
        return results;
    }

    /**
     * Select string.
     *
     * @param itemNumber the item number
     * @return the string
     */
    public String select(final int itemNumber) {
        if (!status) {
            return "%s is offline.".formatted(getUsername());
        }

        if (!lastSearched) {
            return "Please conduct a search before making a selection.";
        }

        lastSearched = false;

        if (searchBar.getLastSearchType().equals("artist")
                || searchBar.getLastSearchType().equals("host")) {
            ContentCreator selected = searchBar.selectContentCreator(itemNumber);

            if (selected == null) {
                return "The selected ID is too high.";
            }

            currentPage = selected.getPage();
            return "Successfully selected %s's page.".formatted(selected.getUsername());
        } else {
            LibraryEntry selected = searchBar.select(itemNumber);

            if (selected == null) {
                return "The selected ID is too high.";
            }

            return "Successfully selected %s.".formatted(selected.getName());
        }
    }
    /**
     * Increments the value associated with the specified key in the given map.
     *
     * @param map The map to be modified. Must not be null.
     * @param key The key whose associated value is to be incremented. Must not be null.
     *
     * This method takes a map and a key as parameters, and increments the value associated
     * with the specified key in the map. If the key is not present in the map, a new entry
     * with the key and a value of 1 is added. If the map or key is null, the method does nothing.
     */
    public void incrementMap(final Map<String, Integer> map, final String key) {
        map.compute(key, (k, oldValue) -> (oldValue == null) ? 1 : oldValue + 1);
    }
    /**
     * Load string.
     *
     * @return the string
     */
    public String load() {
        if (!status) {
            return "%s is offline.".formatted(getUsername());
        }

        if (searchBar.getLastSelected() == null) {
            return "Please select a source before attempting to load.";
        }

        if (!searchBar.getLastSearchType().equals("song")
                && ((AudioCollection) searchBar.getLastSelected()).getNumberOfTracks() == 0) {
            return "You can't load an empty audio collection!";
        }
        currentSong = null;
        player.setSource(searchBar.getLastSelected(), searchBar.getLastSearchType());
        //----------------
        if (player.getType().equals("song")) {
            String album = ((Song) player.getSource().getAudioFile()).getAlbum();
            String genre = ((Song) player.getSource().getAudioFile()).getGenre();
            String artistName = ((Song) player.getSource().getAudioFile()).getArtist();
            String song = (player.getSource().getAudioFile()).getName();
            incrementMap(albumName, album);
            incrementMap(genreName, genre);
            incrementMap(artistNames, artistName);
            incrementMap(songName, song);
            Admin admin = Admin.getInstance();
            Artist artist = admin.getArtist(artistName);
            if (artist == null) {
                admin.addArtist(artistName);
                artist = admin.getArtist(artistName);
            }
            artist.incrementMap(artist.getBestAlbums(), album);
            artist.incrementMap(artist.getBestSongs(), song);
            artist.incrementMap(artist.getBestFans(), getUsername());
            artist.incrementMap(artist.getListeners(), getUsername());
            artist.incrementMap(artist.getCities(), getCity());
        }
        //------
        searchBar.clearSelection();

        player.pause();

        return "Playback loaded successfully.";
    }

    /**
     * Play pause string.
     *
     * @return the string
     */
    public String playPause() {
        if (!status) {
            return "%s is offline.".formatted(getUsername());
        }

        if (player.getCurrentAudioFile() == null) {
            return "Please load a source before attempting to pause or resume playback.";
        }

        player.pause();

        if (player.getPaused()) {
            return "Playback paused successfully.";
        } else {
            return "Playback resumed successfully.";
        }
    }

    /**
     * Repeat string.
     *
     * @return the string
     */
    public String repeat() {
        if (!status) {
            return "%s is offline.".formatted(getUsername());
        }

        if (player.getCurrentAudioFile() == null) {
            return "Please load a source before setting the repeat status.";
        }

        Enums.RepeatMode repeatMode = player.repeat();
        String repeatStatus = "";

        switch (repeatMode) {
            case NO_REPEAT -> {
                repeatStatus = "no repeat";
            }
            case REPEAT_ONCE -> {
                repeatStatus = "repeat once";
            }
            case REPEAT_ALL -> {
                repeatStatus = "repeat all";
            }
            case REPEAT_INFINITE -> {
                repeatStatus = "repeat infinite";
            }
            case REPEAT_CURRENT_SONG -> {
                repeatStatus = "repeat current song";
            }
            default -> {
                repeatStatus = "";
            }
        }

        return "Repeat mode changed to %s.".formatted(repeatStatus);
    }

    /**
     * Shuffle string.
     *
     * @param seed the seed
     * @return the string
     */
    public String shuffle(final Integer seed) {
        if (!status) {
            return "%s is offline.".formatted(getUsername());
        }

        if (player.getCurrentAudioFile() == null) {
            return "Please load a source before using the shuffle function.";
        }

        if (!player.getType().equals("playlist")
                && !player.getType().equals("album")) {
            return "The loaded source is not a playlist or an album.";
        }

        player.shuffle(seed);

        if (player.getShuffle()) {
            return "Shuffle function activated successfully.";
        }
        return "Shuffle function deactivated successfully.";
    }

    /**
     * Forward string.
     *
     * @return the string
     */
    public String forward() {
        if (!status) {
            return "%s is offline.".formatted(getUsername());
        }

        if (player.getCurrentAudioFile() == null) {
            return "Please load a source before attempting to forward.";
        }

        if (!player.getType().equals("podcast")) {
            return "The loaded source is not a podcast.";
        }

        player.skipNext();

        return "Skipped forward successfully.";
    }

    /**
     * Backward string.
     *
     * @return the string
     */
    public String backward() {
        if (!status) {
            return "%s is offline.".formatted(getUsername());
        }

        if (player.getCurrentAudioFile() == null) {
            return "Please select a source before rewinding.";
        }

        if (!player.getType().equals("podcast")) {
            return "The loaded source is not a podcast.";
        }

        player.skipPrev();

        return "Rewound successfully.";
    }

    /**
     * Like string.
     *
     * @return the string
     */
    public String like() {
        if (!status) {
            return "%s is offline.".formatted(getUsername());
        }

        if (player.getCurrentAudioFile() == null) {
            return "Please load a source before liking or unliking.";
        }

        if (!player.getType().equals("song") && !player.getType().equals("playlist")
                && !player.getType().equals("album")) {
            return "Loaded source is not a song.";
        }

        Song song = (Song) player.getCurrentAudioFile();

        if (likedSongs.contains(song)) {
            likedSongs.remove(song);
            song.dislike();

            return "Unlike registered successfully.";
        }

        likedSongs.add(song);
        song.like();
        return "Like registered successfully.";
    }

    /**
     * Next string.
     *
     * @return the string
     */
    public String next() {
        if (!status) {
            return "%s is offline.".formatted(getUsername());
        }

        if (player.getCurrentAudioFile() == null) {
            return "Please load a source before skipping to the next track.";
        }

        player.next();

        if (player.getCurrentAudioFile() == null) {
            return "Please load a source before skipping to the next track.";
        }

        return "Skipped to next track successfully. The current track is %s."
                .formatted(player.getCurrentAudioFile().getName());
    }

    /**
     * Prev string.
     *
     * @return the string
     */
    public String prev() {
        if (!status) {
            return "%s is offline.".formatted(getUsername());
        }

        if (player.getCurrentAudioFile() == null) {
            return "Please load a source before returning to the previous track.";
        }

        player.prev();

        return "Returned to previous track successfully. The current track is %s."
                .formatted(player.getCurrentAudioFile().getName());
    }

    /**
     * Create playlist string.
     *
     * @param name      the name
     * @param timestamp the timestamp
     * @return the string
     */
    public String createPlaylist(final String name, final int timestamp) {
        if (!status) {
            return "%s is offline.".formatted(getUsername());
        }

        if (playlists.stream().anyMatch(playlist -> playlist.getName().equals(name))) {
            return "A playlist with the same name already exists.";
        }

        playlists.add(new Playlist(name, getUsername(), timestamp));

        return "Playlist created successfully.";
    }

    /**
     * Add remove in playlist string.
     *
     * @param id the id
     * @return the string
     */
    public String addRemoveInPlaylist(final int id) {
        if (!status) {
            return "%s is offline.".formatted(getUsername());
        }

        if (player.getCurrentAudioFile() == null) {
            return "Please load a source before adding to or removing from the playlist.";
        }

        if (player.getType().equals("podcast")) {
            return "The loaded source is not a song.";
        }

        if (id > playlists.size()) {
            return "The specified playlist does not exist.";
        }

        Playlist playlist = playlists.get(id - 1);

        if (playlist.containsSong((Song) player.getCurrentAudioFile())) {
            playlist.removeSong((Song) player.getCurrentAudioFile());
            return "Successfully removed from playlist.";
        }

        playlist.addSong((Song) player.getCurrentAudioFile());
        return "Successfully added to playlist.";
    }

    /**
     * Switch playlist visibility string.
     *
     * @param playlistId the playlist id
     * @return the string
     */
    public String switchPlaylistVisibility(final Integer playlistId) {
        if (!status) {
            return "%s is offline.".formatted(getUsername());
        }

        if (playlistId > playlists.size()) {
            return "The specified playlist ID is too high.";
        }

        Playlist playlist = playlists.get(playlistId - 1);
        playlist.switchVisibility();

        if (playlist.getVisibility() == Enums.Visibility.PUBLIC) {
            return "Visibility status updated successfully to public.";
        }

        return "Visibility status updated successfully to private.";
    }

    /**
     * Show playlists array list.
     *
     * @return the array list
     */
    public ArrayList<PlaylistOutput> showPlaylists() {
        ArrayList<PlaylistOutput> playlistOutputs = new ArrayList<>();
        for (Playlist playlist : playlists) {
            playlistOutputs.add(new PlaylistOutput(playlist));
        }

        return playlistOutputs;
    }

    /**
     * Follow string.
     *
     * @return the string
     */
    public String follow() {
        if (!status) {
            return "%s is offline.".formatted(getUsername());
        }

        LibraryEntry selection = searchBar.getLastSelected();
        String type = searchBar.getLastSearchType();

        if (selection == null) {
            return "Please select a source before following or unfollowing.";
        }

        if (!type.equals("playlist")) {
            return "The selected source is not a playlist.";
        }

        Playlist playlist = (Playlist) selection;

        if (playlist.getOwner().equals(getUsername())) {
            return "You cannot follow or unfollow your own playlist.";
        }

        if (followedPlaylists.contains(playlist)) {
            followedPlaylists.remove(playlist);
            playlist.decreaseFollowers();

            return "Playlist unfollowed successfully.";
        }

        followedPlaylists.add(playlist);
        playlist.increaseFollowers();


        return "Playlist followed successfully.";
    }

    /**
     * Gets player stats.
     *
     * @return the player stats
     */
    public PlayerStats getPlayerStats() {
        return player.getStats();
    }

    /**
     * Show preferred songs array list.
     *
     * @return the array list
     */
    public ArrayList<String> showPreferredSongs() {
        ArrayList<String> results = new ArrayList<>();
        for (AudioFile audioFile : likedSongs) {
            results.add(audioFile.getName());
        }

        return results;
    }
    /**
     * Wraps statistical information into an ObjectNode based on the provided command input.
     *
     * @param command The command input to process. Must not be null.
     * @return An ObjectNode containing statistical information based on the provided command input,
     *         or {@code null} if required data is missing.
     *
     * This method generates an ObjectNode containing statistical information such as top artists,
     * genres, songs, and albums based on the provided command input. The information is derived
     * from the data stored in various maps (artistNames, genreName, songName, albumName).
     * The result is structured and categorized within the ObjectNode.
     * If any required data is missing,
     * such as empty maps, the method returns {@code null}.
     */
    public ObjectNode wrapped(final CommandInput command) {
        if (artistNames.isEmpty() || genreName.isEmpty() || songName.isEmpty()
                || albumName.isEmpty()) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();

        ObjectNode node = objectMapper.createObjectNode();
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(artistNames.entrySet());

        entryList.sort(Map.Entry.<String, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey()));

        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entryList) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        for (int i = 0; i < wrappedLimit; i++) {
            if (sortedMap.size() <= i) {
                break;
            }
            node.put(sortedMap.keySet().toArray()[i].toString(),
                    (Integer) sortedMap.values().toArray()[i]);
        }
        objectNode.set("topArtists", node);

        ObjectNode node1 = objectMapper.createObjectNode();

        List<Map.Entry<String, Integer>> entryList1 = new ArrayList<>(genreName.entrySet());

        entryList1.sort(Map.Entry.<String, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey()));

        Map<String, Integer> sortedMap1 = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entryList1) {
            sortedMap1.put(entry.getKey(), entry.getValue());
        }
        for (int i = 0; i < wrappedLimit; i++) {
            if (sortedMap1.size() <= i) {
                break;
            }
            node1.put(sortedMap1.keySet().toArray()[i].toString(),
                    (Integer) sortedMap1.values().toArray()[i]);
        }
        objectNode.set("topGenres", node1);

        ObjectNode node2 = objectMapper.createObjectNode();
        List<Map.Entry<String, Integer>> entryList2 = new ArrayList<>(songName.entrySet());

        entryList2.sort(Map.Entry.<String, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey()));

        Map<String, Integer> sortedMap2 = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entryList2) {
            sortedMap2.put(entry.getKey(), entry.getValue());
        }
        for (int i = 0; i < wrappedLimit; i++) {
            if (sortedMap2.size() <= i) {
                break;
            }
            node2.put(sortedMap2.keySet().toArray()[i].toString(),
                    (Integer) sortedMap2.values().toArray()[i]);
        }
        objectNode.set("topSongs", node2);

        ObjectNode node3 = objectMapper.createObjectNode();
        List<Map.Entry<String, Integer>> entryList3 = new ArrayList<>(albumName.entrySet());

        entryList3.sort(Map.Entry.<String, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey()));

        Map<String, Integer> sortedMap3 = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entryList3) {
            sortedMap3.put(entry.getKey(), entry.getValue());
        }
        for (int i = 0; i < wrappedLimit; i++) {
            if (sortedMap3.size() <= i) {
                break;
            }
            node3.put(sortedMap3.keySet().toArray()[i].toString(),
                    (Integer) sortedMap3.values().toArray()[i]);
        }
        objectNode.set("topAlbums", node3);

        ObjectNode node4 = objectMapper.createObjectNode();
        objectNode.set("topPodcasts", node4);
        return objectNode;
    }

    /**
     * Gets preferred genre.
     *
     * @return the preferred genre
     */
    public String getPreferredGenre() {
        String[] genres = {"pop", "rock", "rap"};
        int[] counts = new int[genres.length];
        int mostLikedIndex = -1;
        int mostLikedCount = 0;

        for (Song song : likedSongs) {
            for (int i = 0; i < genres.length; i++) {
                if (song.getGenre().equals(genres[i])) {
                    counts[i]++;
                    if (counts[i] > mostLikedCount) {
                        mostLikedCount = counts[i];
                        mostLikedIndex = i;
                    }
                    break;
                }
            }
        }

        String preferredGenre = mostLikedIndex != -1 ? genres[mostLikedIndex] : "unknown";
        return "This user's preferred genre is %s.".formatted(preferredGenre);
    }

    /**
     * Switch status.
     */
    public void switchStatus() {
        status = !status;
    }

    /**
     * Simulate time.
     *
     * @param time the time
     */
    public void simulateTime(final int time) {
        if (!status) {
            return;
        }

        player.simulatePlayer(time);
        if (player.getSource() != null && player.getType().equals("album")) {
            Admin admin = Admin.getInstance();
            boolean exit = false;
            Artist artist = admin.getArtist(player.getSource().getAudioCollection().getOwner());
            if (currentSong != null) {
                boolean isSongCurrentSong = false;
                for (Song name : ((Album) player.getSource().getAudioCollection()).getSongs()) {
                    if (isSongCurrentSong) {
                        incrementMap(albumName, name.getAlbum());
                        incrementMap(genreName, name.getGenre());
                        incrementMap(artistNames, name.getArtist());
                        incrementMap(songName, name.getName());
                        if (artist == null) {
                            admin.addArtist(name.getArtist());
                            artist = admin.getArtist(name.getArtist());
                        }
                        artist.incrementMap(artist.getBestAlbums(), name.getAlbum());
                        artist.incrementMap(artist.getBestSongs(), name.getName());
                        artist.incrementMap(artist.getBestFans(), getUsername());
                        artist.incrementMap(artist.getListeners(), getUsername());
                        artist.incrementMap(artist.getCities(), getCity());
                    }
                    if (name.getName().equals(currentSong)) {
                        isSongCurrentSong = true;
                    }
                    if (name.getName().equals(player.getSource().getAudioFile().getName())) {
                        exit = true;
                        break;
                    }
                }
            } else {
                for (Song name : ((Album) player.getSource().getAudioCollection()).getSongs()) {
                    incrementMap(albumName, name.getAlbum());
                    incrementMap(genreName, name.getGenre());
                    incrementMap(artistNames, name.getArtist());
                    incrementMap(songName, name.getName());
                    if (artist == null) {
                        admin.addArtist(name.getArtist());
                        artist = admin.getArtist(name.getArtist());
                    }
                    artist.incrementMap(artist.getBestAlbums(), name.getAlbum());
                    artist.incrementMap(artist.getBestSongs(), name.getName());
                    artist.incrementMap(artist.getBestFans(), getUsername());
                    artist.incrementMap(artist.getListeners(), getUsername());
                    artist.incrementMap(artist.getCities(), getCity());
                    if (name.getName().equals(player.getSource().getAudioFile().getName())) {
                        exit = true;
                        break;
                    }
                }
            }
            if (exit) {
                currentSong = player.getSource().getAudioFile().getName();
            } else {
                currentSong = null;
            }
        }
    }
}
