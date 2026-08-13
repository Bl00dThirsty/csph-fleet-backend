import os, glob

base_dir = r'c:\Users\User\Downloads\gpl-rfid-livraisons\backend'
ymls = glob.glob(os.path.join(base_dir, '**/application.yml'), recursive=True)

for y in ymls:
    with open(y, 'r', encoding='utf-8') as f:
        content = f.read()
    if 'password: ${DB_PASSWORD:postgres}' in content:
        content = content.replace('password: ${DB_PASSWORD:postgres}', 'password: ${DB_PASSWORD:admin}')
        with open(y, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated default DB password in: {y}")
