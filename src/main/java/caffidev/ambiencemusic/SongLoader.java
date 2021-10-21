package caffidev.ambiencemusic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.logging.log4j.Level;

import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.FMLLog;

public final class SongLoader {

	public static File mainDir;
	public static boolean enabled = false;
	private static List<String> loggedErrorSongs = new LinkedList<>();

	public static void loadFrom(File f) {
		File config = new File(f, "ambience.properties");
		if(!config.exists())
			initConfig(config); 
		
		Properties props = new Properties();
		try {
			props.load(new FileReader(config));
			enabled = props.getProperty("enabled").equals("true");
			
			if(enabled) {
				SongPicker.reset();
				Set<Object> keys = props.keySet();
				for(Object obj : keys) {
					String s = (String) obj;
					
					String[] tokens = s.split("\\.");
					if(tokens.length < 2)
						continue;

					String keyType = tokens[0];
					if(keyType.equals("event")) {	
						String event = tokens[1];
						
						SongPicker.eventMap.put(event, props.getProperty(s).split(","));
					} else if(keyType.equals("biome")) {
						String biomeName = joinTokensExceptFirst(tokens).replaceAll("\\+", " ");
						Biome biome = BiomeMapper.getBiome(biomeName);
						
						if(biome != null)
							SongPicker.biomeMap.put(biome, props.getProperty(s).split(","));
					} else if(keyType.matches("primarytag|secondarytag")) {
						boolean primary = keyType.equals("primarytag");
						String tagName = tokens[1].toUpperCase();
						BiomeDictionary.Type type = BiomeMapper.getBiomeType(tagName);
						
						if(type != null) {
							if(primary)
								SongPicker.primaryTagMap.put(type, props.getProperty(s).split(","));
							else SongPicker.secondaryTagMap.put(type, props.getProperty(s).split(","));
						}
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		File musicDir = new File(f, "music");
		if(!musicDir.exists())
			musicDir.mkdir();
			
		mainDir = musicDir;
	}
	
	public static void initConfig(File f) {
		try {
			f.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(f));
			writer.write("# Ambience Music (modified by caffidev) Config\n");
			writer.write("enabled=false");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static InputStream getStream() {
		String currentSong = PlayerThread.currentSong;
		//Returns null
		if(currentSong == null || currentSong.equals("null") || loggedErrorSongs.contains(currentSong))
			return null;

		//Trying to get a file, if there is no file -> catch
		File f = new File(mainDir, PlayerThread.currentSong + ".mp3");
		//Debug thing
		if(f.getName().equals("null.mp3"))
			return null;
		
		try {
			return new FileInputStream(f);
		}
		//Constant logging leads to big log sizes
		//We need to be sure that logging of a certain file happens only once.
		catch (FileNotFoundException e) {
			FMLLog.log(Level.ERROR, "File " + f + " not found. Ambience music is not set up correctly.");
			//This song is banned now.
			loggedErrorSongs.add(currentSong);
			return null;
		}
	}
	
	private static String joinTokensExceptFirst(String[] tokens) {
		String s = "";
		int i = 0;
		for(String token : tokens) {
			i++;
			if(i == 1)
				continue;
			s += token;
		}
		return s;
	}
}
