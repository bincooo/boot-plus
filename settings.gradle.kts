// 仓库：阿里云优先，mavenCentral 兜底
pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/public")
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/public")
        mavenCentral()
    }
}

rootProject.name = "boot-plus"
include(":boot-common", ":boot-system", ":boot-framework", ":boot-admin")
