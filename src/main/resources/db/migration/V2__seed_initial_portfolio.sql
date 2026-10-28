insert into portfolio_items (slug, type, title, category, description, service_tags, featured_on_home, sort_order)
values
('aura-home', 'PROJECT', 'Aura Home', 'SOCIAL MEDIA', 'Визуальное оформление контента для бренда товаров для дома и аксессуаров. Работа с фактурами, деталями продукта, гармоничной цветовой палитрой и единой эстетикой Instagram.', array['VISUAL CONTENT', 'PRODUCT STYLING', 'INSTAGRAM', 'REELS'], true, 1),
('onstudy-prep', 'PROJECT', 'OnStudy Prep', 'EDUCATION · CONTENT', 'Экспертный контент о подготовке к IELTS и SAT, результатах учеников и образовательных продуктах.', array['CONTENT', 'VISUAL DESIGN', 'INSTAGRAM'], true, 2),
('buketto-flowers', 'PROJECT', 'Buketto Flowers', 'REELS & VIDEO', 'Живой контент для цветочного бренда: эстетика букетов, закулисье флористики, мероприятия и команда. Reels, динамичный монтаж и визуальный сторителлинг раскрывают атмосферу бренда.', array['REELS', 'VIDEO EDITING', 'PRODUCT CONTENT', 'BRAND STORYTELLING'], false, 3),
('baipak-kids', 'PROJECT', 'Baipak Kids', 'RETAIL · SMM CONTENT', 'Визуальный контент для магазина детских товаров: презентация ассортимента, акцент на качестве продукции и понятная коммуникация для родителей.', array['PRODUCT CONTENT', 'VISUAL DESIGN', 'BRAND COMMUNICATION'], false, 4),
('la-diva', 'CASE_STUDY', 'La Diva', 'МОДА · НИЖНЕЕ БЕЛЬЁ · ВИЗУАЛ', 'Визуальная концепция и контент-подача для Instagram: выразительные фактуры, оттенки и гармоничная композиция помогают раскрыть характер бренда.', array['PRODUCT VISUALS', 'FASHION CONTENT', 'INSTAGRAM'], false, 5);

insert into portfolio_media (portfolio_item_id, media_type, url, alt_text, sort_order)
select id, 'IMAGE', '/uploads/aura.homekgz.jpg', 'Aura Home — предметный визуальный контент для бренда товаров для дома', 1 from portfolio_items where slug = 'aura-home';
insert into portfolio_media (portfolio_item_id, media_type, url, alt_text, sort_order)
select id, 'IMAGE', '/uploads/onstudy.prep.jpg', 'OnStudy Prep — контент для образовательного проекта', 1 from portfolio_items where slug = 'onstudy-prep';
insert into portfolio_media (portfolio_item_id, media_type, url, alt_text, sort_order)
select id, 'IMAGE', '/uploads/buketto.jpg', 'Buketto Flowers — фото и видеоконтент для цветочного бренда', 1 from portfolio_items where slug = 'buketto-flowers';
insert into portfolio_media (portfolio_item_id, media_type, url, alt_text, sort_order)
select id, 'IMAGE', '/uploads/baipak.jpg', 'Baipak Kids — визуальный контент для магазина детских товаров', 1 from portfolio_items where slug = 'baipak-kids';
insert into portfolio_media (portfolio_item_id, media_type, url, alt_text, sort_order)
select id, 'IMAGE', '/uploads/ladvina.jpg', 'La Diva — визуальный контент для бренда нижнего белья', 1 from portfolio_items where slug = 'la-diva';
