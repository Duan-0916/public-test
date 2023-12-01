package org.corgi.consumer.sourcedownload.model.repo.enums;
;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author fxt
 * @version : RepoTypeEnum.java, v 0.1 2021/12/2 6:44 下午 fxt Exp $
 */
@Getter
@AllArgsConstructor
public enum RepoTypeEnum {

    /**
     * git
     */
    GIT,
    /**
     * svn
     */
    SVN,
    ;

    private final static RepoTypeEnum DEFAULT_ENUM = null;

    public static RepoTypeEnum findEnumByName(String name) {
        return Arrays.stream(RepoTypeEnum.values())
            .filter(e -> StringUtils.equalsIgnoreCase(e.name(), name))
            .findFirst()
            .orElse(DEFAULT_ENUM);
    }

    public static RepoTypeEnum findEnumByName(String name, RepoTypeEnum defaultEnum) {
        return Optional.ofNullable(findEnumByName(name))
            .orElse(Optional.ofNullable(defaultEnum).orElse(DEFAULT_ENUM));
    }
}
