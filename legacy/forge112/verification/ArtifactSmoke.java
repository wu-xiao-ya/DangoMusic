import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import me.sorihiko.dango.music.AudioDecoder;
import me.sorihiko.dango.shadow.kotlin.Pair;
import me.sorihiko.dango.update.UpdateChecker;

/** Runs against the reobfuscated, shaded distribution on Java 8, outside Gradle's classpath. */
public class ArtifactSmoke {
    public static void main(String[] args) throws Exception {
        for (String file : args) {
            byte[] bytes = Files.readAllBytes(Paths.get(file));
            try (AudioDecoder decoder = AudioDecoder.Companion.open(bytes)) {
                int samples = 0;
                int peak = 0;
                AudioDecoder.Frame frame;
                while ((frame = decoder.read()) != null) {
                    require(frame.getRate() == 44100 && frame.getChannels() == 1, file + ": incorrect PCM format");
                    for (int i = 0; i < frame.getLength(); i++) {
                        peak = Math.max(peak, Math.abs((int) frame.getSamples()[i]));
                    }
                    samples += frame.getLength();
                }
                require(samples >= 43000 && samples <= 47500 && peak > 500, file + ": missing or silent PCM");
            }
            AudioDecoder.Started seek = AudioDecoder.Companion.start(bytes, 500, () -> true);
            try (AudioDecoder decoder = seek.getDecoder()) {
                require(seek.getSkippedMs() >= 500 && seek.getFirst() != null, file + ": seek failed");
            }
            System.out.println("Decoded and sought: " + file);
        }

        String current = UpdateChecker.INSTANCE.getCurrentVersion();
        String target = UpdateChecker.INSTANCE.getTarget();
        require("1.12.2".equals(target) && !current.isEmpty(), "Missing build metadata");
        String next = "999.0-port.2";
        String page = "https://github.com/wu-xiao-ya/DangoMusic/releases/tag/v" + next;
        String name = "DangoMusic-" + next + "+" + target + ".jar";
        UpdateChecker.Release missing = new UpdateChecker.Release(next, page, Collections.emptyList());
        require(UpdateChecker.infoFor(missing, current, target) == null, "Suggested release lacks a matching JAR");
        String download = "https://github.com/wu-xiao-ya/DangoMusic/releases/download/v" + next + "/" + name;
        UpdateChecker.Release release = new UpdateChecker.Release(next, page,
                Collections.singletonList(new Pair<>(name, download)));
        require(download.equals(UpdateChecker.infoFor(release, current, target).getDownload()), "Wrong update asset");
        require(UpdateChecker.infoFor(release, next, target) == null, "Suggested an already installed release");
        System.out.println("Verified release metadata and asset selection");
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
