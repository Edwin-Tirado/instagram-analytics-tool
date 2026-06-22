package ec.ucsg.analytics.infrastructure.instagram.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Hijos de un post de tipo CAROUSEL_ALBUM. */
@Data
@NoArgsConstructor
public class InstagramChildrenResponse {
    private List<InstagramMediaItem> data;
}
