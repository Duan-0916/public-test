package org.corgi.consumer.sourcedownload.model.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;



import java.util.Optional;


public enum CommonStatusEnum {
    /**
     * 可用
     */
    AVAILABLE,
    /**
     * 不可用
     */
    UNAVAILABLE,
    ;

    private final static CommonStatusEnum DEFAULT_ENUM = null;

    public static CommonStatusEnum findEnumByName(String name) {
        return Arrays.stream(CommonStatusEnum.values())
                .filter(e -> StringUtils.equalsIgnoreCase(e.name(), name))
                .findFirst()
                .orElse(DEFAULT_ENUM);
    }

    public static CommonStatusEnum findEnumByName(String name, CommonStatusEnum defaultEnum) {
        return Optional.ofNullable(findEnumByName(name))
                .orElse(Optional.ofNullable(defaultEnum).orElse(DEFAULT_ENUM));
    }
}
