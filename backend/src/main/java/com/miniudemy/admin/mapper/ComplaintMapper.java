package com.miniudemy.admin.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.miniudemy.admin.domain.Complaint;
import com.miniudemy.admin.dto.ComplaintResponse;
import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.common.mapper.MapStructConfig;

/** {@code Complaint} -> {@code ComplaintResponse}. Şikâyet edenin adı auth modülünden geldiği için hazır verilir. */
@Mapper(config = MapStructConfig.class)
public interface ComplaintMapper {

    @Mapping(target = "id", source = "complaint.id")
    @Mapping(target = "reporter", source = "reporter")
    ComplaintResponse toResponse(Complaint complaint, UserSummary reporter);
}
