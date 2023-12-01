package org.corgi.consumer.sourcedownload.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum TaskStatusEnum {

    /**
     * 任务状态
     */
    NEW("NEW", "新任务"),
    PENDING("PENDING", "队列中等待调度"),
    RUNNING("RUNNING", "执行中"),
    CANCELED("CANCELED", "任务取消"),
    DONE("DONE", "任务执行成功"),
    ERROR("ERROR", "任务执行失败"),
    ;

    private String code;

    private String desc;

    private final static TaskStatusEnum DEFAULT_ENUM = null;

    public static TaskStatusEnum findEnumByName(String name) {
        return Arrays.stream(TaskStatusEnum.values())
                .filter(e -> StringUtils.equalsIgnoreCase(e.name(), name))
                .findFirst()
                .orElse(DEFAULT_ENUM);
    }

    public static TaskStatusEnum findEnumByName(String name, TaskStatusEnum defaultEnum) {
        return Optional.ofNullable(findEnumByName(name))
                .orElse(Optional.ofNullable(defaultEnum).orElse(DEFAULT_ENUM));
    }
}
