package org.corgi.consumer.sourcedownload.model.repo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author fxt
 * @version : LanguageEnum.java, v 0.1 2021/12/2 6:57 下午 fxt Exp $
 */
@Getter
@AllArgsConstructor
public enum LanguageEnum {
    /**
     * c
     */
    C("C"),
    /**
     * c++
     */
    CPP("C++"),
    /**
     * python
     */
    PYTHON("Python"),
    ;

    private String code;

    private final static LanguageEnum DEFAULT_ENUM = null;

    public static LanguageEnum findEnumByName(String name) {
        return Arrays.stream(LanguageEnum.values())
            .filter(e -> StringUtils.equalsIgnoreCase(e.name(), name))
            .findFirst()
            .orElse(DEFAULT_ENUM);
    }

    public static LanguageEnum findEnumByName(String name, LanguageEnum defaultEnum) {
        return Optional.ofNullable(findEnumByName(name))
            .orElse(Optional.ofNullable(defaultEnum).orElse(DEFAULT_ENUM));
    }
}
