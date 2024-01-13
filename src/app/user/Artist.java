package app.user;



import app.audio.Collections.Album;
import app.audio.Collections.AlbumOutput;
import app.audio.Files.Song;
import app.pages.ArtistPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
 * The type Artist.
 */
public final class Artist extends ContentCreator {
    private ArrayList<Album> albums;
    private ArrayList<Merchandise> merch;
    private ArrayList<Event> events;
    private final int wrappedLimit = 5;
    @Getter
    @Setter
    private Map<String, Integer> bestAlbums = new HashMap<>();
    @Getter
    @Setter
    private Map<String, Integer> bestSongs = new HashMap<>();
    @Getter
    @Setter
    private Map<String, Integer> bestFans = new HashMap<>();
    @Getter
    @Setter
    private Map<String, Integer> listeners = new HashMap<>();
    @Getter
    @Setter
    private Map<String, Integer> cities = new HashMap<>();

    /**
     * Instantiates a new Artist.
     *
     * @param username the username
     * @param age      the age
     * @param city     the city
     */
    public Artist(final String username, final int age, final String city) {
        super(username, age, city);
        albums = new ArrayList<>();
        merch = new ArrayList<>();
        events = new ArrayList<>();

        super.setPage(new ArtistPage(this));
    }

    /**
     * Gets albums.
     *
     * @return the albums
     */
    public ArrayList<Album> getAlbums() {
        return albums;
    }

    /**
     * Gets merch.
     *
     * @return the merch
     */
    public ArrayList<Merchandise> getMerch() {
        return merch;
    }

    /**
     * Gets events.
     *
     * @return the events
     */
    public ArrayList<Event> getEvents() {
        return events;
    }

    /**
     * Gets event.
     *
     * @param eventName the event name
     * @return the event
     */
    public Event getEvent(final String eventName) {
        for (Event event : events) {
            if (event.getName().equals(eventName)) {
                return event;
            }
        }

        return null;
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
     * Gets album.
     *
     * @param albumName the album name
     * @return the album
     */
    public Album getAlbum(final String albumName) {
        for (Album album : albums) {
            if (album.getName().equals(albumName)) {
                return album;
            }
        }

        return null;
    }

    /**
     * Gets all songs.
     *
     * @return the all songs
     */
    public List<Song> getAllSongs() {
        List<Song> songs = new ArrayList<>();
        albums.forEach(album -> songs.addAll(album.getSongs()));

        return songs;
    }

    /**
     * Show albums array list.
     *
     * @return the array list
     */
    public ArrayList<AlbumOutput> showAlbums() {
        ArrayList<AlbumOutput> albumOutput = new ArrayList<>();
        for (Album album : albums) {
            albumOutput.add(new AlbumOutput(album));
        }

        return albumOutput;
    }
    /**
     * Wraps statistical information into an ObjectNode based on the provided command input.
     *
     * @param command The command input to process. Must not be null.
     * @return An ObjectNode containing statistical information based on the provided command input.
     *
     * This method generates an ObjectNode containing statistical information, such as top albums,
     * top songs, top fans, listener count, and city count, based on the provided command input.
     * The information is derived from the data stored in
     * various maps and sets (bestAlbums, bestSongs,
     * bestFans, listeners, cities). The result is structured and categorized within the ObjectNode.
     * If any required data is missing or the command input is null,
     * the method returns an empty ObjectNode.
     */
    public ObjectNode wrapped(final CommandInput command) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();

        ObjectNode node = objectMapper.createObjectNode();
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(bestAlbums.entrySet());

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
        objectNode.set("topAlbums", node);

        node = objectMapper.createObjectNode();
        entryList = new ArrayList<>(bestSongs.entrySet());

        entryList.sort(Map.Entry.<String, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey()));

        sortedMap = new LinkedHashMap<>();
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
        objectNode.set("topSongs", node);

        ArrayNode arrNode = objectMapper.createArrayNode();
        entryList = new ArrayList<>(bestFans.entrySet());

        entryList.sort(Map.Entry.<String, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey()));

        sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entryList) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        for (int i = 0; i < wrappedLimit; i++) {
            if (sortedMap.size() <= i) {
                break;
            }
            arrNode.add(sortedMap.keySet().toArray()[i].toString());
        }

        objectNode.set("topFans", arrNode);


        objectNode.put("listeners", listeners.size());
        objectNode.put("cities", cities.size());


        return objectNode;
    }

    /**
     * Get user type
     *
     * @return user type string
     */
    public String userType() {
        return "artist";
    }
}
