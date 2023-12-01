package org.corgi.consumer.sourcedownload.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.corgi.consumer.sourcedownload.model.enums.CommonStatusEnum;
import org.corgi.consumer.sourcedownload.model.enums.TenantEnum;

import java.util.Date;

@Data
public class BaseDo {
    private String id;

    private Date gmtCreate;

    private Date gmtModified;

    private TenantEnum tenant;

    private CommonStatusEnum status;
}
