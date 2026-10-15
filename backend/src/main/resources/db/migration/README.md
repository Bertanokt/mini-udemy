# Flyway migration'ları

- Dosya adı: `V<sıra>__<kisa_aciklama>.sql` (örn. `V1__create_users.sql`). İki alt çizgi zorunlu.
- Merge edilmiş bir migration dosyası **bir daha değiştirilmez**; düzeltme yeni bir `V<n+1>` dosyasıyla yapılır.
- Aynı sıra numarasını iki kişi almasın diye migration içeren issue'lar ER diyagramı issue'suna bağlıdır;
  numara çakışırsa sonra merge edilen PR kendi numarasını kaydırır.
