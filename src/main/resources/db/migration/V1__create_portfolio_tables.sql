create table portfolio_items (
    id bigserial primary key,
    slug varchar(120) not null unique,
    type varchar(30) not null check (type in ('PROJECT', 'CASE_STUDY')),
    title varchar(180) not null,
    category varchar(180) not null,
    description text not null,
    service_tags text[] not null default '{}',
    featured_on_home boolean not null default false,
    published boolean not null default true,
    sort_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table portfolio_media (
    id bigserial primary key,
    portfolio_item_id bigint not null references portfolio_items(id) on delete cascade,
    media_type varchar(20) not null check (media_type in ('IMAGE', 'VIDEO')),
    url varchar(1000) not null,
    alt_text varchar(300) not null default '',
    poster_url varchar(1000),
    sort_order integer not null default 0
);

create index portfolio_items_public_order_idx
    on portfolio_items (published, sort_order);
create index portfolio_media_item_order_idx
    on portfolio_media (portfolio_item_id, sort_order);
