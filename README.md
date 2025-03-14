## 发布

- 发布到配置仓库命令：`gradle clean sourcesJar publish`
- 发布到本地仓库命令：`gradle clean sourcesJar publishToMavenLocal`

## 使用

1. 引入依赖

```groovy
    implementation 'cn.cruder.dousx:dynamic-config-redis-starter:1.1.20250306-14'
```

2. 配置文件中指定`dcredis.config.nameSpace`

```yaml
dcredis:
  config:
    nameSpace: service-b
```

3. 添加`@EnableDynamicConfig`注解

```java

@EnableDynamicConfig
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

4. 定义配置接口
- `@DynamicConfig`标记为配置类。
- `@DynamicConfigProperty`指定key(建议使用`:`将key隔开,在大部分redis可视化工具中，会通过`:`将key分割做成树形展示)。
示例如下：

```java

@DynamicConfig
public interface ReaderConfig {

    @DynamicConfigProperty(key = "reader:url", defaultValue = "localhost")
    String getReaderUrl();

    @DynamicConfigProperty(key = "reader:port", defaultValue = "6369")
    Integer getReaderPort();

    @DynamicConfigProperty(key = "reader:retry", defaultValue = "true")
    Boolean getReaderRetry();
}
```
