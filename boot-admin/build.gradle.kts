plugins {
    java
    alias(libs.plugins.spring.boot)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()
}

// 与 Maven finalName 一致：产出 boot-admin.jar
tasks.bootJar {
    archiveFileName = "boot-admin.jar"
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    // 注解处理器：lombok → lombok-mapstruct-binding → mapstruct-plus-processor
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.lombok.mapstruct.binding)
    annotationProcessor(libs.mapstruct.plus.processor)
    compileOnly(libs.lombok)

    // 核心模块（boot-common 的 api 依赖经 apiElements 变体传递到编译类路径）
    implementation(project(":boot-framework"))

    // BOM 管理的依赖（不带版本）—— developmentOnly 不继承 implementation，需单独对齐 BOM
    developmentOnly(platform(libs.spring.boot.dependencies))
    implementation("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Gradle 9 起不再自动注入 JUnit Platform launcher
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
