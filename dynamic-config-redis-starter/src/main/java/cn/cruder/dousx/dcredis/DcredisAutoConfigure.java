package cn.cruder.dousx.dcredis;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({DcredisConfiguration.class})
public class DcredisAutoConfigure {
}
