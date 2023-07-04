package app.chrisw.mc;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
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
            worldCreator.generator(PruneChunkGenerator.INSTANCE);

            World world = worldCreator.createWorld();

            world.setAutoSave(false);

            File[] regionFiles = (new File(filePath + "/region")).listFiles();

            List<String> filesToDelete = new ArrayList<>();
            filesToDelete.add(filePath + "/data");
            filesToDelete.add(filePath + "/playerdata");
            filesToDelete.add(filePath + "/session.lock");
            filesToDelete.add(filePath + "/uid.dat");

//            logger.info(Paths.get(filePath).toFile().getAbsolutePath());

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
                            Chunk chunk = world.getWorld().getChunkAt(x, z);
                            if (!chunk.isEmpty()) {
                                empty = false;
                            }
                        }
                    }
                    if (empty) {
//                        logger.info("Deleting: " + regionFile.getAbsolutePath());
                        filesToDelete.add(regionFile.getAbsolutePath());
                    } else {
//                        logger.info("Keeping: " + regionFile.getAbsolutePath());
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
