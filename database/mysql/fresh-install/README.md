# RexCinemas fresh install (MySQL 8+)

`full_install.sql` contains the complete schema and demo data. It has no `SOURCE` dependencies and targets an empty database named `rex_chain_cinema`. It creates that database if missing and never drops or resets it. Do not import it over a database that already contains application data.

## Windows Command Prompt

From the repository root, run:

```bat
mysql --protocol=tcp -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 < "database\mysql\fresh-install\full_install.sql"
```

Enter the MySQL password when prompted. The `mysql` client must be on `PATH`; otherwise use its full path, for example `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`.

## Demo accounts

All seeded demo accounts use password `123456`, stored as a BCrypt hash compatible with the application's Spring Security `BCryptPasswordEncoder`. Available roles include admin, cinema manager, Quận 1 supervisors, Quận 1 and Gò Vấp staff, and seven customer accounts. Use these accounts only in a local/demo environment.

The BANK_QR setting is disabled and blank until an operator configures a real payment account. No provider transactions, bank snapshots, email, or push notifications are sent by importing the SQL.

The component files are numbered in dependency order. `11_validation.sql` reports row counts and data-integrity checks after a manual import; `full_install.sql` already runs those checks at its end.
