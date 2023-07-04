package app.chrisw.mc;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChunkPruner extends JavaPlugin {

    public void recursiveProcess(List<String> paths, File file) {
        String absolutePath = file.getAbsolutePath();
        if (file.isDirectory()) {
            for (File listFile : file.listFiles()) {
                recursiveProcess(paths, listFile);
            }
        } else {
            if (absolutePath.endsWith("map.xml")) {
                paths.add(absolutePath.substring(0, absolutePath.length() - 7).replace("/./", "/"));
            }
        }
    }

    @Override
    public void onEnable() {
        Logger logger = Logger.getLogger("ChunkPruner");

        String basePath = "maps";

        ArrayList<String> filePaths = new ArrayList<>();
        File baseFile = Paths.get(Bukkit.getWorldContainer().getAbsolutePath(), basePath).toFile();
        String absolutePath = baseFile.getParentFile().getAbsolutePath();

        recursiveProcess(filePaths, baseFile);

        int numProcessed = 0;

        for (String filePath : filePaths) {
            numProcessed += 1;
            String worldName = filePath.substring(absolutePath.length() - 1, filePath.length() - 1);
            logger.info("Processing: " + worldName + ", " + numProcessed + "/" + filePaths.size());

            WorldCreator worldCreator = new WorldCreator(worldName);

            if (Integer.valueOf(Bukkit.getServer().getClass().getPackage().getName().split("_")[1]) > 12 ) {
                worldCreator.generator(new PruneChunkGenerator1_13());
            } else {
                worldCreator.generator(PruneChunkGenerator.INSTANCE);
            }

            World world = worldCreator.createWorld();

            world.setAutoSave(false);

            File[] regionFiles = (new File(filePath + "/region")).listFiles();

            List<String> filesToDelete = new ArrayList<>();
            filesToDelete.add(filePath + "/data");
            filesToDelete.add(filePath + "/playerdata");
            filesToDelete.add(filePath + "/session.lock");
            filesToDelete.add(filePath + "/uid.dat");

            // This is pruning
            if (regionFiles != null) {
                for (File regionFile : regionFiles) {
                    Matcher matcher = Pattern.compile("r\\.(-?\\d+).(-?\\d+).mca").matcher(regionFile.getName());
                    if (!matcher.matches()) continue;

                    int regionX = Integer.parseInt(matcher.group(1));
                    int regionZ = Integer.parseInt(matcher.group(2));
                    int minX = regionX << 5;
                    int minZ = regionZ << 5;
                    int maxX = minX + 32;
                    int maxZ = minZ + 32;
                    boolean empty = true;

                    for (int x = minX; x < maxX; x++) {
                        for (int z = minZ; z < maxZ; z++) {
                            Chunk chunk = world.getChunkAt(x, z);
                            ChunkSnapshot chunkSnapshot = chunk.getChunkSnapshot();
                            for (int chunkX = 0; chunkX < 16; chunkX++) {
                                for (int chunkZ = 0; chunkZ < 16; chunkZ++) {
                                    int highestBlockYAt = chunkSnapshot.getHighestBlockYAt(chunkX, chunkZ);
                                    if (highestBlockYAt > 0) {
                                        empty = false;
                                        break;
                                    } else {
                                        boolean b;
                                        try {
                                            b = chunkSnapshot.getBlockType(chunkX, 0, chunkZ) != Material.AIR;
                                        } catch (NoSuchMethodError e) {
                                            try {
                                                Method getBlockTypeId = ChunkSnapshot.class.getMethod("getBlockTypeId", int.class, int.class, int.class);
                                                b = 0 != ((int)getBlockTypeId.invoke(chunkSnapshot,chunkX, 0, chunkZ));
                                            } catch (NoSuchMethodException | InvocationTargetException |
                                                     IllegalAccessException ex) {
                                                throw new RuntimeException(ex);
                                            }
                                        }
                                        if (b) {
                                            empty = false;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (empty) {
                        filesToDelete.add(regionFile.getAbsolutePath());
                    }
                }
            }
            Bukkit.unloadWorld(world, true);

            for (String file : filesToDelete) {
                File fileToDelete = new File(file);
                fileToDelete.delete();
            }

        }
    }
}
