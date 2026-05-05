# Grey Toad

Aplikacja do zarządzania zespołami i zadaniami. Obsługuje czat w czasie rzeczywistym, system ticketów z priorytetami i SLA, role użytkowników (Admin, Lider, Pracownik) oraz wiadomości prywatne.

## Technologie

- **Backend:** Java 17, Spring Boot 3.2.5, PostgreSQL, WebSocket (STOMP)
- **Frontend:** Angular 17, SCSS, TypeScript

## Wymagania

- Java 17+
- Node.js 18+
- PostgreSQL 14+

## Uruchomienie

### 1. Baza danych

Utwórz bazę i użytkownika w PostgreSQL:

```sql
CREATE USER greytoad WITH PASSWORD 'password';
CREATE DATABASE grey_toad OWNER greytoad;
```

### 2. Backend

```bash
cd Grey_Toad
./gradlew bootRun
```

Serwer uruchamia się na `http://localhost:8080`

### 3. Frontend

```bash
cd grey-toad-frontend
npm install
ng serve
```

Aplikacja dostępna pod adresem `http://localhost:4200`

### 4. Pierwsze konto

Po uruchomieniu zarejestruj się na stronie `/auth/register`. Pierwsze zarejestrowane konto automatycznie otrzymuje rolę **Administratora**.

## Funkcje

- Zarządzanie projektami i zespołami
- System zadań z priorytetami, typami, SLA i terminami
- Czat grupowy (kanały) i wiadomości prywatne w czasie rzeczywistym
- Role: Administrator, Lider, Pracownik
- Statusy użytkownika (Dostępny, Przerwa, Spotkanie itp.)
