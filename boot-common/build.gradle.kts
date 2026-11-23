plugins {
    `java-library`
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

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    // 注解处理器：lombok → lombok-mapstruct-binding → mapstruct-plus-processor
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.lombok.mapstruct.binding)
    annotationProcessor(libs.mapstruct.plus.processor)
    compileOnly(libs.lombok)

    // BOM 管理的依赖（不带版本）—— compile scope，对消费者传递（等价 Maven compile）
    api("org.springframework:spring-context-support")
    api("org.springframework:spring-web")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("tools.jackson.core:jackson-databind")
    api("org.apache.commons:commons-lang3")
    api("com.github.ben-manes.caffeine:caffeine")
    api("org.apache.commons:commons-pool2")
    api("jakarta.servlet:jakarta.servlet-api")

    // catalog 带版本依赖
    api(libs.mybatis.plus.extension)
    api(libs.fastjson2)
    api(libs.commons.io)
    api(libs.poi.ooxml)
    api(libs.jjwt.api)
    api(libs.yauaa)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Gradle 9 起不再自动注入 JUnit Platform launcher
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
