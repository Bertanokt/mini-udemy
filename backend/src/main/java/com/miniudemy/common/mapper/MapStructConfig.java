package com.miniudemy.common.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

/**
 * Tüm MapStruct mapper'larının ortak ayarı (DTO + Mapper deseni, ADR-006).
 *
 * Her modülün mapper'ı {@code @Mapper(config = MapStructConfig.class)} ile bu ayarı kullanır:
 * <ul>
 *   <li>{@code componentModel = "spring"}: mapper bir Spring bean'idir, service'e constructor ile verilir.</li>
 *   <li>{@code unmappedTargetPolicy = ERROR}: DTO'ya eklenen bir alanın kaynağı yoksa derleme durur. Böylece
 *       sözleşmeye eklenen bir alan sessizce {@code null} dönmez; ya eşlenir ya da bilerek
 *       {@code @Mapping(target = "...", ignore = true)} ile yok sayılır.</li>
 * </ul>
 */
@MapperConfig(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MapStructConfig {
}
