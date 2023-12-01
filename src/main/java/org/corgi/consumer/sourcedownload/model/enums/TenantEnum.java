package org.corgi.consumer.sourcedownload.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum TenantEnum {

    /**
     * ALIPAY = 蚂蚁主站
     */
    ALIPAY("蚂蚁主站"),
    /**
     * ANTCLOUD = 金融云
     */
    ANTCLOUD("金融云"),
    /**
     * ALIBABA = 集团
     */
    ALIBABA("阿里集团"),
    /**
     * BFRXATCN = 网商LinkE部分
     */
    BFRXATCN("网商银行"),
    /**
     * FRESHHEMA = 盒马
     */
    FRESHHEMA("盒马"),
    ;

    private String displayName;

    private final static TenantEnum DEFAULT_ENUM = null;

    public static TenantEnum findEnumByName(String name) {
        return Arrays.stream(TenantEnum.values())
                .filter(e -> StringUtils.equalsIgnoreCase(e.name(), name))
                .findFirst()
                .orElse(DEFAULT_ENUM);
    }

    public static TenantEnum findEnumByName(String name, TenantEnum defaultEnum) {
        return Optional.ofNullable(findEnumByName(name))
                .orElse(Optional.ofNullable(defaultEnum).orElse(DEFAULT_ENUM));
    }
}
