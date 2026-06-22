package ec.ucsg.analytics.infrastructure.instagram.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Envoltura paginada devuelta por el endpoint /{user-id}/media. */
@Data
@NoArgsConstructor
public class InstagramMediaResponse {

    private List<InstagramMediaItem> data;

    private Paging paging;

    @Data
    @NoArgsConstructor
    public static class Paging {
        private Cursors cursors;
        private String  next;   // null cuando no hay más páginas

        @Data
        @NoArgsConstructor
        public static class Cursors {
            private String before;
            private String after;
        }
    }
}
