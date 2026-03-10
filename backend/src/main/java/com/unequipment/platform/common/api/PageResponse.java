package com.unequipment.platform.common.api;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> list;
    private long total;
    private int pageNum;
    private int pageSize;
}
