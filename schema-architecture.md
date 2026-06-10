# Bölüm 1: Mimari Özeti
Bu uygulama aşağıdaki mimari bileşenlerden oluşmaktadır:
-	Yönetici ve doktor panelleri için HTML sayfaları Thymeleaf aracılığıyla sunulur.
-	Randevu ve hasta işlemleri için JSON tabanlı REST API’leri sağlanır.
-	Yapılandırılmış veriler (Hasta, Doktor, Randevu, Yönetici) MySQL veritabanında saklanır.
-	Yapılandırılmamış belge tabanlı veriler (Reçete) MongoDB veritabanında saklanır.

## 1. Kullanıcı Arayüzü Katmanı
Sistem, birden fazla kullanıcı türünü ve etkileşim modelini desteklemektedir. Kullanıcılar uygulamaya aşağıdaki yöntemlerle erişebilir:
-	Thymeleaf tabanlı web panelleri (örneğin Admin Dashboard ve Doctor Dashboard)
-	REST API istemcileri (mobil uygulamalar veya diğer ön yüz uygulamaları)
Thymeleaf kullanılarak sunucu tarafında oluşturulan HTML sayfaları tarayıcıya gönderilir. REST API istemcileri ise HTTP üzerinden JSON formatında veri alışverişi yapar. Bu yapı, hem geleneksel web uygulamalarını hem de API tabanlı entegrasyonları desteklemektedir.

## 2. Kontrol Katmanı
-   Kullanıcı tarafından gerçekleştirilen her işlem ilgili Controller sınıfına yönlendirilir.
-	Thymeleaf Controller’ları HTML görünümlerini oluşturur ve kullanıcıya sunar.
-	REST Controller’ları API isteklerini karşılar ve JSON yanıtları döndürür.
-   Controller katmanı, istek doğrulamalarını gerçekleştirir ve iş mantığının yürütülmesi için Service katmanını çağırır.

## 3. Servis Katmanı
Service katmanı uygulamanın iş mantığını içerir.
Bu katman:
-	İş kurallarını uygular.
-	Veri doğrulamalarını gerçekleştirir.
-	Birden fazla varlık arasındaki işlemleri koordine eder.
-	Controller ve veri erişim katmanları arasında ayrım sağlar.

## 4. Repository Katmanı
Repository katmanı veri erişim işlemlerinden sorumludur.
-	MySQL veritabanı için Spring Data JPA kullanılır.
-	MongoDB veritabanı için Spring Data MongoDB kullanılır.

## 5. Veritabanı Katmanı
Uygulama iki farklı veritabanı teknolojisini birlikte kullanmaktadır.
### MySQL
Aşağıdaki yapılandırılmış veriler MySQL üzerinde saklanmaktadır:
-	Hasta
-	Doktor
-	Randevu
-	Yönetici
-	Kullanıcı ve Rol bilgileri
### MongoDB
Aşağıdaki belge tabanlı veriler MongoDB üzerinde saklanmaktadır:
-	Reçete kayıtları


## 6. Model Bağlama
Veritabanından alınan veriler Java nesnelerine dönüştürülür.
-	MySQL verileri JPA Entity sınıflarına eşlenir.
-	MongoDB verileri Document sınıflarına eşlenir.

## 7. Yanıt ve Sunum Katmanı
İşlenen veriler son kullanıcıya aşağıdaki yöntemlerle sunulur:
### MVC Akışı
Controller tarafından oluşturulan model nesneleri Thymeleaf şablonlarına aktarılır ve dinamik HTML sayfaları üretilir.
### REST Akışı
Model nesneleri veya DTO’lar JSON formatına dönüştürülerek API istemcilerine gönderilir.
Bu süreç, istemcinin türüne göre HTML veya JSON çıktısı üretilmesiyle tamamlanır.


# Bölüm 2: Veri ve Kontrol Akışı

1. Kullanıcılar, AdminDashboard ve DoctorDashboard gibi Thymeleaf tabanlı paneller veya Appointments, PatientDashboard ve PatientRecord gibi REST modülleri üzerinden sisteme erişir.

2. Gelen istekler ilgili Thymeleaf Controller veya REST Controller tarafından karşılanır ve işlenmek üzere yönlendirilir.

3. Controller katmanı, iş kurallarını uygulamak ve işlemleri yürütmek için Service Layer'ı çağırır.

4. Service Layer, gerekli verilere erişmek veya verileri güncellemek için MySQL Repository veya MongoDB Repository katmanlarını kullanır.

5. Repository katmanı ilgili veritabanına erişir; yapılandırılmış veriler MySQL'de, reçete verileri ise MongoDB'de saklanır.

6. Veritabanından alınan kayıtlar Java model nesnelerine dönüştürülür. MySQL verileri JPA Entity nesnelerine, MongoDB verileri ise Document nesnelerine eşlenir.

7. Elde edilen modeller Controller katmanına geri iletilir ve sonuçlar Thymeleaf aracılığıyla HTML sayfası veya REST API üzerinden JSON yanıtı olarak kullanıcıya gönderilir.




