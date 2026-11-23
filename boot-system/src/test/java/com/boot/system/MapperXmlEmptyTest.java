package com.boot.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 守卫测试：第四波转换后，mapper/system 下的 XML 只保留空的 mapper 定义，
 * 不允许再出现任何 select/insert/update/delete 语句（所有 SQL 已收敛到 Repository 的 MP wrapper）。
 */
class MapperXmlEmptyTest {

    private static final int EXPECTED_XML_FILES = 8;

    private static final Pattern STATEMENT = Pattern.compile("<(select|insert|update|delete)\\b");

    private static final Pattern RESIDUAL = Pattern.compile("<(sql|resultMap)\\b");

    private static final Pattern NAMESPACE = Pattern.compile("<mapper\\s+namespace=\"([^\"]+)\"");

    @Test
    void mapperXmlFilesContainOnlyEmptyMapperRoot() throws IOException, ClassNotFoundException {
        Path mapperDir = Path.of("src", "main", "resources", "mapper", "system");
        List<Path> xmlFiles;
        try (Stream<Path> files = Files.list(mapperDir)) {
            xmlFiles = files
                    .filter(p -> p.getFileName().toString().endsWith(".xml"))
                    .sorted()
                    .toList();
        }
        assertEquals(EXPECTED_XML_FILES, xmlFiles.size(),
                "mapper/system 下应保留 " + EXPECTED_XML_FILES + " 个空 XML 文件");

        for (Path xml : xmlFiles) {
            String content = Files.readString(xml, StandardCharsets.UTF_8);
            assertFalse(STATEMENT.matcher(content).find(),
                    xml + " 不允许包含 select/insert/update/delete 语句");
            assertFalse(RESIDUAL.matcher(content).find(),
                    xml + " 不允许残留 <sql>/<resultMap> 片段");
            assertTrue(content.contains("<mapper namespace=\""),
                    xml + " 必须保留 mapper namespace 定义");
            assertTrue(content.contains("</mapper>"),
                    xml + " 必须保留闭合的 mapper 根元素");

            Matcher namespace = NAMESPACE.matcher(content);
            assertTrue(namespace.find(), xml + " 必须声明 mapper namespace");
            String namespaceName = namespace.group(1);
            assertTrue(namespaceName.startsWith("com.boot.system.repo.mapper."),
                    xml + " 的 namespace 必须位于 com.boot.system.repo.mapper 包下");
            Class<?> mapperInterface = Class.forName(namespaceName);
            assertTrue(mapperInterface.isInterface(),
                    namespaceName + " 必须是 mapper 接口");
            assertEquals(xml.getFileName().toString().replace(".xml", ""),
                    mapperInterface.getSimpleName(),
                    xml + " 的 namespace 必须指向同名 mapper 接口");
        }
    }
}