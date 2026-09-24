package com.luismunozse.reservalago.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PermanentDeletionSchemaTest {

    @Test
    void projectChildrenCascadeButMediaAssetsAreKept() throws IOException {
        assertThat(normalizedMigration("V17__create_project_advances_table.sql"))
                .contains("project_id uuid not null references projects(id) on delete cascade");
        assertThat(normalizedMigration("V18__create_project_images_table.sql"))
                .contains("project_id uuid not null references projects(id) on delete cascade");
        assertThat(normalizedMigration("V19__create_project_documents_table.sql"))
                .contains("project_id uuid not null references projects(id) on delete cascade");

        assertThat(normalizedMigration("V20__create_media_assets_and_project_image_links.sql"))
                .contains("foreign key (image_asset_id) references media_assets(id) on delete set null")
                .contains("foreign key (media_asset_id) references media_assets(id) on delete set null");
        assertThat(normalizedMigration("V21__link_documents_and_advances_to_media_assets.sql"))
                .contains("foreign key (media_asset_id) references media_assets(id) on delete set null")
                .contains("foreign key (image_asset_id) references media_assets(id) on delete set null");
        assertThat(normalizedMigration("V22__link_project_videos_to_media_assets.sql"))
                .contains("foreign key (video_asset_id) references media_assets(id) on delete set null");
    }

    @Test
    void newsChildrenAndSocialContentCascadeButMediaAssetsAreKept() throws IOException {
        String newsMigration = normalizedMigration("V23__create_news_tables.sql");

        assertThat(newsMigration)
                .contains("news_id uuid not null references news(id) on delete cascade")
                .contains("image_asset_id uuid references media_assets(id) on delete set null")
                .contains("video_asset_id uuid references media_assets(id) on delete set null")
                .contains("media_asset_id uuid references media_assets(id) on delete set null");
    }


    @Test
    void multimediaGalleryTablesCascadeOwnersButKeepMediaAssets() throws IOException {
        String migration = normalizedMigration("V26__create_multimedia_gallery_items.sql");

        assertThat(migration)
                .contains("create table if not exists project_gallery_items")
                .contains("project_id uuid not null references projects(id) on delete cascade")
                .contains("create table if not exists project_advance_gallery_items")
                .contains("advance_id uuid not null references project_advances(id) on delete cascade")
                .contains("create table if not exists news_gallery_items")
                .contains("news_id uuid not null references news(id) on delete cascade")
                .contains("media_asset_id uuid references media_assets(id) on delete set null")
                .contains("insert into project_gallery_items")
                .contains("insert into project_advance_gallery_items")
                .contains("insert into news_gallery_items");
    }
    private String normalizedMigration(String filename) throws IOException {
        return Files.readString(Path.of("src", "main", "resources", "db", "migration", filename))
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }
}