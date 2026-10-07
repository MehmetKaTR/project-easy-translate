# TaleMind Sözlük Sistemi — Roadmap

Kendi sözlük veritabanımız ("TaleMind Sözlüğü") + özgür kelime ekleme + akıllı eşleştirme
+ premium AI kart. Amaç: kurumsal, hızlı, offline, maliyet ~$0.

## 0. İlkeler
- **Veri:** Wiktionary (kaikki.org dump) → kendi DB'miz: sınırsız, hızlı, offline, kürasyonlu.
  - Lisans: Wiktionary **CC BY-SA** → kaynak gösterimi (attribution) + share-alike zorunlu.
- **Çeviri:** Google Cloud Translate, **lazy + cache** → her (anlam, hedef dil) **bir kez** çevrilir,
  DB'ye yazılır, sonrası hep **$0**.
- **Özgürlük korunur:** kullanıcı kendi anlamını yazabilir (Anki gibi).

## 1. Veri modeli (tablolar)
- **dictionary_entry** — id, word, language_code, cefr, category (nullable). (kelime başına 1)
- **dictionary_sense** — id, entry_id, sıra, pos, idiom?, definition(EN), examples(EN,json),
  synonyms(EN,json). (anlam başına 1 → homonym/çok-anlam burada)
- **dictionary_sense_tr** — id, sense_id, target_lang, definition_tr, examples_tr(json),
  primary (ör. "yarasa"), variants(json) (ör. ["sopa","beyzbol sopası"]). (lazy doldurulur)
- **translation_cache** (mevcut) — genel (text, src, tgt) → çeviri.
- **user_word** (mevcut) + yeni: dictionary_entry_id (nullable), sense_id, is_custom, ai_generated.
- **custom_card** (opsiyonel) — premium AI kartları.

## 2. ETL — sözlük üretimi (kaikki)
- kaikki İngilizce JSONL'i bir kez indir (~3.3GB, akışla işlenir, diske büyük dosya kalmaz).
- Parser (çok-anlamlı): kelime başına en önemli 2-3 distinct anlam (pos+def+örnek+sinonim).
- CEFR seviyesi: CEFR-J (A1-B2) + Octanove (C1-C2) → ~8.6k kelimeye seviye etiketi.
- Kategori: Wiktionary konu etiketlerinden (varsa).
- İdiom: sıkı filtre (idiomatic-tag veya ≤3 kelime yaygın öbek).
- Kaynaklar (hepsi ücretsiz):
  - kaikki.org/dictionary/English/kaikki.org-dictionary-English.jsonl
  - CEFR: github.com/openlanguageprofiles/olp-en-cefrj (cefrj-vocabulary-profile-1.5.csv + octanove-...c1c2-1.0.csv)
  - (opsiyonel örnekler) Tatoeba dump

## 3. Çeviri katmanı (lazy, $0) — Google Cloud Translation
- Kart ilk açıldığında (hedef TR): her anlamın tanım+örnek'i çevrilir → dictionary_sense_tr'ye yazılır.
  Sonraki açılışlar DB'den, $0.
- Variants (yokluk/bulunmama/gıyap) → Google alternatif çeviriler (dt=bd) lazy, saklanır (eşleştirme).
- Bonus: kaikki `translations` alanından TR'leri önceden seed → daha az Google çağrısı.
- Fiyat: $20/1M karakter, ilk 500k/ay bedava → bu hacimde $0. Kart/billing + $1 bütçe alarmı.

## 4. Runtime — kart gösterimi
- Detay: dictionary_entry + dictionary_sense DB'den anında (çok-anlamlı, CEFR rozeti, kategori).
- TR: dictionary_sense_tr varsa göster; yoksa lazy Google → yaz → göster.
- Kart örn: "bat: 1) yarasa … 2) beyzbol sopası …" her anlam kendi TR'siyle.

## 5. Akıllı ekleme akışı
```
kelime yaz (EN) → dictionary_entry'de ara
 ├ VAR → anlamları göster ("bat: yarasa / sopa — kullanmak ister misin?")
 │   ├ bir anlamı seçti → user_word(dictionary_entry_id, sense_id) → ZENGİN KART
 │   ├ yazdığı çeviri bir anlamın primary/variant'ıyla eşleşti → o anlama bağla
 │   └ farklı/custom → "DB'de var, kartlı ekleyeyim mi?" → evet=kart / hayır=CUSTOM
 └ YOK → CUSTOM (kelime+anlam, kart yok)   [premium: "AI ile kart üret"]
```
- Quiz cevabı = kullanıcının seçtiği anlam; kart hepsini gösterir, onunki vurgulanır.

## 6. Premium AI kart + guard
- Custom/bulunamayan kelimede premium "AI ile oluştur" → Gemini bizim kart şablonunda üretir.
- Guard: anlamsız/çelişkili/gerçek olmayan girdi → "kart oluşturulamadı."

## 7. Kısıtlamalar (kurumsallık)
- Ruleset (çöp eleme) + max uzunluk + suggest/translate rate-limit (kullanıcı/dk) + sabit dil çiftleri.

## 8. Fazlar & sıra
1. Şema: 4 tablo + user_word alanları (migration).
2. ETL (çok-anlamlı + CEFR + kategori) → dictionary_entry/sense yükle.
3. Çeviri katmanı: Google Cloud key + lazy + cache (dictionary_sense_tr).
4. Runtime kart: DB-önce, çok-anlamlı, CEFR rozeti.
5. Akıllı ekleme akışı (eşleştirme + custom/kart).
6. Premium AI kart + guard.
7. Cila: kategoriler, DE/ES hedefleri, quiz entegrasyonu.

## Maliyet
- Veri: $0 (Wiktionary). Çeviri: ~$0 (lazy + cache + Google 500k/ay bedava; her anlam bir kez çevrilir).
- AI: sadece premium + guard'lı. Depolama: birkaç sent. → Pratikte $0.

## Dikkat edilecekler
- Wiktionary CC BY-SA: attribution + share-alike (kurumsal uyumluluk).
- Çeviri kalitesi/variant eşleştirmede yanlış eşleşmeye dikkat (eşik/normalize).
- Kapsam büyük → MVP-önce, fazlı ilerle; hepsini birden kurma.
- Wiktionary dump periyodik güncellenir → ara sıra yeniden ETL.
- Sense seçim kalitesi (Wiktionary sırası/obscure anlamlar) → ayar gerektirir.
