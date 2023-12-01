package org.corgi.consumer.sourcedownload.model.repo.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author fxt
 * @version : RepoStatusEnum.java, v 0.1 2021/12/2 6:45 下午 fxt Exp $
 */
@Getter
public enum RepoStatusEnum {

    /**
     * 更新中
     */
    UPDATE,
    /**
     * 更新完成
     */
    DONE,
    ;


    private final static RepoStatusEnum DEFAULT_ENUM = null;

    public static RepoStatusEnum findEnumByName(String name) {
        return Arrays.stream(RepoStatusEnum.values())
            .filter(e -> StringUtils.equalsIgnoreCase(e.name(), name))
            .findFirst()
            .orElse(DEFAULT_ENUM);
    }

    public static RepoStatusEnum findEnumByName(String name, RepoStatusEnum defaultEnum) {
        return Optional.ofNullable(findEnumByName(name))
            .orElse(Optional.ofNullable(defaultEnum).orElse(DEFAULT_ENUM));
    }
}
