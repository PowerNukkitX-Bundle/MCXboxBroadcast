plugins {
    id("com.rtm516.mcxboxbroadcast.shadow-conventions")
}

repositories {
    maven("https://repo.powernukkitx.org/releases")
}

dependencies {
    api(project(":core")) {
        exclude(group = "org.powernukkitx.protocol")
    }
    compileOnly(libs.powernukkitx.server)
}

sourceSets {
    main {
        blossom {
            val info = GitInfo(indraGit)
            resources {
                property("version", info.version)
            }
        }
    }
}

relocate("org.yaml.snakeyaml")
relocate("org.spongepowered.configurate")
relocate("com.google.gson")
relocate("net.raphimc.minecraftauth")
relocate("org.bouncycastle")
relocate("net.lenni0451.commons.httpclient")
relocate("net.lenni0451.commons.gson")

nameJar("MCXboxBroadcast")

description = "bootstrap-pnx"
