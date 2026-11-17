-- Kategori adı büyük/küçük harf duyarsız tekil (#118). Mevcut uk_categories_name yalnız tam eşleşmeyi tutar;
-- "Yazılım" ile "yazılım" ayrı satır olabilirdi. Seed kategorileri (#109) bu kurala uyuyor.
CREATE UNIQUE INDEX uk_categories_name_lower ON categories (lower(name));
