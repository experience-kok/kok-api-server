package com.example.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class YouTubeChannelInfo {

    private String kind;
    private String etag;
    private String pageInfo;
    private List<Item> items;

    @Getter
    @ToString
    public static class Item {
        private String kind;
        private String etag;
        private String id;
        private Snippet snippet;
    }

    @Getter
    @ToString
    public static class Snippet {
        private String title;
        private String description;
        private String customUrl;
        private String publishedAt;
        private Thumbnails thumbnails;
        private String country;
    }

    @Getter
    @ToString
    public static class Thumbnails {
        private Thumbnail default_;
        private Thumbnail medium;
        private Thumbnail high;

        @JsonProperty("default")
        public Thumbnail getDefault_() {
            return default_;
        }

        @JsonProperty("default")
        public void setDefault_(Thumbnail default_) {
            this.default_ = default_;
        }
    }

    @Getter
    @ToString
    public static class Thumbnail {
        private String url;
        private int width;
        private int height;
    }
}