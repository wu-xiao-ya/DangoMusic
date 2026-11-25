import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import me.sorihiko.dango.DangoMusic;
import me.sorihiko.dango.music.AudioOutput;
import me.sorihiko.dango.music.MusicPlayer;
import me.sorihiko.dango.music.NeteaseTrack;
import me.sorihiko.dango.platform.Platform;

/** Exercises the shipped player and decoder with an audio device whose open can be delayed. */
public final class SeekResumeSmoke {
    private static void await(CountDownLatch latch, String message) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(message, e);
        }
    }

    private static void seedPlayerField(String name, Object value) throws Exception {
        Field field = MusicPlayer.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static final class Output implements AudioOutput {
        private final CountDownLatch wrotePcm = new CountDownLatch(1);
        private final CountDownLatch finishWrite = new CountDownLatch(1);
        private volatile boolean running;

        public void start() { running = true; }
        public void stop() { running = false; }
        public boolean isRunning() { return running; }
        public boolean getLost() { return false; }
        public long getPositionMicros() { return 0; }
        public void setVolume(float volume) {}
        public void flush() {}
        public void drain() {}
        public void close() { finishWrite.countDown(); }

        public void write(byte[] data, int offset, int length) {
            if (running && length > 0) wrotePcm.countDown();
            await(finishWrite, "Timed out holding the first resumed PCM buffer");
        }
    }

    public static void main(String[] args) throws Exception {
        Path config = Files.createTempDirectory("dango-seek-resume");
        CountDownLatch opening = new CountDownLatch(1);
        CountDownLatch allowOpen = new CountDownLatch(1);
        Output output = new Output();
        Platform platform = (Platform) Proxy.newProxyInstance(Platform.class.getClassLoader(),
                new Class<?>[]{Platform.class}, (proxy, method, values) -> {
                    switch (method.getName()) {
                        case "getConfigDir": return config;
                        case "getHasGameAudio": return true;
                        case "openAudioOutput":
                            opening.countDown();
                            await(allowOpen, "Timed out opening the game audio output");
                            return output;
                        default: throw new AssertionError("Unexpected platform call " + method.getName());
                    }
                });
        DangoMusic.start(platform, Collections.emptyList());
        MusicPlayer player = MusicPlayer.INSTANCE;
        player.setOutputMode(MusicPlayer.OUTPUT_GAME);
        // Seed the already downloaded track so this test never needs a music provider or account.
        seedPlayerField("audio", Files.readAllBytes(Paths.get(args[0])));
        seedPlayerField("current", new NeteaseTrack(1, "fixture", Collections.emptyList(), 1000, ""));
        seedPlayerField("state", MusicPlayer.State.PLAYING);
        try {
            player.togglePause();
            player.seek(400);
            await(opening, "Seek failed to reach the game audio output");
            if (player.getState() != MusicPlayer.State.PAUSED || player.getPositionMs() != 400)
                throw new AssertionError("Paused seek did not retain its requested position");

            player.togglePause();
            if (player.getState() != MusicPlayer.State.PLAYING)
                throw new AssertionError("Resume during seek left the player in " + player.getState());
            allowOpen.countDown();
            await(output.wrotePcm, "Resumed decoder failed to deliver PCM to a running output");
            System.out.println("Resumed a paused seek during device open and delivered PCM");
        } finally {
            player.stop();
            allowOpen.countDown();
            output.finishWrite.countDown();
        }
    }
}
