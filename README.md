# Talemind Backend - Docker & DevOps Baseline

Bu rehber, projeyi Railway tarafına dokunmadan yerelde Docker ile sağlam bir DevOps temeline oturtmak için hazırlanmıştır.

## 1) Neden DevOps?

- Kodun her geliştiricide aynı ortamda çalışmasını sağlar.
- "Bende çalışıyor" problemini ciddi şekilde azaltır.
- Deploy öncesi riskleri erkenden yakalar.
- CI/CD, AWS (ECR/ECS/EKS), monitoring gibi ileri adımlara geçişi kolaylaştırır.

## 2) Bu projede kurduğumuz mimari

- `backend` (Spring Boot 3, Java 21)
- `mysql` (kalıcı volume ile)
- `mongodb` (kalıcı volume ile)
- Tüm servisler `docker-compose.yaml` ile tek komutta ayağa kalkar.
- Healthcheck'ler sayesinde backend, DB'ler hazır olmadan başlamaz.

## 3) Gereksinimler

- Docker Desktop (Compose v2 ile)
- En az 4 GB RAM (Docker için)

Kontrol:

```bash
docker --version
docker compose version
```

## 4) İlk kurulum (adım adım)

1. `backend` dizinine gir:

```bash
cd backend
```

2. Docker değişken dosyası oluştur:

```bash
cp .env.docker.example .env.docker
```

3. Servisleri build edip başlat:

```bash
docker compose --env-file .env.docker up -d --build
```

4. Container durumlarını kontrol et:

```bash
docker compose ps
```

5. Logları izle:

```bash
docker compose logs -f backend
```

6. Sağlık kontrolü:

```bash
curl http://localhost:8080/actuator/health
```

Beklenen cevap: `"status":"UP"`.

## 5) Günlük kullanım komutları

Başlat:

```bash
docker compose --env-file .env.docker up -d
```

Durdur:

```bash
docker compose down
```

Volume dahil tamamen sıfırla:

```bash
docker compose down -v
```

Yeniden build:

```bash
docker compose --env-file .env.docker up -d --build
```

## 6) Neleri neden böyle yaptık?

- **Multi-stage Dockerfile**: Build ve runtime imajlarını ayırarak daha temiz/taşınabilir image.
- **Maven wrapper (`mvnw`)**: CI, lokal ve container'da aynı Maven davranışı.
- **JRE runtime**: JDK yerine daha hafif runtime image.
- **Non-root user**: Container güvenliği için root yerine düşük yetkili kullanıcı.
- **Actuator + healthcheck**: Sadece process ayağa kalktı mı değil, uygulama gerçekten hazır mı kontrolü.
- **`depends_on: service_healthy`**: Backend'in DB hazır olmadan başlamasını engeller.
- **Named volumes**: MySQL/Mongo verisinin container silinse de korunması.
- **Yerel port çakışmasını azaltma**: MySQL host portu `3307`, Mongo host portu `27018`.

## 7) Railway'e neden dokunmadık?

- Bu setup sadece local Docker öğrenimi ve ortam standardizasyonu içindir.
- Railway deploy akışın mevcut haliyle devam edebilir.
- Local DevOps disiplini oturduğunda aynı prensipleri AWS'ye taşıyacağız.

## 8) AWS'ye geçiş roadmap (öneri)

1. GitHub Actions ile `mvn test` + Docker image build pipeline kur.
2. Image'ı AWS ECR'a push et.
3. ECS Fargate ile backend servisini ayağa al.
4. RDS (MySQL) ve Mongo Atlas / DocumentDB entegrasyonu yap.
5. CloudWatch + alarm + merkezi log yönetimi ekle.
6. Son adımda staging/prod ayrımını secrets manager ile tamamla.

## 9) Sık hatalar

- `backend unhealthy`: `/actuator/health` dönmüyorsa önce backend loglarına bak.
- DB bağlantı hatası: `docker compose ps` içinde MySQL/Mongo `healthy` mi kontrol et.
- Port çakışması: 8080/3307/27018 portlarını başka servis kullanıyorsa değiştir.
- Verification mail gelmiyor: `.env.docker` içinde `DOCKER_MAIL_ENABLED=true`, `DOCKER_MAIL_PROVIDER=resend`, `DOCKER_RESEND_API_KEY=<key>` ayarlarını kontrol et.

## 10) Resend ile local test

`.env.docker` dosyasına ekle/güncelle:

```env
DOCKER_MAIL_ENABLED=true
DOCKER_MAIL_PROVIDER=resend
DOCKER_MAIL_FROM=onboarding@resend.dev
DOCKER_RESEND_API_KEY=re_xxxxxxxxx
```

Sonra container'ı yeniden başlat:

```bash
docker compose --env-file .env.docker up -d --build
docker compose logs -f backend
```

## 11) Güvenlik notu

- `DOCKER_JWT_SECRET` değerini `.env.docker` içinde mutlaka değiştir.
- Gerçek production ortamında şifre/secrets değerlerini düz metin yerine secret manager ile yönet.
