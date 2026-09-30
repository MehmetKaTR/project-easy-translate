# TaleMind AI — Backend Mimari Rehberi

> Projenin **tüm yapısını** sıfırdan açıklar: katmanlar, her entity, her servis, DTO'lar,
> güvenlik ve uçtan uca akışlar. Amaç: projeye tam hâkim olmak (+ mülakat hazırlığı).

---

## 1. Proje ne yapıyor?
**TaleMind AI** (talemindai.com) — bir **dil öğrenme** uygulamasının backend'i:
- Kullanıcı **kelime listeleri** oluşturur, kelime ekler (word + çeviri).
- Seçtiği kelimelerden **yapay zeka (Google Gemini)** ile **hikâye üretir** — kelimeleri bağlamda öğretir.
- **Tam auth sistemi:** kayıt, e-posta doğrulama, şifre sıfırlama, JWT + refresh token, Google ile giriş.
- **Admin paneli:** kullanıcı yönetimi, AI model yönetimi, token kullanım raporları, denetim (audit) logları.

**Teknoloji:** Java 21, Spring Boot 3.5, Spring Data JPA (MySQL), Spring Security (JWT), Google Gemini API, Resend (e-posta). Tek veritabanı: **MySQL**.

---

## 2. Katmanlı mimari (en önemli kavram)
İstek şu katmanlardan **sırayla** geçer:

```
İstemci (web/mobil)
   │  HTTP + JSON
   ▼
[Filtreler]  TraceIdFilter → JwtAuthenticationFilter   (kimlik + izleme)
   ▼
[Controller]  HTTP'yi karşılar, DTO alır/döner, iş yapmaz
   ▼
[Service]     İŞ MANTIĞI burada (kurallar, doğrulama, orkestrasyon)
   ▼
[Repository]  Sadece veritabanı erişimi (Spring Data JPA)
   ▼
[Entity]      Veritabanı tablosunun Java karşılığı
   ▼
MySQL
```

