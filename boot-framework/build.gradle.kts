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

    // BOM 管理的依赖（不带版本）
    api("org.springframework.boot:spring-boot-starter-webmvc")
    api("org.springframework.boot:spring-boot-starter-aspectj")

    // catalog 带版本依赖
    api(libs.druid.spring.boot4.starter)
    api(libs.mybatis.plus.jsqlparser)
    api(libs.oshi.core)
    api(libs.kaptcha) { exclude(module = "servlet-api") }

    // 系统模块
    api(project(":boot-system"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Gradle 9 起不再自动注入 JUnit Platform launcher
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
