<p align="center"><img src="docs/logo.png" alt="Dango Music" width="220"></p>

<h1 align="center">Dango Music</h1>

## Forge 移植版 v1.7.6-port.2

本分支由 wu-xiao-ya 维护，基于 Sorihiko 的 Dango Music `v1.7.6`，采用 GPL-3.0 许可证。Minecraft 1.12.2 移植沿用 kochegarr 的适配工作。[上游 v1.7.6](https://github.com/Sorihiko/DangoMusic/releases/tag/v1.7.6) · [原始适配 PR #32](https://github.com/Sorihiko/DangoMusic/pull/32)

| Minecraft | 已实测的 Forge 版本 | 客户端下载（Java 8） |
| --- | --- | --- |
| 1.7.10 | 10.13.4.1558、10.13.4.1614 | [DangoMusic-1.7.6-port.2+1.7.10.jar](https://github.com/wu-xiao-ya/DangoMusic/releases/download/v1.7.6-port.2/DangoMusic-1.7.6-port.2%2B1.7.10.jar) |
| 1.12.2 | 14.23.5.2860 | [DangoMusic-1.7.6-port.2+1.12.2.jar](https://github.com/wu-xiao-ya/DangoMusic/releases/download/v1.7.6-port.2/DangoMusic-1.7.6-port.2%2B1.12.2.jar) |

下载与游戏版本对应的 JAR，放入客户端实例的 `mods` 目录。1.12.2 用户从 `port.1` 升级时请替换旧 JAR。用 Java 8 启动后，在主菜单或存档内按 `M` 打开播放器。

本次新增 1.7.10 支持，更新了 1.12.2 移植包，并修复暂停后拖动进度、立即恢复播放时偶尔仍停在暂停状态的问题。[移植版发布说明](https://github.com/wu-xiao-ya/DangoMusic/releases/tag/v1.7.6-port.2) · [验证记录](https://github.com/wu-xiao-ya/DangoMusic/releases/download/v1.7.6-port.2/forge-port.2-verification.md) · [SHA-256 校验值](https://github.com/wu-xiao-ya/DangoMusic/releases/download/v1.7.6-port.2/SHA256SUMS.txt)

- **一起听**。同一局域网存档内，房主和参加者安装模组后即可直连。跨存档或跨游戏版本时，每位参加者可将仓库中的 `firebase.properties.example` 复制到各自实例的 `config/dangomusic/firebase.properties`，填入同一项目的 `apiKey` 和 `databaseUrl`，再重启游戏。Firebase 项目需要开启匿名登录、Realtime Database 并应用仓库中的数据库规则。普通专服玩家可以只在客户端安装。
- **从源码构建**。构建 1.7.10 时安装 JDK 17 和 JDK 8，用 JDK 17 在 `legacy/forge1710` 启动 Gradle。构建 1.12.2 时用 JDK 8 在 `legacy/forge112` 启动 Gradle。命令均为 `gradlew.bat clean build`（Linux/macOS 使用 `./gradlew clean build`），在对应目录的 `build/libs` 获取带游戏版本后缀的成品。

<p align="center"><em>A Minecraft mod that brings NetEase Cloud Music, QQ Music and Kugou into the game: liquid-glass player, 3D in-world lyrics, song card HUD, Dynamic Island and Listen Together.</em></p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/github/license/Sorihiko/DangoMusic?style=for-the-badge&label=license&labelColor=555555&color=FF5C8A" alt="License"></a>
  <a href="https://github.com/Sorihiko/DangoMusic/stargazers"><img src="https://img.shields.io/github/stars/Sorihiko/DangoMusic?style=for-the-badge&label=stars&labelColor=555555&color=FF5C8A" alt="Stars"></a>
  <a href="https://github.com/wu-xiao-ya/DangoMusic/releases"><img src="https://img.shields.io/github/v/release/wu-xiao-ya/DangoMusic?style=for-the-badge&label=Forge%20port&labelColor=555555&color=FF5C8A" alt="Forge 移植版版本"></a>
</p>

在 Minecraft 里听网易云音乐 / QQ 音乐 / 酷狗的 mod：液态玻璃播放器、漂浮在世界里的 3D 歌词、Song card、灵动岛，还能和朋友一起听歌。

<p align="center"><img src="docs/screenshots/lyrics-3d.jpg" alt="3D 歌词" width="100%"></p>

| | | |
|:---:|:---:|:---:|
| <img src="docs/screenshots/home.jpg" alt="首页"> | <img src="docs/screenshots/playlist.jpg" alt="歌单"> | <img src="docs/screenshots/coverflow.jpg" alt="封面墙"> |

## 功能

- **播放器**（默认按 `M`，主菜单也能打开）：推荐、排行榜、我的歌单、搜索，列表 / 封面墙两种视图
- **三个音源**：网易云 / QQ 音乐 / 酷狗，都在游戏内扫码登录（会员歌曲需要对应平台的会员）
- **歌词**：逐字歌词，3D 歌词漂浮在世界里，带发光、抖动、文闪等效果
- **歌曲卡片与灵动岛**可在游戏内显示当前歌曲。Forge 移植版从 `1.7.6-port.3` 起支持双行灵动岛。按 `M` 打开播放器，在「设置 → 灵动岛」中开启「显示正在播放」和「同时显示玩家状态」，即可在同一个胶囊里看到上方的玩家名、FPS、时间和下方的歌曲。
- **歌手页**：点歌手名看热门歌曲、专辑和简介
- **一起听歌**：分享房间码，所有人同步到同一首歌、同一秒；局域网 / 内网穿透联机时直接经房主的游戏同步，不用 Firebase、不用代理；不在同一个服务器、不是同一个 MC 版本也能经 Firebase 一起听
- 设置都在播放器侧栏的 ⚙ 里，可以绑定快捷键

## 上游版本

Sorihiko 的 `v1.7.6` 为下列游戏版本提供 JAR。按游戏版本和加载器选择文件，使用 Fabric 时另需安装对应版本的 Fabric API。[上游 v1.7.6 下载](https://github.com/Sorihiko/DangoMusic/releases/tag/v1.7.6) · [上游更新日志](docs/CHANGELOG-1.7.6.md) · [Fabric API](https://modrinth.com/mod/fabric-api)

| Minecraft | 加载器 |
| --- | --- |
| 1.8.9 | Forge 11.15.1.2318 |
| 1.20.1 | Fabric、Forge 47.x |
| 1.21.1、1.21.10、1.21.11 | Fabric、NeoForge |
| 26.1、26.1.1、26.1.2 | Fabric、NeoForge（这三个版本共用文件名含 `26.1.2` 的对应加载器 JAR） |
| 26.2、26.3 | Fabric、NeoForge |

26.2 和 26.3 支持 Vulkan，3D 歌词会显示在手部画面上方。配置和登录信息保存在客户端实例的 `config/dangomusic/` 目录。

## 构建全部版本

需要 JDK 8、17、21、25，运行 `build-all.bat`，jar 收集到 `dist/`（JDK 不在默认位置时设置对应的 `DANGO_JDK17`、`DANGO_JDK21` 或 `DANGO_JDK25`）。

<details>
<summary>启用一起听歌（Firebase）</summary>

1. 在 [Firebase 控制台](https://console.firebase.google.com/) 新建项目，开启 **匿名登录**，创建 **Realtime Database**
2. 把 `firebase.properties.example` 复制成 `common/core/src/main/resources/firebase.properties`，填上 `apiKey` 和 `databaseUrl`
3. 数据库规则（也在 [database.rules.json](database.rules.json)，限制了每个字段的类型和长度）：

```json
{
  "rules": {
    "rooms": {
      "$code": {
        ".read": "auth != null",
        ".write": "auth != null && (!data.exists() || data.child('host').val() === auth.uid || data.child('members').child(data.child('host').val()).child('lastSeen').val() < now - 600000)",
        ".validate": "$code.length <= 16 && newData.hasChildren(['host']) && (newData.child('host').val() === auth.uid || newData.child('host').val() === data.child('host').val())",
        "host": { ".validate": "newData.isString() && newData.val().length <= 128" },
        "hostName": { ".validate": "newData.isString() && newData.val().length <= 64" },
        "protocol": { ".validate": "newData.isNumber()" },
        "memberControl": { ".validate": "newData.isBoolean()" },
        "createdAt": { ".validate": "newData.isNumber()" },
        "members": {
          "$uid": {
            ".write": "auth != null && auth.uid === $uid && root.child('rooms').child($code).exists()",
            ".validate": "$uid.length <= 128 && newData.hasChildren(['lastSeen'])",
            "name": { ".validate": "newData.isString() && newData.val().length <= 64" },
            "failedTrack": { ".validate": "newData.isString() && newData.val().length <= 256" },
            "lastSeen": { ".validate": "newData.isNumber()" },
            "$other": { ".validate": false }
          }
        },
        "state": {
          "trackId": { ".validate": "newData.isNumber()" },
          "source": { ".validate": "newData.isString() && newData.val().length <= 16" },
          "nativeId": { ".validate": "newData.isString() && newData.val().length <= 64" },
          "mediaId": { ".validate": "newData.isString() && newData.val().length <= 64" },
          "albumId": { ".validate": "newData.isString() && newData.val().length <= 64" },
          "name": { ".validate": "newData.isString() && newData.val().length <= 1024" },
          "artists": { ".validate": "newData.isString() && newData.val().length <= 2048" },
          "cover": { ".validate": "newData.isString() && newData.val().length <= 4096" },
          "duration": { ".validate": "newData.isNumber()" },
          "pos": { ".validate": "newData.isNumber()" },
          "playing": { ".validate": "newData.isBoolean()" },
          "at": { ".validate": "newData.isNumber()" },
          "$other": { ".validate": false }
        },
        "req": {
          ".write": "auth != null && data.parent().child('members').child(auth.uid).exists() && data.parent().child('memberControl').val() === true",
          "id": { ".validate": "newData.isString() && newData.val().length <= 64" },
          "by": { ".validate": "newData.isString() && newData.val() === auth.uid" },
          "byName": { ".validate": "newData.isString() && newData.val().length <= 64" },
          "op": { ".validate": "newData.isString() && newData.val().length <= 16" },
          "trackId": { ".validate": "newData.isNumber()" },
          "source": { ".validate": "newData.isString() && newData.val().length <= 16" },
          "nativeId": { ".validate": "newData.isString() && newData.val().length <= 64" },
          "mediaId": { ".validate": "newData.isString() && newData.val().length <= 64" },
          "albumId": { ".validate": "newData.isString() && newData.val().length <= 64" },
          "name": { ".validate": "newData.isString() && newData.val().length <= 1024" },
          "artists": { ".validate": "newData.isString() && newData.val().length <= 2048" },
          "cover": { ".validate": "newData.isString() && newData.val().length <= 4096" },
          "duration": { ".validate": "newData.isNumber()" },
          "pos": { ".validate": "newData.isNumber()" },
          "at": { ".validate": "newData.isNumber()" },
          "tracks": { ".validate": "newData.isString() && newData.val().length <= 600000" },
          "$other": { ".validate": false }
        },
        "$other": { ".validate": false }
      }
    },
    "clock": {
      "$uid": {
        ".read": "auth != null && auth.uid == $uid",
        ".write": "auth != null && auth.uid == $uid",
        ".validate": "newData.isNumber()"
      }
    }
  }
}
```

不配置也能构建：局域网 / 内网穿透联机时照样能经房主的游戏一起听，只是不在同一个服务器时没法一起听；只有连同一个 Firebase 项目的人才能经 Firebase 一起听。

不想用 Firebase、想自己架服务器的，可以看第三方的自建服务端 [dango-party-server](https://github.com/Tea-chabai/dango-party-server)（非官方维护）。
</details>

## 说明

使用各音乐平台的非官方接口，仅供个人学习交流，歌曲版权归各平台和版权方所有。

第三方：[Inter](https://github.com/rsms/inter)（OFL 1.1）、[JLayer](https://central.sonatype.com/artifact/javazoom/jlayer/1.0.1)（LGPL）、[JOrbis / Jogg](https://github.com/ymnk/jorbis)（LGPL 2.0+）、[ZXing](https://github.com/zxing/zxing)（Apache 2.0）、Kotlin 标准库（Apache 2.0）。

界面参考：播放器排版参考 [MuoniumPlayer](https://github.com/zihe114514/MuoniumPlayer-multiversion)；歌手页布局参考 [SoundCloud-Desktop](https://github.com/zxcloli666/SoundCloud-Desktop)。

许可证：[GPL-3.0](LICENSE)