**Her katmanın tek bir işi var (Separation of Concerns):**
- **Controller:** "postacı" — isteği alır, servise verir, cevabı DTO olarak döner. İş kuralı YOK.
- **Service:** "beyin" — tüm iş mantığı, `@Transactional` yönetimi.
- **Repository:** "veri kapısı" — SQL/sorgu. `JpaRepository`'yi extend eder.
- **Entity:** tablo = sınıf, satır = nesne.
- **DTO:** dış dünyayla konuşma nesnesi (entity'yi asla direkt dışarı vermeyiz).

---

## 3. Entity Katmanı (veritabanı tabloları)

Hepsi `BaseEntity`'den miras alır → otomatik `created_at` / `updated_at` (JPA Auditing).

| Entity | Tablo | Ne tutar | Önemli ilişkiler |
|---|---|---|---|
| **User** | `users` | Kullanıcı hesabı: username, email, passwordHash, emailVerified, subscriptionLevel (FREE/PREMIUM), tokenBalance, dil/tema tercihi, hesap silme zamanları, e-posta/şifre doğrulama kodları | — |
| **Word** | `words` | Bir kelime: word, translated (çeviri), languageCode, starred | `@ManyToOne User`, `@ManyToMany WordList` |
| **WordList** | `wordlists` | Kelime listesi: name, hexColorCode | `@ManyToOne User`, `@ManyToMany Word` |
| **Story** | `stories` | Üretilen hikâye: content, çeviriler, promptWords, wordMappingsJson (LONGTEXT), starred | `userId` (Long alan — FK değil, gevşek) |
| **RefreshToken** | `refresh_tokens` | Refresh token: tokenHash (ham token DEĞİL!), expiresAt, revokedAt, replacedByTokenHash, IP/UA | `@ManyToOne User` |
| **Admin** | `admins` | Yönetici hesabı: username, email, passwordHash | `@OneToMany AdminActionLog` |
| **AdminActionLog** | `admin_action_log` | Denetim izi: admin kim, hedef kullanıcı, aksiyon tipi (DELETE_USER vb.) | `@ManyToOne Admin`, `@ManyToOne User` |
| **TokenUsageLog** | `token_usage_log` | Her AI üretiminde: user, storyId, tokensUsed | `@ManyToOne User` (index: user_id+created_at) |
| **LlmModel** | `llm_models` | AI modeli kaydı: name, description, status (ACTIVE/INACTIVE) | — |

**İlişki türleri (neden):**
- `Word ↔ WordList` = **ManyToMany**: bir kelime birden çok listede olabilir, bir listede çok kelime → ara tablo (`wordlist_words`).
- `User → Word/WordList` = **OneToMany/ManyToOne**: bir kullanıcının çok kelimesi/listesi; sahiplik.
- Tüm `@ManyToOne` **LAZY** (performans: gerekmedikçe yüklenmez).
- **Story neden `userId` alanı, FK değil?** Story eskiden MongoDB'deydi; MySQL'e taşıdık ama gevşek bağ (loose coupling) korundu — sahiplik `userId` ile kontrol edilir.

---

## 4. DTO Katmanı (dış dünya sözleşmesi)

**Neden entity'yi direkt dönmüyoruz?** Güvenlik (passwordHash sızmasın), esneklik (API farklı şekil verebilir), döngü/lazy sorunları önlenir.

- **`dto/request/`** — istemciden GELEN veri (doğrulama anotasyonlu: `@NotBlank`, `@Size`...).
  Örn: `RegisterRequest`, `LoginRequest`, `StoryGenerateRequest`, `LlmModelRequest`.
- **`dto/response/`** — istemciye GİDEN veri.
  Örn: `AuthResponse` (token + kullanıcı bilgisi), `WordDTO`, `StoryDTO`, `LlmModelResponse`.
- **Özel yapılar:**
  - **`PagedResponse<T>`** — sayfalı yanıt sözleşmesi (Spring `Page`'i sızdırmaz): content + totalPages + page...
  - **`WordListSummaryResponse`** — liste görünümü için hafif özet (kelimeler yok, sadece wordCount) → master-detail.

---

## 5. Service Katmanı (iş mantığı) — HER SERVİS

### Kullanıcı & Auth
- **`UserService`** — çekirdek: kayıt (registerUser), giriş (login), e-posta doğrulama, şifre sıfırlama, hesap silme talebi, tercihler. Auth akışının kalbi. En büyük servis.
- **`RefreshTokenService`** — refresh token yaşam döngüsü:
  - `issueRefreshToken`: yeni token verir (önce kullanıcının eski aktiflerini iptal eder → tek aktif token).
  - `rotateRefreshToken`: token yenileme — mevcutu iptal + yenisini üretir, `replacedByTokenHash` ile zinciri izler (çalıntı token tespiti).
  - Ham token **asla saklanmaz** → SHA-256 hash'i saklanır. IP/UserAgent kaydedilir.
- **`TokenService`** — JWT üretimi/doğrulaması. Kullanıcı token'ı (1 saat), admin token'ı (24 saat). HMAC-SHA256 ile imzalar. TTL güvenlik sınırları.
- **`GoogleTokenVerifierService`** — Google ile giriş: Google'ın `tokeninfo` ucundan id_token'ı doğrular, `email_verified` + `aud` (client id allowlist) kontrol eder.
- **`AuthRateLimitService`** — brute-force koruması: bellekte "key başına N istek / süre" sayacı; aşılırsa `RateLimitExceededException`.
- **`AccountLifecycleService`** — `@Scheduled` (30 dk'da bir): silme süresi dolan hesapları temizler (`purgeExpiredPendingDeletions`).
- **`EmailDeliveryService`** — doğrulama/sıfırlama kodu e-postaları (Resend/SMTP), asenkron gönderim.

### Kelime & Hikâye
- **`WordService`** — kelime CRUD; kullanıcı/liste bazlı sorgular (sayfalı + sayfasız overload).
- **`WordListService`** — liste CRUD; özet sorgusu (projeksiyon + COUNT).
- **`StoryService`** — kaydedilmiş hikâyeleri yönetir (ekle, listele-sayfalı, güncelle, sil, yıldızla).
- **`StoryGenerationService`** — **AI çekirdeği**: Gemini'ye prompt gönderir, hikâye üretir; **fallback model zinciri** (bir model çökerse sıradakine geçer); günlük token limitini kontrol eder; token kullanımını loglar. En karmaşık servis.
- **`TranslationSuggestionService`** — hızlı çeviri önerisi (Google Translate ucu).

### Admin & Log
- **`AdminService`** — admin auth/CRUD; `hasAnyAdmin` (bootstrap), `authenticate`, `registerAdmin`.
- **`AdminUserManagementService`** — admin'in kullanıcı yönetimi: listele, güncelle, **sil** (cross-tablo native delete ile tüm ilişkili veriyi temizler), her aksiyonu audit log'a yazar.
- **`AdminActionLogService`** — denetim logları yaz/oku (`logAction`, `findRecent`).
- **`TokenUsageLogService`** — token kullanım kaydı + raporlama (`sumTokensByUserBetween` → günlük limit hesabı).
- **`LlmModelService`** — AI model kayıtları (findAll, create, updateStatus).

---

## 6. Controller Katmanı (uç noktalar)

| Controller | Taban yol | İşi |
|---|---|---|
| `AuthController` | `/api/auth` | kayıt, giriş, doğrulama, şifre sıfırlama, refresh, Google giriş |
| `UserController` | `/api/users` | kullanıcı tercihleri (dil/tema) |
| `WordController` | `/api/words` | kelime ekle/listele/yıldızla (sayfalı) |
| `WordListController` | `/api/wordlists` | liste CRUD (özet listeleme sayfalı) |
| `StoryController` | `/api/stories` | kaydedilmiş hikâyeler (sayfalı) |
| `StoryGenerationController` | `/api/story` | AI: generate, validate-words, limit-status |
| `PublicController` | `/api/public` | health, çeviri önerisi (auth gerektirmez) |
| `AuthController (admin)` | `/api/admin/auth` | admin login, bootstrap, status |
| `AdminManagementController` | `/api/admin` | kullanıcı yönetimi + audit logları |
| `LlmModelController` | `/api/admin/llm-models` | AI model yönetimi |
| `TokenUsageLogController` | `/api/admin/token-usage` | token kullanım raporu |

---

## 7. Güvenlik (Security) — nasıl çalışıyor

**`SecurityConfig`** kuralları:
- `/api/auth/**`, `/api/public/**`, admin login/bootstrap → **permitAll** (herkese açık)
- `/api/admin/**` → **ROLE_ADMIN** gerekli
- Geri kalan her şey → **kimlik doğrulaması** gerekli
- **Stateless** (session yok) — her istek JWT ile kendini kanıtlar → yatay ölçeklenir.

**Filtreler (istek sırasıyla):**
1. **`TraceIdFilter`** — her isteğe `traceId` (MDC + `X-Trace-Id` header) → tüm loglar izlenebilir.
2. **`JwtAuthenticationFilter`** — `Authorization: Bearer <token>` header'ındaki JWT'yi doğrular, kullanıcıyı SecurityContext'e koyar.
3. **`MethodNotAllowedLoggingFilter`** — 405 hatalarını loglar.

**`AuthenticatedUserResolver`** — kritik güvenlik parçası: controller'lar istemcinin gönderdiği `userId`'ye **güvenmez**; gerçek kullanıcı id'sini JWT'den çözer. (Yoksa biri başkasının userId'siyle veri çekebilirdi.)

**Şifreler:** `PasswordEncoder` (BCrypt) ile hash'lenir; ham şifre asla saklanmaz. Refresh token'lar SHA-256 hash olarak saklanır.

---

## 8. Uçtan uca akış: Kayıt → Giriş

```
1. POST /api/auth/register  (username, email, password)
   → UserService.registerUser:
     - email/username benzersiz mi kontrol
     - şifreyi BCrypt ile hash'le, User kaydet
     - varsayılan "All" kelime listesi oluştur
     - e-posta doğrulama kodu üret + (commit sonrası) e-posta gönder
   → log.info("Yeni kullanici kaydi...")

2. POST /api/auth/verify-email  (email, code)
   → kod doğru + süresi geçmemişse emailVerified=true

3. POST /api/auth/login  (identifier, password)
   → UserService.login:
     - kullanıcıyı bul, şifreyi karşılaştır (yanlışsa log.warn + hata)
     - email doğrulanmamışsa EmailNotVerifiedException
     - JWT access token üret (TokenService)
     - refresh token üret (RefreshTokenService)
   → AuthResponse { accessToken, refreshToken, kullanıcı bilgisi }
   → log.info("Giris basarili...")

4. Sonraki istekler: Authorization: Bearer <accessToken>
   → JwtAuthenticationFilter doğrular

5. Access token süresi dolunca: POST /api/auth/refresh (refreshToken)
   → RefreshTokenService.rotateRefreshToken: eskiyi iptal, yeni ver

6. POST /api/auth/logout → refresh token iptal
```

---

## 9. Uçtan uca akış: AI Hikâye Üretimi

```
POST /api/story/generate  (kelimeler, çeviri hedefi)
→ StoryGenerationService.generateStory:
  1. Gemini API key var mı?
  2. Kullanıcının GÜNLÜK token limiti aşıldı mı? (TokenUsageLog'dan hesapla)
  3. Prompt oluştur
  4. callGeminiWithFallback: model listesini sırayla dene
     - model bulunamadı/kota doldu → log.warn, sonrakine geç
     - hepsi çöktü → log.error + hata
  5. Yanıtı JSON parse et → başlık, hikâye, çeviri, kelime eşlemeleri
  6. Kullanılan token'ı kaydet (TokenUsageLog)
  → log.info("Hikaye uretildi: userId, model, tokensUsed")
  → StoryGenerateResponse
```

---

## 10. Kesişen konular (cross-cutting)
- **Auditing** — `BaseEntity` + JPA Auditing → her tabloda otomatik created_at/updated_at.
- **Loglama** — SLF4J, seviyeli (INFO/WARN/ERROR), parametreli, maskeli; traceId ile bağlı.
- **Pagination** — büyük listeler `Pageable` + `PagedResponse` ile sayfalı.
- **Hata yönetimi** — `GlobalExceptionHandler` (`@RestControllerAdvice`): 13 özel exception + catch-all + validation hataları, hepsi tutarlı JSON.
- **Config dışsallaştırma** — `@Value("${...}")` ile tüm ayarlar `.env`/properties'ten (limitler, TTL'ler, API key'ler).

---

## 11. Exception'lar (özel hata tipleri)
Her biri anlamlı bir HTTP durumu + kod döner (GlobalExceptionHandler'da):
`AuthenticationFailedException` (401), `EmailNotVerifiedException` (403), `InvalidCodeException` (400),
`InvalidRefreshTokenException` (401), `RateLimitExceededException` (429), `DailyStoryLimitExceededException` (429),
`AccountPendingDeletionException` (423), `ResourceConflictException` (409), `MailDeliveryException` (503).

---

## Çalışma önerisi
1. Bu dokümanı bir kez baştan oku (harita).
2. Bir akışı seç (ör. Kayıt→Giriş), kodda **adım adım takip et** (Controller → Service → Repository).
3. Bir entity seç, tablosunu + ilişkilerini incele.
4. Test yazarak pekiştir (bkz. `src/test` — her servis bir birim testle netleşir).
</content>
