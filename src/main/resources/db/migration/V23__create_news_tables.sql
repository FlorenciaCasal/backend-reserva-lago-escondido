create table news (
    id uuid primary key,
    title varchar(160) not null,
    summary varchar(600) not null,
    content text not null,
    slug varchar(160) not null unique,
    image_asset_id uuid references media_assets(id) on delete set null,
    image_url varchar(500),
    video_asset_id uuid references media_assets(id) on delete set null,
    video_url varchar(500),
    status varchar(20) not null default 'DRAFT',
    published_at timestamp,
    archived_at timestamp,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint ck_news_status check (status in ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

create index idx_news_status_published_at on news(status, published_at desc, created_at desc);

create table news_images (
    id uuid primary key,
    news_id uuid not null references news(id) on delete cascade,
    media_asset_id uuid references media_assets(id) on delete set null,
    image_url varchar(500) not null,
    alt_text varchar(180),
    caption varchar(300),
    sort_order integer not null default 0,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create index idx_news_images_news_order on news_images(news_id, sort_order, created_at);

create table news_social_contents (
    id uuid primary key,
    news_id uuid not null references news(id) on delete cascade,
    platform varchar(20) not null,
    caption text,
    body text,
    hashtags text,
    call_to_action varchar(300),
    alt_text text,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint ck_news_social_contents_platform check (platform in ('INSTAGRAM', 'FACEBOOK')),
    constraint uk_news_social_contents_news_platform unique (news_id, platform)
);
