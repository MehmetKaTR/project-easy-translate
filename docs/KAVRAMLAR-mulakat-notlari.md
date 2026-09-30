# Kurumsal Java / Spring — Kavram Notları (Mülakat Hazırlığı)

> TaleMind AI revizesi sırasında öğrenilen kavramların kalıcı notu.
> İleride PDF'e çevrilecek. Her başlık: "nedir, neden, önce/sonra, kendi projemizden referans".

---

## 1. `equals()` ve `hashCode()` — "İki obje aynı mı?"

### Temel fikir
- Java'da `==` iki objenin **bellek adresini** karşılaştırır (aynı kutu mu?).
- `equals()` ise **"bu iki obje benim gözümde aynı şey mi?"** sorusunun cevabıdır — tanımını sen yaparsın.
- `hashCode()`, `equals()`'ın ikizidir: **aynı sayılan iki obje aynı hash sayısını üretmelidir.**

```java
User a = userRepo.findById(5L);
User b = userRepo.findById(5L);
a == b        // false  (iki farklı obje)
a.equals(b)   // true olmasını isteriz — ikisi de "5 numaralı kullanıcı"
```

### Ne zaman devreye girer? → Koleksiyonlar
`HashSet`, `HashMap`, `.contains()`, `.distinct()` bunları arka planda kullanır:
```java
Set<User> set = new HashSet<>();
set.add(user5);
set.contains(user5);   // "zaten var mı?" → hashCode + equals ile bakar
```
hashCode yanlışsa: Set aynı objeyi iki kere ekler veya eklediğini bulamaz. Bug kaynağı.

### Entity'de neyi karşılaştırmalı?
**Yanlış — "tüm alanlara bak":**
```java
Set<User> set = new HashSet<>();
set.add(user);            // içeri koydun
user.setUpdatedAt(now);   // bir alan değişti
set.contains(user);       // FALSE! artık "başka obje" sayıldı, kayboldu
```
**Doğru — "sadece id'ye bak":** Kullanıcının adı/e-postası/updatedAt'i değişse de id=5 ise hâlâ aynı kişidir.

> **Altın kural:** Entity/document eşitliği **her zaman `id` üzerinden**. Diğer alanlar zamanla değişir, id değişmez.

### `callSuper`
Üst sınıf (BaseEntity/BaseDocument) alanlarını eşitliğe katmak isteyip istemediğini söyler.
Audit alanları (createdAt/updatedAt) katılmamalı → genelde `callSuper = false`.

---

## 2. Entity'de `@Data` KULLANMA — `@Getter/@Setter` kullan

### Sorun
`@Data` beş anotasyonun paketidir:
| İçindeki | Entity'de |
|---|---|
| `@Getter`, `@Setter` | ✅ sorun yok |
| `@ToString` | ❌ çift yönlü ilişkilerde **sonsuz döngü (StackOverflow)**; lazy alanları erken yükler |
| `@EqualsAndHashCode` (tüm alanlar) | ❌ koleksiyon felaketi (bkz. Bölüm 1) |
| `@RequiredArgsConstructor` | ❌ builder/constructor ile çakışabilir |

### Doğru konvansiyon (kendi projemizden referans)
- **Meridian (en olgun proje):** entity'lerde `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`; `@Data` ve `@EqualsAndHashCode` **yok** → Java'nın varsayılan kimlik eşitliği kalır (güvenli).
- **FinanceHub (daha eski):** entity'lerde `@Data` + `@EqualsAndHashCode(callSuper=false)` (eski alışkanlık).
- **Kural:** `@Data` → **DTO'lara** yakışır (düz, ilişkisiz, kimliksiz). Entity'de **kullanma**.

### Önce / Sonra (TaleMind)
```java
// ÖNCE (eski alışkanlık)
@Data
@Builder @NoArgsConstructor @AllArgsConstructor
public class Story extends BaseDocument { ... }

// SONRA (meridian konvansiyonu)
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class Story extends BaseDocument { ... }
```

