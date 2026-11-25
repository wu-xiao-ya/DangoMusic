"""Check the actual shaded, reobfuscated legacy jar before uploading it."""

import json
import sys
from pathlib import Path
from zipfile import ZipFile


def properties(data):
    lines = data.decode("utf-8").splitlines()
    return dict(line.partition("=")[::2] for line in lines if "=" in line and not line.startswith("#"))


def check(ok, message):
    if not ok:
        raise ValueError(message)


def verify(target, path):
    root = Path(__file__).resolve().parents[2]
    port = target == "1.12.2"
    version_file = root / ("legacy/forge112/gradle.properties" if port else "version.properties")
    version = properties(version_file.read_bytes())["portVersion" if port else "version"]
    check(path.name == f"DangoMusic-{version}+{target}.jar", "unexpected artifact filename")
    with ZipFile(path) as jar:
        check(jar.testzip() is None, "invalid jar checksum")
        names = set(jar.namelist())
        lang = ["en_us", "zh_cn"] if port else ["en_US", "zh_CN"]
        base = "me/sorihiko/dango/"
        required = {
            base + "forge/DangoMusicMod.class", base + "forge/PartyNetwork.class",
            base + "party/ListenParty.class", base + "music/MusicPlayer.class",
            base + "shadow/kotlin/jvm/internal/Intrinsics.class",
            base + "shadow/javazoom/jl/decoder/Decoder.class",
            base + "shadow/jcraft/jorbis/DspState.class", base + "shadow/jcraft/jogg/SyncState.class",
            base + "shadow/zxing/MultiFormatWriter.class",
            "assets/dangomusic/fonts/Inter-Regular-cut.ttf",
            "assets/dangomusic/shaders/ui/shape.frag", "assets/dangomusic/shaders/ui/glass.frag",
            "assets/dangomusic/lang/zh_cn.json", "assets/dangomusic/system-media.ps1", "logo.png",
            *("assets/dangomusic/lang/" + locale + ".lang" for locale in lang),
            *("META-INF/licenses/" + license for license in (
                "Inter-OFL.txt", "JLayer-LGPL-2.1.txt", "JOrbis-LGPL-2.1.txt", "ZXing-Apache-2.0.txt",
            )),
        }
        if port:
            required.update(("pack.mcmeta", "META-INF/licenses/LICENSE"))
        check(required <= names, f"missing entries: {sorted(required - names)}")
        meta, = json.loads(jar.read("mcmod.info"))
        check(meta["modid"] == "dangomusic" and meta["version"] == version, "incorrect Forge metadata")
        check(meta["mcversion"].split("-")[0] == target, "incorrect Minecraft version")
        build = properties(jar.read("dangomusic-build.properties"))
        check(build["version"] == version and build["target"] == target, "incorrect update metadata")
        if port:
            check(build["updateRepository"] == "wu-xiao-ya/DangoMusic", "incorrect update repository")
        check(properties(jar.read("version.properties"))["dangomusic.version"] == version, "wrong FML version")
        check(f"[{target}]".encode() in jar.read(base + "forge/DangoMusicMod.class"), "wrong @Mod target")
        # Both legacy adapters call Minecraft.getMinecraft(). Production Forge expects its SRG name.
        check(any(b"func_71410_x" in jar.read(n) for n in names if n.startswith(base + "legacy/")
                  and n.endswith(".class")), "SRG Minecraft methods missing")
        for name in names:
            if name.endswith(".class"):
                data = jar.read(name)
                check(int.from_bytes(data[6:8], "big") <= 52, f"Java 8 cannot load {name}")
                check(not name.startswith(("kotlin/", "javazoom/", "com/jcraft/", "com/google/zxing/",
                                           "net/minecraft/", "net/minecraftforge/")), f"unexpected class: {name}")
    print(f"Verified {path.name}")


if __name__ == "__main__":
    target, jar_path = sys.argv[1:]
    verify(target, Path(jar_path))
