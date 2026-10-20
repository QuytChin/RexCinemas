# RexCinemas: MySQL backup and test restore

The scripts use `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` from the current PowerShell process. If those values are absent, they reuse the current-user DPAPI files created by `start-rex-prefilled.ps1`; secrets are never printed and are never passed as a command-line argument. The generated backup directory is ignored by Git.

Create a local backup with:

```powershell
.\scripts\backup-mysql.ps1
```

The default guard permits only `localhost`, `127.0.0.1`, or `::1`. A remote target requires an explicit `-AllowNonLocalHost` decision. Review the output file size and keep the file under restricted Windows ACLs. Encrypt or move the backup to approved protected storage before treating it as an operational backup. The script does not delete old backups; retention is an owner decision.

Restore is intentionally restricted to a dedicated database whose name starts with `REX_TEST_`:

```powershell
.\scripts\restore-mysql-test.ps1 -BackupFile .\backups\mysql\rex-cinemas-...sql -TargetDatabase REX_TEST_RESTORE
```

PowerShell asks for confirmation before the write. The script refuses a production or ordinary development database, checks that the backup is present and non-empty, creates only the named test database, restores it, and reports table counts before and after. Verify representative row counts and foreign-key relationships separately before using the restored database. Never copy real customer data into an unprotected environment.

For Windows Task Scheduler, run `powershell.exe -NoProfile -ExecutionPolicy Bypass -File C:\path\to\RexCinemas\scripts\backup-mysql.ps1` under the same Windows account that owns the DPAPI files. Restrict the task account and the backup directory, and set a retention policy before automating deletion.

## Production deployment and rollback checklist

1. Load production secrets from the deployment secret store and set `SPRING_PROFILES_ACTIVE=mysql,prod`.
2. Take and verify a backup before any migration. Review the migration SQL separately.
3. Keep `spring.jpa.hibernate.ddl-auto=validate`, `spring.sql.init.mode=never`, and `app.seed.enabled=false` in production. Run schema changes through the approved migration process.
4. Check `/actuator/health`, login, and a non-writing booking page after startup. Do not use real payment, mail, webhook, or Cloudinary calls for smoke tests.
5. Watch application and database errors. Roll back the application artifact first if the release is faulty; restore the database only through the separately reviewed test-and-approval procedure.
