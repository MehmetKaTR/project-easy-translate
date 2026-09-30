# TaleMind AI — Kurumsal Revize Roadmap

> Amaç: Mevcut sağlam ama olgunlaşmamış backend'i, **FinanceHub / Meridian** seviyesinde
> tam kurumsal bir yapıya taşımak. Bu doküman fazlara bölünmüştür; her fazın
> **çıktısı, alt görevleri, kabul kriteri ve tahmini süresi** vardır.
>
> Süre birimi = **kişi-gün** (tam odaklı 1 gün). Part-time çalışırsan takvim süresi ~2-3 katı.
>
> Konvansiyon referansı: `financehub/user-service` (dto/request+response, entity/base,
> MapStruct mapper, service katmanı, MySQL, JDK21, CI: `mvn clean verify`).

**Durum kısaltmaları:** ⬜ yapılacak · 🟨 devam ediyor · ✅ bitti

---

## Faz 0 — Temizlik & Hazırlık  `~0.5 gün`
Zemin hazırlığı. Riski sıfır, hızlı.

- ⬜ `revamp` adında yeni git branch aç, buradan çalış
- ⬜ Mevcut projenin derlendiğini doğrula: `./mvnw clean compile` yeşil
- ⬜ Boş 7 iskelet dosya için karar tablosu (Faz 2'de dolacaklar — şimdilik dokunma, sadece not al):
  `AdminController`, `LlmModelController`, `TokenUsageLogController`,
  `AdminActionLogController`, `ImportExportLogController`, `BaseService`, `BaseRepository`
- ⬜ `.gitignore` kontrol: `.env`, `target/`, `.idea/` ignore'da mı?

**Kabul:** Temiz branch, yeşil derleme, plan netleşmiş.

---

## Faz 1 — Paket & Mimari Konvansiyonları (FinanceHub hizası)  `~2-3 gün`
Kod tabanını FinanceHub'ın paket düzenine hizala. **En temel faz** — sonrakiler buna dayanır.

### 1a. DTO yeniden düzenleme
- ⬜ `dto/request/` ve `dto/response/` alt paketleri oluştur
- ⬜ Mevcut DTO'ları taşı: `*Request` → request, `*Response`/`*DTO` → response
- ⬜ `WordDTO`, `StoryDTO`, `WordListDTO` gibi çift yönlü DTO'ları request/response olarak ayır

### 1b. Entity tabanı & denetim (audit)
- ⬜ `entity/base/` paketi oluştur, `BaseEntity`'yi buraya taşı
- ⬜ **Story** ve **LlmModel** entity'lerini `extends BaseEntity` yap (şu an eksik → createdAt/updatedAt yok)
- ⬜ Manuel `@PrePersist` yerine **Spring Data JPA Auditing**:
  `@EnableJpaAuditing`, `@CreatedDate`, `@LastModifiedDate`, `@EntityListeners(AuditingEntityListener.class)`
- ⬜ (İleri) `createdBy`/`updatedBy` için `AuditorAware` (giriş yapan kullanıcı)

### 1c. Mapper katmanı (MapStruct)
- ⬜ `pom.xml`'e MapStruct + annotation processor ekle
- ⬜ `mapper/` paketi: `WordMapper`, `StoryMapper`, `WordListMapper`, `UserMapper` ...
- ⬜ Elle yazılmış DTO constructor'larını (`new WordDTO(entity)`) mapper'a taşı
- ⬜ Servislerde mapper enjekte et, dönüşümleri oradan yap

### 1d. Servis katmanı ayrımı (CQRS - opsiyonel ama önerilen)
- ⬜ Büyük servisleri `service/command/` (yazma) ve `service/query/` (okuma) olarak ayır
- ⬜ **`UserService` (648 satır, 26 metod)** böl: `UserCommandService`, `UserQueryService`,
  ve zaten var olan `AccountLifecycleService` / `RefreshTokenService` ile netleştir

**Kabul:** FinanceHub'la aynı paket düzeni; tüm dönüşümler mapper üzerinden; derleme yeşil.

---

## Faz 2 — Boş Dosyaları Gerçek Özelliklere Çevir  `~2-3 gün`
"Sonra ekleriz" diye bırakılan iskeletleri gerçek admin/analitik özelliklerine dönüştür.

- ⬜ **`AdminController`** → admin dashboard özet endpoint'leri (kullanıcı/istatistik özeti).
  *(Not: kullanıcı list/güncelle zaten `AdminManagementController`'da — çakışmayı netleştir, ya birleştir ya sorumluluk ayır)*
- ⬜ **`LlmModelController`** → LLM model yönetimi CRUD (admin): model ekle/güncelle/aktif-pasif
- ⬜ **`TokenUsageLogController`** → token kullanım raporları (kullanıcı bazlı/tarih bazlı, admin)
- ⬜ **`AdminActionLogController`** → denetim (audit) log listeleme + filtreleme
- ⬜ **`ImportExportLogController`** → kelime/hikaye import-export geçmişi
- ⬜ Her yeni controller için: `@RestController`, `@RequestMapping`, yetki (`@PreAuthorize`), servis + mapper
- ⬜ **`BaseService` / `BaseRepository`** kararı:
  - Ya gerçek generic taban yap (`BaseRepository<T,ID> extends JpaRepository`, ortak CRUD servis)
  - Ya da **sil** (kullanılmıyorsa kafa karışıklığı yaratıyor)

**Kabul:** Boş sınıf kalmadı; her admin endpoint'i yetkiyle korunuyor ve çalışıyor.

---

## Faz 3 — Girdi Doğrulama & Hata Yönetimi  `~1 gün`
Kurumsal sağlamlığın temeli: hiçbir gövde doğrulanmadan içeri girmesin.

- ⬜ Tüm `@RequestBody` parametrelerine `@Valid` ekle
  (eksik olanlar: `WordController`, `WordListController`, `StoryController`, `UserController`, `PublicController`, `AuthController`'da 1)
- ⬜ Eksik DTO'lara doğrulama anotasyonları: `@NotNull`, `@NotBlank`, `@Email`, `@Size`, `@Positive`
- ⬜ `GlobalExceptionHandler`'a **catch-all** `@ExceptionHandler(Exception.class)` ekle (beklenmeyen 500'ler için tutarlı cevap)
- ⬜ Standart hata DTO'su: `{ timestamp, status, code, message, path, traceId }`
- ⬜ `MethodArgumentNotValidException` cevabında alan bazlı hata listesi döndür

**Kabul:** Doğrulanmayan endpoint yok; her hata tek tip JSON formatında dönüyor.

---

## Faz 4 — Kurumsal Loglama  `~2 gün`
Şu an 100+ dosyadan sadece 4'ü log kullanıyor. Production'da hata ayıklanabilir olmalı.

- ⬜ Servis katmanına yapılandırılmış SLF4J loglama (giriş/çıkış, önemli iş olayları, hatalar)
- ⬜ **Correlation/Trace ID**: her isteğe MDC ile `traceId` ekleyen bir `OncePerRequestFilter`
- ⬜ `logback-spring.xml`: profil bazlı (dev = okunur konsol, prod = JSON satır log)
- ⬜ Hassas veri maskeleme (şifre, token, e-posta loglanmasın)
- ⬜ **(İleri seviye — FinanceHub gibi)** merkezi log: Kafka'ya log event gönderip
  bir `log-consumer` ile toplama *(TaleMind tek servis olduğu için opsiyonel; ileride ölçeklenince)*

**Kabul:** Her istek traceId taşıyor; kritik akışlar loglanıyor; hassas veri log'a düşmüyor.

---

## Faz 5 — Test Altyapısı  `~3-4 gün`
Şu an tek test var (varsayılan context testi). Kurumsal seviye için en büyük açık.

- ⬜ Bağımlılıklar: JUnit 5, Mockito, AssertJ (spring-boot-starter-test zaten var)
- ⬜ **Testcontainers** ekle: gerçek MySQL + MongoDB ile entegrasyon testleri (H2 değil)
- ⬜ Birim testleri (servis katmanı, mock repository):
  - `UserService` / auth akışı (kayıt, giriş, e-posta doğrulama, şifre sıfırlama)
  - `StoryGenerationService` iş kuralları
  - `RefreshTokenService`, `AuthRateLimitService`, token limit mantığı
- ⬜ Controller testleri (`@WebMvcTest` + MockMvc): doğrulama & yetki
- ⬜ Entegrasyon testleri (`@SpringBootTest` + Testcontainers): kritik uçtan uca akışlar
- ⬜ Hedef: **servis katmanı %60-70 kapsam**

**Kabul:** `./mvnw clean verify` tüm testleri çalıştırıp yeşil; kritik iş kuralları test altında.

---

## Faz 6 — Docker & CI/CD  `~1-2 gün`
FinanceHub'daki gibi otomatik build+test ve konteynerleştirme.

- ⬜ `Dockerfile` (multi-stage: build + jre-slim runtime)
- ⬜ `docker-compose.yml` (local): app + mysql + mongodb + (opsiyonel) mongo-express/adminer
- ⬜ `.github/workflows/ci.yml` (FinanceHub tarzı):
  - JDK 21 (temurin), maven cache
  - `mvn -B clean verify`
  - push (main) + pull_request tetikleyici
- ⬜ (Opsiyonel) Railway otomatik deploy adımı ya da image push

**Kabul:** `docker compose up` ile tüm sistem lokal ayağa kalkıyor; her push'ta CI yeşil.

---

## Faz 7 — Dokümantasyon & Son Rötuş  `~1 gün`
- ⬜ **springdoc-openapi (Swagger UI)** ekle → `/swagger-ui.html` ile canlı API dokümanı
- ⬜ README güncelle: kurulum, `.env` şablonu, çalıştırma, mimari şeması
- ⬜ Actuator: `health`, `info`, `metrics` düzgün expose (prod'da güvenli)
- ⬜ Güvenlik gözden geçirme: JWT süre/refresh, CORS origin listesi, rate limit eşikleri
- ⬜ `application.properties` temizliği (yorumlu ölü satırlar, `ddl-auto` prod=validate teyidi)

**Kabul:** Swagger'dan tüm API görülüyor; README güncel; actuator sağlıklı.

---

## 📊 Toplam Tahmin
| Faz | Konu | Süre (kişi-gün) |
|-----|------|-----------------|
| 0 | Temizlik & Hazırlık | 0.5 |
| 1 | Mimari konvansiyonlar | 2-3 |
| 2 | Boş dosyalar → özellik | 2-3 |
| 3 | Doğrulama & hata | 1 |
| 4 | Loglama | 2 |
| 5 | Test altyapısı | 3-4 |
| 6 | Docker & CI/CD | 1-2 |
| 7 | Dokümantasyon | 1 |
| **Toplam** | | **~13-16.5 kişi-gün** |

- **Tam zamanlı:** ~3 hafta
- **Part-time (günde 2-3 saat):** ~5-7 hafta

---

## Faz 8+ — Yeni Özellikler (revizeden SONRA)
Kurumsal temel oturunca eklenecekler buraya yazılır. Örnek başlıklar:
- ⬜ (senin ekleyeceğin yeni özellikler)

---

### Önerilen çalışma sırası
`Faz 0 → 1 → 3 → 2 → 4 → 5 → 6 → 7`
*(Not: Doğrulama/hata yönetimi (Faz 3) mimariden hemen sonra yapılırsa, boş dosyaları
doldururken (Faz 2) baştan doğru desende yazarsın.)*
