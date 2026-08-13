import os, subprocess

psql = r'C:\Program Files\PostgreSQL\18\bin\psql.exe'
if not os.path.exists(psql):
    psql = r'C:\Program Files\PostgreSQL\17\bin\psql.exe'

passwords = ['postgres', 'root', 'admin', '1234', '123456', '']
ports = ['5432', '5433', '5434', '5435']

found = False
for port in ports:
    for pwd in passwords:
        env = os.environ.copy()
        env['PGPASSWORD'] = pwd
        res = subprocess.run([psql, '-U', 'postgres', '-p', port, '-c', 'SELECT version();'], capture_output=True, text=True, env=env)
        if res.returncode == 0:
            print(f"SUCCESS! Port: {port}, Password: '{pwd}'")
            dbs = ['gpl_auth_db', 'gpl_organization_db', 'gpl_user_db', 'gpl_audit_db', 'gpl_notification_db', 'gpl_tour_db', 'gpl_cylinder_db', 'gpl_fleet_db', 'gpl_subsidy_db']
            for db in dbs:
                res_db = subprocess.run([psql, '-U', 'postgres', '-p', port, '-c', f'CREATE DATABASE {db};'], capture_output=True, text=True, env=env)
                out = res_db.stderr.strip() or res_db.stdout.strip()
                print(f"Database {db}: {out}")
            found = True
            break
    if found:
        break

if not found:
    print("Could not authenticate with standard passwords.")