---

## 3. JPA & Mongo Auditing — zaman damgalarını framework doldursun

### Önce / Sonra
- **ÖNCE:** `BaseEntity`'de elle `@PrePersist`/`@PreUpdate` + `LocalDateTime.now()`;
  serviste her update'te `setUpdatedAt(new Date())` (dağınık, unutmaya açık).
- **SONRA:** `@CreatedDate` / `@LastModifiedDate` + aktivasyon anotasyonu → otomatik.

### Kurulum
- JPA: `@EnableJpaAuditing` (main sınıf) + `@EntityListeners(AuditingEntityListener.class)` (base).
- Mongo: `@EnableMongoAuditing` (main sınıf); dinleyici otomatik.
- Tek tip zaman tipi: **`LocalDateTime`** her yerde (Date karışımı bırakma).

### Referans
FinanceHub `entity/base/BaseEntity` elle `@PrePersist` kullanıyor; auditing bunun bir adım ilerisi (talemind artık burada önde).

---

## 4. `@Value` — konfigürasyonu dışarıdan okuma

```java
@Value("${app.auth.refresh-token-ttl-days:30}")
private int refreshTokenTtlDays;
```
- `${...}` → `application.properties`/`.env`'den ayarı oku; `:30` → tanımlı değilse varsayılan.
- **Neden:** davranışı kodu değiştirmeden (recompile YOK) ortam değişkeniyle ayarlarsın. Dev'de farklı, prod'da farklı; API key'ler, limitler, TTL'ler böyle yönetilir.

---

## 5. Auth: JWT, SHA-256, Refresh Token

### Temel problem
HTTP **stateless** — sunucu istekler arası seni hatırlamaz. Her istekte kimliğini kanıtlaman gerekir.
- **Session (eski):** sunucu defter tutar → ölçeklenmez.
- **JWT (modern):** sunucu imzalı kimlik kartı verir, defter tutmaz → ölçeklenir.

### JWT (JSON Web Token)
`header.payload.signature` yapısında imzalı token.
- Payload: `{ sub: username, role, exp }`.
- İmza = `HMAC-SHA256(header + payload, GİZLİ_ANAHTAR)`; gizli anahtar sadece sunucuda (`JWT_SECRET`).
- Payload'ı değiştirirsen imza tutmaz → **sahtelenemez.** Sunucu her istekte imzayı yeniden hesaplayıp doğrular → **defter gerekmez** (self-contained).

### SHA-256 / HMAC
Tek yönlü özet fonksiyonu: her girdi → sabit 256-bit parmak izi; **geri dönülemez.** JWT imzasında (HMAC) ve refresh token saklamada (hash) kullanılır.

### Access token neden kısa (1 saat)?
JWT **iptal edilemez** (defter yok). Çalınırsa süresi bitene kadar geçerli → hasarı sınırlamak için **kısa ömürlü** tutulur. Kullanıcıyı yormaz çünkü refresh token arka planda yeniler.

### Refresh token
| | Access (JWT) | Refresh |
|---|---|---|
| Ömür | 1 saat | 30 gün |
| Saklanır mı | Hayır (stateless) | Evet, DB'de (hash olarak) |
| İptal | Edilemez | Edilebilir (logout) |
| İş | kimlik kanıtı | yeni access token almak |

**Mobil akış:** app iki token'ı saklar; her istekte access; 1 saat sonra 401 → app arka planda `/refresh` ile yeni access alır; kullanıcı hiçbir şey görmez, 30 gün girişsiz kullanır.
- **DB'de HASH saklanır** (ham değil): DB sızarsa token'lar kullanılamaz.
- **Rotation:** her yenilemede eski iptal, yeni verilir (`replacedByTokenHash` zinciri) → çalıntı token tespiti.

### Şifreler
`PasswordEncoder` (BCrypt) ile hash'lenir; ham şifre asla saklanmaz. Login'de `passwordEncoder.matches(girilen, hash)`.
