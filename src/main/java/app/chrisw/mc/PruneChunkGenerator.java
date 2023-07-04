package app.chrisw.mc;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

public class PruneChunkGenerator extends ChunkGenerator {
    public static final PruneChunkGenerator INSTANCE = new PruneChunkGenerator();
    private static final byte[] CHUNK = new byte[0];

    private PruneChunkGenerator() {
    }

    public byte[] generate(final World world, final Random random, final int x, final int z) {
        return CHUNK;
    }
}
